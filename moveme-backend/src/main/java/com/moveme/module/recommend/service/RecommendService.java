package com.moveme.module.recommend.service;

import com.moveme.module.movie.entity.Movie;
import com.moveme.module.movie.mapper.MovieMapper;
import com.moveme.module.recommend.dto.ChatRequest;
import com.moveme.module.user.entity.RecommendationLog;
import com.moveme.module.user.mapper.*;
import com.moveme.module.user.vo.UserFavoriteVO;
import com.moveme.module.user.vo.UserRatingVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendService {

    private final MimoService mimoService;
    private final UserGenrePrefMapper userGenrePrefMapper;
    private final UserPersonPrefMapper userPersonPrefMapper;
    private final RatingMapper ratingMapper;
    private final FavoriteMapper favoriteMapper;
    private final RecommendationLogMapper recommendationLogMapper;
    private final MovieMapper movieMapper;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            你是 MovieMe 的 AI 电影推荐助手。

            【核心规则】
            1. 你只能推荐下方「电影片单」中列出的电影，严禁推荐片单之外的任何电影
            2. 每部推荐的电影必须附上详情页链接，格式：[查看详情](/movies/{id})
            3. 回答使用中文，格式清晰美观
            4. 电影片名用 **加粗** 标记
            5. 如果用户的请求在片单中找不到合适的电影，请诚实告知并建议换个方向

            【电影片单】（仅可推荐以下电影）
            %s
            """;

    private static final String CHAT_SYSTEM_PROMPT = """
            你是 MovieMe 的 AI 电影助手。你热爱电影，知识渊博，可以用中文和用户自然地聊电影相关的话题。
            回答简洁友好，使用中文。
            """;

    /**
     * 聊天模式 — 非流式
     */
    public String chat(Long userId, List<ChatRequest.ChatMessage> messages) {
        List<Map<String, String>> mimoMessages = new ArrayList<>();
        mimoMessages.add(Map.of("role", "system", "content", CHAT_SYSTEM_PROMPT));
        for (ChatRequest.ChatMessage msg : messages) {
            mimoMessages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
        }

        long start = System.currentTimeMillis();
        String reply = mimoService.chat(mimoMessages);
        long latency = System.currentTimeMillis() - start;

        reply = injectMovieLinks(reply);
        saveLog(userId, "chat", summarizeMessages(messages), reply, latency);
        return reply;
    }

    /**
     * 聊天模式 — 流式输出
     */
    public void chatStream(Long userId, List<ChatRequest.ChatMessage> messages,
                           MimoService.StreamCallback callback) {
        List<Map<String, String>> mimoMessages = new ArrayList<>();
        mimoMessages.add(Map.of("role", "system", "content", CHAT_SYSTEM_PROMPT));
        for (ChatRequest.ChatMessage msg : messages) {
            mimoMessages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
        }

        long start = System.currentTimeMillis();
        mimoService.chatStream(mimoMessages, new MimoService.StreamCallback() {
            final StringBuilder fullResponse = new StringBuilder();

            @Override
            public void onToken(String token) {
                fullResponse.append(token);
                callback.onToken(token);
            }

            @Override
            public void onComplete() {
                long latency = System.currentTimeMillis() - start;
                saveLog(userId, "chat", summarizeMessages(messages),
                        fullResponse.toString(), latency);
                callback.onComplete();
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * 一键推荐 — 基于用户口味档案生成推荐
     */
    public String quickRecommend(Long userId) {
        String catalog = buildMovieCatalog();
        String userContext = buildUserContext(userId);
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, catalog);

        String userPrompt = userContext.isEmpty()
                ? "请从片单中挑选一部最值得观看的电影，用 3-5 段文字详细介绍（剧情梗概、亮点、导演/演员、获奖情况等），最后附上详情页链接。"
                : "根据以下我的观影档案，从片单中挑选一部最适合我口味的电影。用 3-5 段文字详细介绍（剧情梗概、亮点、导演/演员、获奖情况），说明为什么这部电影适合我，最后附上详情页链接。\n\n" + userContext;

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );

        long start = System.currentTimeMillis();
        String reply = mimoService.chat(messages);
        long latency = System.currentTimeMillis() - start;

        reply = injectMovieLinks(reply);
        saveLog(userId, "quick", userContext, reply, latency);
        return reply;
    }

    /**
     * 构建完整的用户画像上下文
     */
    private String buildUserContext(Long userId) {
        List<Map<String, Object>> genres = userGenrePrefMapper.selectWithGenreName(userId);
        List<Map<String, Object>> persons = userPersonPrefMapper.selectWithPersonName(userId);
        List<UserRatingVO> ratings = ratingMapper.selectByUserIdWithMovie(userId);
        List<UserFavoriteVO> favorites = favoriteMapper.selectByUserIdWithMovie(userId);

        if (genres.isEmpty() && persons.isEmpty() && ratings.isEmpty() && favorites.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("【我的观影档案】\n");

        // 类型偏好
        if (!genres.isEmpty()) {
            sb.append("偏好类型：");
            sb.append(genres.stream()
                    .limit(5)
                    .map(g -> g.get("genreName") + "(" + String.format("%.1f", g.get("score")) + ")")
                    .collect(Collectors.joining("、")));
            sb.append("\n");
        }

        // 影人偏好
        if (!persons.isEmpty()) {
            sb.append("偏好影人：");
            sb.append(persons.stream()
                    .limit(5)
                    .map(p -> {
                        String role = switch (String.valueOf(p.get("roleKind"))) {
                            case "director" -> "导演";
                            case "actor" -> "演员";
                            case "writer" -> "编剧";
                            default -> "影人";
                        };
                        return p.get("personName") + "(" + role + ")";
                    })
                    .collect(Collectors.joining("、")));
            sb.append("\n");
        }

        // 评分记录（最有价值的信号）
        if (!ratings.isEmpty()) {
            sb.append("已评分电影（不要重复推荐）：\n");
            ratings.stream().limit(15).forEach(r -> {
                sb.append(String.format("  - %s → %d分", r.getTitle(), r.getScore()));
                if (r.getComment() != null && !r.getComment().isBlank()) {
                    String comment = r.getComment().length() > 60
                            ? r.getComment().substring(0, 60) + "..." : r.getComment();
                    sb.append(" (\"").append(comment).append("\")");
                }
                sb.append("\n");
            });
        }

        // 收藏列表
        List<UserFavoriteVO> wishList = favorites.stream()
                .filter(f -> f.getStatus() == 0).toList();
        List<UserFavoriteVO> watchedList = favorites.stream()
                .filter(f -> f.getStatus() == 1).toList();

        if (!wishList.isEmpty()) {
            sb.append("想看列表：");
            sb.append(wishList.stream()
                    .limit(10)
                    .map(UserFavoriteVO::getTitle)
                    .collect(Collectors.joining("、")));
            sb.append("\n");
        }

        if (!watchedList.isEmpty()) {
            sb.append("已看列表：");
            sb.append(watchedList.stream()
                    .limit(10)
                    .map(UserFavoriteVO::getTitle)
                    .collect(Collectors.joining("、")));
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 构建电影片单，按评分降序
     */
    private String buildMovieCatalog() {
        List<Movie> movies = movieMapper.selectList(
                new LambdaQueryWrapper<Movie>()
                        .isNotNull(Movie::getDoubanRating)
                        .isNotNull(Movie::getTitleCn)
                        .orderByDesc(Movie::getDoubanRating)
                        .last("LIMIT 300")
        );

        StringBuilder sb = new StringBuilder();
        for (Movie m : movies) {
            sb.append(String.format("- id:%d | %s | %s年 | 豆瓣%s分\n",
                    m.getId(),
                    m.getTitleCn() != null ? m.getTitleCn() : m.getTitle(),
                    m.getYear() != null ? m.getYear() : "未知",
                    m.getDoubanRating() != null ? m.getDoubanRating().toPlainString() : "无"));
        }
        return sb.toString();
    }

    private String summarizeMessages(List<ChatRequest.ChatMessage> messages) {
        if (messages.isEmpty()) return "";
        ChatRequest.ChatMessage last = messages.get(messages.size() - 1);
        String content = last.getContent();
        return content.length() > 200 ? content.substring(0, 200) + "..." : content;
    }

    /**
     * 后处理：将回复中出现的数据库电影名自动加上详情链接
     */
    private String injectMovieLinks(String text) {
        List<Movie> allMovies = movieMapper.selectList(
                new LambdaQueryWrapper<Movie>()
                        .select(Movie::getId, Movie::getTitleCn, Movie::getTitle)
        );

        // 构建 (片名, id) 列表，中文名和英文原名都收录
        record TitleId(String title, Long id) {}
        List<TitleId> titleIds = new ArrayList<>();
        for (Movie m : allMovies) {
            if (m.getTitleCn() != null && m.getTitleCn().length() >= 2) {
                titleIds.add(new TitleId(m.getTitleCn(), m.getId()));
            }
            if (m.getTitle() != null && m.getTitle().length() >= 3
                    && !m.getTitle().equals(m.getTitleCn())) {
                titleIds.add(new TitleId(m.getTitle(), m.getId()));
            }
        }

        // 按片名长度降序，避免短名先匹配导致长名被截断
        titleIds.sort((a, b) -> b.title.length() - a.title.length());

        String result = text;
        for (TitleId ti : titleIds) {
            String escaped = Pattern.quote(ti.title);
            String regex = "(?<!\\[)" + escaped + "(?!\\]\\()";
            String replacement = "[" + ti.title + "](/movies/" + ti.id + ")";
            result = result.replaceAll(regex, replacement);
        }
        return result;
    }

    private void saveLog(Long userId, String strategyType, String requestData,
                          String responseData, long latencyMs) {
        try {
            RecommendationLog log = new RecommendationLog();
            log.setUserId(userId);
            log.setStrategyType(strategyType);
            log.setRequestData(requestData);
            log.setResponseData(responseData.length() > 2000
                    ? responseData.substring(0, 2000) + "..." : responseData);
            log.setLlmProvider("mimo");
            log.setLatencyMs((int) latencyMs);
            recommendationLogMapper.insert(log);
        } catch (Exception e) {
            log.warn("保存推荐日志失败", e);
        }
    }
}
