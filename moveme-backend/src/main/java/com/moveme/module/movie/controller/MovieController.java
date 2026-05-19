package com.moveme.module.movie.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moveme.common.result.Result;
import com.moveme.module.movie.dto.MovieQueryDTO;
import com.moveme.module.movie.service.MovieService;
import com.moveme.module.user.entity.SearchHistory;
import com.moveme.module.user.mapper.SearchHistoryMapper;
import com.moveme.module.movie.vo.BoardVO;
import com.moveme.module.movie.vo.DiscoverSectionVO;
import com.moveme.module.movie.vo.MovieChartVO;
import com.moveme.module.movie.vo.MovieDetailVO;
import com.moveme.module.movie.vo.MovieVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "电影模块")
@Validated
@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final SearchHistoryMapper searchHistoryMapper;

    @Operation(summary = "电影列表")
    @GetMapping
    public Result<IPage<MovieVO>> listMovies(@Valid MovieQueryDTO query) {
        return Result.success(movieService.listMovies(query));
    }

    @Operation(summary = "全文搜索")
    @GetMapping("/search")
    public Result<IPage<MovieVO>> searchMovies(
            @RequestParam("q") @NotBlank(message = "q must not be blank")
            @Size(max = 100, message = "q is too long") String keyword,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be >= 1") long page,
            @RequestParam(defaultValue = "12") @Min(value = 1, message = "size must be >= 1")
            @Max(value = 50, message = "size must be <= 50") long size,
            Authentication authentication) {
        IPage<MovieVO> result = movieService.searchMovies(keyword.trim(), page, size);
        // 记录搜索历史
        Long userId = authentication != null ? (Long) authentication.getPrincipal() : null;
        SearchHistory sh = new SearchHistory();
        sh.setUserId(userId);
        sh.setKeyword(keyword.trim());
        sh.setResultCnt((int) result.getTotal());
        searchHistoryMapper.insert(sh);
        return Result.success(result);
    }

    @Operation(summary = "排行榜类型列表")
    @GetMapping("/chart-genres")
    public Result<List<String>> listChartGenres() {
        return Result.success(movieService.listChartGenres());
    }

    @Operation(summary = "分类排行榜")
    @GetMapping("/chart-genres/{genre}")
    public Result<MovieChartVO> getChartByGenre(@PathVariable String genre) {
        return Result.success(movieService.getChartByGenre(genre));
    }

    @Operation(summary = "年度榜单年份列表")
    @GetMapping("/annual-years")
    public Result<List<Integer>> listAnnualYears() {
        return Result.success(movieService.listAnnualYears());
    }

    @Operation(summary = "年度榜单")
    @GetMapping("/annual/{year}")
    public Result<List<MovieChartVO>> getAnnualByYear(@PathVariable Integer year) {
        return Result.success(movieService.getAnnualByYear(year));
    }

    @Operation(summary = "热门榜单列表")
    @GetMapping("/boards")
    public Result<List<BoardVO>> listBoards() {
        return Result.success(movieService.listBoards());
    }

    @Operation(summary = "榜单电影列表")
    @GetMapping("/boards/{boardName}")
    public Result<MovieChartVO> getBoardMovies(
            @PathVariable @NotBlank @Size(max = 50) String boardName) {
        return Result.success(movieService.getBoardMovies(boardName));
    }

    @Operation(summary = "Top250 榜单")
    @GetMapping("/top250")
    public Result<MovieChartVO> listTop250() {
        return Result.success(movieService.listTop250());
    }

    @Operation(summary = "发现页推荐")
    @GetMapping("/discover")
    public Result<List<DiscoverSectionVO>> discover() {
        return Result.success(movieService.discover());
    }

    @Operation(summary = "电影类型列表")
    @GetMapping("/genres")
    public Result<List<String>> listGenres() {
        return Result.success(movieService.listGenres());
    }

    @Operation(summary = "电影详情")
    @GetMapping("/{id}")
    public Result<MovieDetailVO> getMovieDetail(@PathVariable Long id) {
        return Result.success(movieService.getMovieDetail(id));
    }

    @Operation(summary = "分页获取评论")
    @GetMapping("/{id}/comments")
    public Result<IPage<MovieDetailVO.CommentVO>> getMovieComments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) long size,
            @RequestParam(defaultValue = "hot") String sort,
            Authentication authentication) {
        Long userId = authentication != null ? (Long) authentication.getPrincipal() : null;
        return Result.success(movieService.getMovieComments(id, page, size, sort, userId));
    }

    @Operation(summary = "发表评论")
    @PostMapping("/{id}/comments")
    public Result<Void> submitComment(@PathVariable Long id,
                                      @RequestBody Map<String, Object> body,
                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String content = (String) body.get("content");
        Integer rating = body.get("rating") != null ? ((Number) body.get("rating")).intValue() : null;
        movieService.submitComment(userId, id, content, rating);
        return Result.success();
    }

    @Operation(summary = "点赞评论")
    @PostMapping("/comments/{commentId}/like")
    public Result<Void> likeComment(@PathVariable Long commentId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        movieService.likeComment(userId, commentId);
        return Result.success();
    }

    @Operation(summary = "取消点赞")
    @DeleteMapping("/comments/{commentId}/like")
    public Result<Void> unlikeComment(@PathVariable Long commentId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        movieService.unlikeComment(userId, commentId);
        return Result.success();
    }
}
