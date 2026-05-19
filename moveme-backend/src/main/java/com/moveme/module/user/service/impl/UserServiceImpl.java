package com.moveme.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moveme.common.constant.RedisKeyConstants;
import com.moveme.common.exception.BusinessException;
import com.moveme.common.result.ResultCode;
import com.moveme.common.util.JwtUtil;
import com.moveme.module.movie.entity.Movie;
import com.moveme.module.movie.entity.MovieComment;
import com.moveme.module.movie.mapper.MovieCommentMapper;
import com.moveme.module.movie.mapper.MovieMapper;
import com.moveme.module.user.dto.UserLoginDTO;
import com.moveme.module.user.dto.UserRegisterDTO;
import com.moveme.module.user.entity.*;
import com.moveme.module.user.mapper.*;
import com.moveme.module.user.service.UserService;
import com.moveme.module.user.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    @Value("${moveme.avatar.dir:./data/avatars}")
    private String avatarDir;

    @Value("${moveme.avatar.url-prefix:/static/avatars}")
    private String avatarUrlPrefix;

    @Value("${moveme.avatar.users-dir:./data/avatars/users}")
    private String avatarUsersDir;

    @Value("${moveme.avatar.users-url-prefix:/static/avatars/users}")
    private String avatarUsersUrlPrefix;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RatingMapper ratingMapper;
    private final FavoriteMapper favoriteMapper;
    private final ViewHistoryMapper viewHistoryMapper;
    private final UserGenrePrefMapper userGenrePrefMapper;
    private final UserPersonPrefMapper userPersonPrefMapper;
    private final MovieMapper movieMapper;
    private final MovieCommentMapper movieCommentMapper;

    @Override
    public void register(UserRegisterDTO dto) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername())) > 0) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        if (StringUtils.hasText(dto.getEmail()) &&
                userMapper.selectCount(new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, dto.getEmail())) > 0) {
            throw new BusinessException(ResultCode.EMAIL_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setRole(0);
        user.setStatus(1);
        userMapper.insert(user);
    }

    @Override
    public TokenVO login(UserLoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));

        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.WRONG_CREDENTIALS);
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        String roleName = user.getRole() == 1 ? "ADMIN" : "USER";
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roleName);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        redisTemplate.opsForValue().set(
                RedisKeyConstants.REFRESH_TOKEN_PREFIX + user.getId(),
                refreshToken,
                7, TimeUnit.DAYS);

        return new TokenVO(accessToken, refreshToken);
    }

    @Override
    public TokenVO refreshToken(String refreshToken) {
        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Refresh Token已过期");
        }

        Long userId = jwtUtil.getUserId(refreshToken);
        String storedToken = (String) redisTemplate.opsForValue()
                .get(RedisKeyConstants.REFRESH_TOKEN_PREFIX + userId);

        if (!refreshToken.equals(storedToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Refresh Token无效");
        }

        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        String roleName = user.getRole() == 1 ? "ADMIN" : "USER";
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roleName);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());

        redisTemplate.opsForValue().set(
                RedisKeyConstants.REFRESH_TOKEN_PREFIX + userId,
                newRefreshToken,
                7, TimeUnit.DAYS);

        return new TokenVO(newAccessToken, newRefreshToken);
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return toVO(user);
    }

    @Override
    public void updateProfile(Long userId, String nickname, String email) {
        User user = new User();
        user.setId(userId);
        if (StringUtils.hasText(nickname)) {
            user.setNickname(nickname);
        }
        if (StringUtils.hasText(email)) {
            Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, email)
                    .ne(User::getId, userId));
            if (count > 0) {
                throw new BusinessException(ResultCode.EMAIL_ALREADY_EXISTS);
            }
            user.setEmail(email);
        }
        userMapper.updateById(user);
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅支持 jpg/png/gif/webp 格式");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件大小不能超过 5MB");
        }

        String ext = switch (contentType) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String filename = userId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

        try {
            Path dir = Paths.get(avatarUsersDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件保存失败: " + e.getMessage());
        }

        String url = avatarUsersUrlPrefix + "/" + filename;
        User user = new User();
        user.setId(userId);
        user.setAvatarUrl(url);
        userMapper.updateById(user);
        return url;
    }

    @Override
    public UserStatsVO getUserStats(Long userId) {
        UserStatsVO vo = new UserStatsVO();
        vo.setRatingCount(ratingMapper.selectCount(
                new LambdaQueryWrapper<Rating>().eq(Rating::getUserId, userId)));
        vo.setWishCount(favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getStatus, 0)));
        vo.setWatchedCount(favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getStatus, 1)));
        vo.setHistoryCount(viewHistoryMapper.selectCount(
                new LambdaQueryWrapper<ViewHistory>()
                        .eq(ViewHistory::getUserId, userId)));
        return vo;
    }

    @Override
    public IPage<UserRatingVO> getUserRatings(Long userId, long page, long size) {
        List<UserRatingVO> all = ratingMapper.selectByUserIdWithMovie(userId);
        return paginate(all, page, size);
    }

    @Override
    public IPage<UserFavoriteVO> getUserFavorites(Long userId, Integer status, long page, long size) {
        List<UserFavoriteVO> all = (status != null)
                ? favoriteMapper.selectByUserIdAndStatusWithMovie(userId, status)
                : favoriteMapper.selectByUserIdWithMovie(userId);
        // 填充 genres
        for (UserFavoriteVO vo : all) {
            vo.setGenres(movieMapper.selectGenreNamesByMovieId(vo.getMovieId()));
        }
        return paginate(all, page, size);
    }

    @Override
    public IPage<UserHistoryVO> getUserHistory(Long userId, long page, long size) {
        List<UserHistoryVO> all = viewHistoryMapper.selectByUserIdWithMovie(userId);
        return paginate(all, page, size);
    }

    @Override
    public UserTasteVO getUserTaste(Long userId) {
        UserTasteVO vo = new UserTasteVO();

        // 平均评分
        List<Rating> ratings = ratingMapper.selectList(
                new LambdaQueryWrapper<Rating>()
                        .eq(Rating::getUserId, userId));
        vo.setRatingCount(ratings.size());
        if (!ratings.isEmpty()) {
            BigDecimal avg = ratings.stream()
                    .map(r -> BigDecimal.valueOf(r.getScore()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(ratings.size()), 1, RoundingMode.HALF_UP);
            vo.setAvgRatingGiven(avg);
        } else {
            vo.setAvgRatingGiven(BigDecimal.ZERO);
        }

        // 类型偏好
        List<Map<String, Object>> genreRows = userGenrePrefMapper.selectWithGenreName(userId);
        vo.setGenrePrefs(genreRows.stream().map(row -> {
            UserTasteVO.GenrePref gp = new UserTasteVO.GenrePref();
            gp.setGenreId(((Number) row.get("genre_id")).intValue());
            gp.setGenreName((String) row.get("genreName"));
            gp.setScore((BigDecimal) row.get("score"));
            return gp;
        }).collect(Collectors.toList()));

        // 人物偏好
        List<Map<String, Object>> personRows = userPersonPrefMapper.selectWithPersonName(userId);
        vo.setPersonPrefs(personRows.stream().map(row -> {
            UserTasteVO.PersonPref pp = new UserTasteVO.PersonPref();
            pp.setPersonId(((Number) row.get("person_id")).longValue());
            pp.setPersonName((String) row.get("personName"));
            pp.setRoleKind((String) row.get("roleKind"));
            pp.setScore((BigDecimal) row.get("score"));
            return pp;
        }).collect(Collectors.toList()));

        return vo;
    }

    // ─── 收藏 ───

    @Override
    public Favorite checkFavorite(Long userId, Long movieId) {
        return favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getMovieId, movieId));
    }

    @Override
    public void addFavorite(Long userId, Long movieId, Integer status) {
        Favorite existing = checkFavorite(userId, movieId);
        if (existing != null) {
            if (!existing.getStatus().equals(status)) {
                existing.setStatus(status);
                favoriteMapper.updateById(existing);
            }
        } else {
            Favorite fav = new Favorite();
            fav.setUserId(userId);
            fav.setMovieId(movieId);
            fav.setStatus(status);
            favoriteMapper.insert(fav);
        }
    }

    @Override
    public void removeFavorite(Long userId, Long movieId) {
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getMovieId, movieId));
    }

    // ─── 评分 ───

    @Override
    public Rating checkRating(Long userId, Long movieId) {
        return ratingMapper.selectOne(new LambdaQueryWrapper<Rating>()
                .eq(Rating::getUserId, userId)
                .eq(Rating::getMovieId, movieId));
    }

    @Override
    public void saveRating(Long userId, Long movieId, Integer score, String comment) {
        Rating existing = checkRating(userId, movieId);
        if (existing != null) {
            existing.setScore(score);
            existing.setComment(comment);
            ratingMapper.updateById(existing);
        } else {
            Rating rating = new Rating();
            rating.setUserId(userId);
            rating.setMovieId(movieId);
            rating.setScore(score);
            rating.setComment(comment);
            ratingMapper.insert(rating);
        }
        updateMovieLocalRating(movieId);

        // 同步评论到 movie_comment 表
        if (comment != null && !comment.isBlank()) {
            MovieComment existingComment = movieCommentMapper.selectOne(
                    new LambdaQueryWrapper<MovieComment>()
                            .eq(MovieComment::getUserId, userId)
                            .eq(MovieComment::getMovieId, movieId)
                            .eq(MovieComment::getSource, 1));
            if (existingComment != null) {
                existingComment.setContent(comment);
                existingComment.setRating(score != null ? (int) Math.ceil(score / 2.0) : null);
                movieCommentMapper.updateById(existingComment);
            } else {
                User user = userMapper.selectById(userId);
                MovieComment mc = new MovieComment();
                mc.setMovieId(movieId);
                mc.setSource(1);
                mc.setUserId(userId);
                mc.setAuthorName(user != null && user.getNickname() != null ? user.getNickname() : user != null ? user.getUsername() : "匿名用户");
                mc.setContent(comment);
                mc.setRating(score != null ? (int) Math.ceil(score / 2.0) : null);
                mc.setVotes(0);
                mc.setPostedAt(java.time.LocalDateTime.now());
                movieCommentMapper.insert(mc);
            }
        }
    }

    @Override
    public void deleteRating(Long userId, Long movieId) {
        ratingMapper.delete(new LambdaQueryWrapper<Rating>()
                .eq(Rating::getUserId, userId)
                .eq(Rating::getMovieId, movieId));
        updateMovieLocalRating(movieId);
    }

    private void updateMovieLocalRating(Long movieId) {
        List<Rating> ratings = ratingMapper.selectList(new LambdaQueryWrapper<Rating>()
                .eq(Rating::getMovieId, movieId));
        Movie movie = new Movie();
        movie.setId(movieId);
        if (ratings.isEmpty()) {
            movie.setLocalRating(BigDecimal.ZERO);
            movie.setLocalVotes(0);
        } else {
            BigDecimal avg = ratings.stream()
                    .map(r -> BigDecimal.valueOf(r.getScore()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(ratings.size()), 1, RoundingMode.HALF_UP);
            movie.setLocalRating(avg);
            movie.setLocalVotes(ratings.size());
        }
        movieMapper.updateById(movie);
    }

    // ─── 浏览记录 ───

    @Override
    public void recordView(Long userId, Long movieId) {
        ViewHistory vh = new ViewHistory();
        vh.setUserId(userId);
        vh.setMovieId(movieId);
        viewHistoryMapper.insert(vh);
    }

    private <T> IPage<T> paginate(List<T> list, long page, long size) {
        int from = (int) ((page - 1) * size);
        int to = (int) Math.min(from + size, list.size());
        List<T> subList = (from < list.size()) ? list.subList(from, to) : List.of();
        Page<T> result = new Page<>(page, size, list.size());
        result.setRecords(subList);
        return result;
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setBio(user.getBio());
        vo.setRole(user.getRole());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
