package com.moveme.common.constant;

public final class RedisKeyConstants {

    private RedisKeyConstants() {}

    public static final String REFRESH_TOKEN_PREFIX = "auth:refresh_token:";
    public static final String MOVIE_DETAIL_PREFIX = "movie:detail:";
    public static final String MOVIE_PAGE_PREFIX = "movie:page:";
    public static final String GENRE_LIST = "movie:genres";
    public static final String LLM_RECOMMEND_PREFIX = "llm:recommend:";
    public static final String SIMILARITY_MATRIX = "recommend:similarity_matrix";
}
