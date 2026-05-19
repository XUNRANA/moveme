package com.moveme.module.movie.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moveme.module.movie.dto.MovieQueryDTO;
import com.moveme.module.movie.vo.BoardVO;
import com.moveme.module.movie.vo.DiscoverSectionVO;
import com.moveme.module.movie.vo.MovieChartVO;
import com.moveme.module.movie.vo.MovieDetailVO;
import com.moveme.module.movie.vo.MovieVO;

import java.util.List;

public interface MovieService {

    IPage<MovieVO> listMovies(MovieQueryDTO query);

    IPage<MovieVO> searchMovies(String keyword, long page, long size);

    MovieDetailVO getMovieDetail(Long id);

    List<String> listGenres();

    List<String> listChartGenres();

    MovieChartVO getChartByGenre(String genre);

    List<Integer> listAnnualYears();

    List<MovieChartVO> getAnnualByYear(Integer year);

    List<BoardVO> listBoards();

    MovieChartVO getBoardMovies(String boardName);

    MovieChartVO listTop250();

    List<DiscoverSectionVO> discover();

    IPage<MovieDetailVO.CommentVO> getMovieComments(Long movieId, long page, long size, String sort, Long userId);

    void submitComment(Long userId, Long movieId, String content, Integer rating);

    void likeComment(Long userId, Long commentId);

    void unlikeComment(Long userId, Long commentId);
}
