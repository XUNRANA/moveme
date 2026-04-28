package com.moveme.module.movie.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moveme.common.result.Result;
import com.moveme.module.movie.dto.MovieQueryDTO;
import com.moveme.module.movie.service.MovieService;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "电影模块")
@Validated
@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

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
            @Max(value = 50, message = "size must be <= 50") long size) {
        return Result.success(movieService.searchMovies(keyword, page, size));
    }

    @Operation(summary = "电影详情")
    @GetMapping("/{id}")
    public Result<MovieDetailVO> getMovieDetail(@PathVariable Long id) {
        return Result.success(movieService.getMovieDetail(id));
    }

    @Operation(summary = "电影类型列表")
    @GetMapping("/genres")
    public Result<List<String>> listGenres() {
        return Result.success(movieService.listGenres());
    }
}
