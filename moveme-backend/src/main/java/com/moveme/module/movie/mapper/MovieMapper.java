package com.moveme.module.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moveme.module.movie.entity.Movie;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MovieMapper extends BaseMapper<Movie> {

    /**
     * 根据 douban_id 去重写入/更新电影数据
     * INSERT ON DUPLICATE KEY UPDATE
     */
    @Insert("INSERT INTO movies (douban_id, title, original_title, poster_url, year, " +
            "douban_rating, douban_votes, summary, country, language, duration, release_date, imdb_id, status) " +
            "VALUES (#{m.doubanId}, #{m.title}, #{m.originalTitle}, #{m.posterUrl}, #{m.year}, " +
            "#{m.doubanRating}, #{m.doubanVotes}, #{m.summary}, #{m.country}, #{m.language}, " +
            "#{m.duration}, #{m.releaseDate}, #{m.imdbId}, 1) " +
            "ON DUPLICATE KEY UPDATE " +
            "title=VALUES(title), original_title=VALUES(original_title), poster_url=VALUES(poster_url), " +
            "year=VALUES(year), douban_rating=VALUES(douban_rating), douban_votes=VALUES(douban_votes), " +
            "summary=VALUES(summary), country=VALUES(country), language=VALUES(language), " +
            "duration=VALUES(duration), release_date=VALUES(release_date), imdb_id=VALUES(imdb_id)")
    @Options(useGeneratedKeys = true, keyProperty = "m.id")
    int upsertByDoubanId(@Param("m") Movie movie);
}
