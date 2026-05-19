package com.moveme.common.constant;

public final class RedisKeyConstants {

    private RedisKeyConstants() {}

    public static final String REFRESH_TOKEN_PREFIX = "auth:refresh_token:";
    public static final String MOVIE_DETAIL_PREFIX = "movie:detail:";
    public static final String MOVIE_PAGE_PREFIX = "movie:page:";
    public static final String GENRE_LIST = "movie:genres";
    public static final String LLM_RECOMMEND_PREFIX = "llm:recommend:";
    public static final String SIMILARITY_MATRIX = "recommend:similarity_matrix";
    public static final String CRAWLER_LOCK_PREFIX = "crawler:lock:";
    public static final String PERSON_DETAIL_PREFIX = "person:detail:";
    public static final String MOVIE_CHART_PREFIX = "movie:chart:";
    public static final String MOVIE_CHART_GENRES = "movie:chart:genres";
    public static final String MOVIE_ANNUAL_PREFIX = "movie:annual:";
    public static final String MOVIE_ANNUAL_YEARS = "movie:annual:years";
    public static final String MOVIE_BOARDS = "movie:boards";
    public static final String MOVIE_DISCOVER = "movie:discover";
    public static final String MOVIE_TOP250 = "movie:top250";
    public static final String MOVIE_BOARD_MOVIES_PREFIX = "movie:board:";
}
