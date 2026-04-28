package com.moveme.module.movie.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moveme.module.movie.dto.MovieQueryDTO;
import com.moveme.module.movie.vo.MovieDetailVO;
import com.moveme.module.movie.vo.MovieVO;

import java.util.List;

public interface MovieService {

    IPage<MovieVO> listMovies(MovieQueryDTO query);

    IPage<MovieVO> searchMovies(String keyword, long page, long size);

    MovieDetailVO getMovieDetail(Long id);

    List<String> listGenres();
}
