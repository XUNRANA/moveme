package com.moveme.common.constant;

public final class CrawlerConstants {

    private CrawlerConstants() {}

    /** 豆瓣按标签搜索电影 JSON API */
    public static final String DOUBAN_SEARCH_URL =
            "https://movie.douban.com/j/search_subjects?type=movie&tag=%s&page_limit=%d&page_start=%d";

    /** 豆瓣获取可用标签 */
    public static final String DOUBAN_TAGS_URL =
            "https://movie.douban.com/j/search_tags?type=movie";

    /** 豆瓣电影详情页 */
    public static final String DOUBAN_DETAIL_URL =
            "https://movie.douban.com/subject/%s/";

    /** User-Agent 池（模拟不同浏览器） */
    public static final String[] USER_AGENTS = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:125.0) Gecko/20100101 Firefox/125.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:125.0) Gecko/20100101 Firefox/125.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Edg/122.0.0.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64; rv:125.0) Gecko/20100101 Firefox/125.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    };

    /** 爬虫任务类型 */
    public static final String TASK_TYPE_TAG = "DOUBAN_TAG";
    public static final String TASK_TYPE_TOP250 = "DOUBAN_TOP250";
    public static final String TASK_TYPE_DETAIL = "DOUBAN_DETAIL";

    /** 爬虫状态 */
    public static final int CRAWL_STATUS_RUNNING = 0;
    public static final int CRAWL_STATUS_SUCCESS = 1;
    public static final int CRAWL_STATUS_FAILED = 2;
}
