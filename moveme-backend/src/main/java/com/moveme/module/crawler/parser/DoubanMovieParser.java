package com.moveme.module.crawler.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveme.module.movie.entity.Movie;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 豆瓣电影数据解析器
 * - parseListJson: 解析列表接口的 JSON 响应
 * - parseDetailHtml: 解析详情页 HTML，提取导演/演员/类型/简介等
 */
@Slf4j
@Component
public class DoubanMovieParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析豆瓣列表 JSON API 响应
     * 格式: {"subjects": [{"id":"1292052","title":"肖申克的救赎","cover":"...","rate":"9.7","url":"..."}]}
     */
    public List<Movie> parseListJson(String json) {
        List<Movie> movies = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode subjects = root.get("subjects");
            if (subjects == null || !subjects.isArray()) {
                return movies;
            }

            for (JsonNode node : subjects) {
                Movie movie = new Movie();
                movie.setDoubanId(node.get("id").asText());
                movie.setTitle(node.get("title").asText());
                movie.setPosterUrl(node.has("cover") ? node.get("cover").asText() : null);

                String rate = node.has("rate") ? node.get("rate").asText("0") : "0";
                if (!rate.isEmpty() && !"0".equals(rate)) {
                    movie.setDoubanRating(new BigDecimal(rate));
                }

                movies.add(movie);
            }
        } catch (Exception e) {
            log.error("解析豆瓣列表 JSON 失败", e);
        }
        return movies;
    }

    /**
     * 解析豆瓣电影详情页 HTML，填充电影详细信息
     * @param html   HTML 内容
     * @param movie  已有基础信息的 Movie 对象
     * @return 填充后的 ParsedDetail
     */
    public ParsedDetail parseDetailHtml(String html, Movie movie) {
        ParsedDetail detail = new ParsedDetail();
        detail.setMovie(movie);

        try {
            Document doc = Jsoup.parse(html);

            // 标题（如果列表没有，从详情页取）
            Element titleEl = doc.selectFirst("span[property=v:itemreviewed]");
            if (titleEl != null && (movie.getTitle() == null || movie.getTitle().isEmpty())) {
                movie.setTitle(titleEl.text());
            }

            // 年份
            Element yearEl = doc.selectFirst(".year");
            if (yearEl != null) {
                String yearText = yearEl.text().replaceAll("[^0-9]", "");
                if (!yearText.isEmpty()) {
                    movie.setYear(Short.parseShort(yearText));
                }
            }

            // 评分
            Element ratingEl = doc.selectFirst("strong[property=v:average]");
            if (ratingEl != null && !ratingEl.text().isEmpty()) {
                try {
                    movie.setDoubanRating(new BigDecimal(ratingEl.text().trim()));
                } catch (NumberFormatException ignored) {}
            }

            // 评分人数
            Element votesEl = doc.selectFirst("span[property=v:votes]");
            if (votesEl != null) {
                try {
                    movie.setDoubanVotes(Integer.parseInt(votesEl.text().trim()));
                } catch (NumberFormatException ignored) {}
            }

            // 简介
            Elements summaryEls = doc.select("span[property=v:summary]");
            if (!summaryEls.isEmpty()) {
                movie.setSummary(summaryEls.first().text().trim());
            }

            // 导演
            Elements directorEls = doc.select("a[rel=v:directedBy]");
            for (Element el : directorEls) {
                detail.getDirectors().add(el.text().trim());
            }

            // 主演
            Elements actorEls = doc.select("a[rel=v:starring]");
            for (Element el : actorEls) {
                detail.getActors().add(el.text().trim());
            }

            // 类型
            Elements genreEls = doc.select("span[property=v:genre]");
            for (Element el : genreEls) {
                detail.getGenres().add(el.text().trim());
            }

            // #info 区域的文本信息（制片国家、语言、片长等）
            Element infoEl = doc.selectFirst("#info");
            if (infoEl != null) {
                String infoText = infoEl.text();

                // 制片国家/地区
                movie.setCountry(extractInfoField(infoText, "制片国家/地区"));

                // 语言
                movie.setLanguage(extractInfoField(infoText, "语言"));

                // 片长
                Element durationEl = doc.selectFirst("span[property=v:runtime]");
                if (durationEl != null) {
                    movie.setDuration(durationEl.text().trim());
                }

                // 又名 / 原名
                String aka = extractInfoField(infoText, "又名");
                if (aka != null && movie.getOriginalTitle() == null) {
                    movie.setOriginalTitle(aka.split("/")[0].trim());
                }

                // IMDb
                String imdb = extractInfoField(infoText, "IMDb");
                if (imdb != null) {
                    movie.setImdbId(imdb.trim());
                }
            }

        } catch (Exception e) {
            log.error("解析豆瓣详情页失败, doubanId={}", movie.getDoubanId(), e);
        }

        return detail;
    }

    /**
     * 从 #info 文本中提取指定字段值
     * 例如 "制片国家/地区: 美国" → "美国"
     */
    private String extractInfoField(String infoText, String fieldName) {
        Pattern pattern = Pattern.compile(fieldName + ":\\s*([^\\n]+?)(?:\\s+\\S+:|$)");
        Matcher matcher = pattern.matcher(infoText);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 详情解析结果：电影 + 导演列表 + 演员列表 + 类型列表
     */
    @lombok.Data
    public static class ParsedDetail {
        private Movie movie;
        private List<String> directors = new ArrayList<>();
        private List<String> actors = new ArrayList<>();
        private List<String> genres = new ArrayList<>();
    }
}
