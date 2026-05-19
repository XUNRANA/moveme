-- =============================================
-- MovieMe v2 数据库 schema （推倒重建版）
-- 38 张表，按域分组：
--   1) 用户域           : users
--   2) 字典域           : genres, countries, languages, tags, award_ceremonies
--   3) 内容主域         : movies, persons
--   4) 关联表（多对多） : movie_genre/country/language/tag/director/writer/actor
--   5) 富字段域         : movie_top250/aka/release_date/award/related/comment/rating_dist/genre_rank
--   6) 用户行为域       : ratings, favorites, view_history, search_history
--   7) 推荐特征域       : movie_features, user_features, user_genre_pref, user_person_pref,
--                         movie_similarity, movie_co_occurrence, user_reco_cache, llm_reco_log
--   8) 系统日志域       : crawl_logs, recommendation_logs, import_logs
-- =============================================

SET NAMES utf8mb4;

DROP DATABASE IF EXISTS moveme;
CREATE DATABASE moveme
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;
USE moveme;

-- =====================================================================
-- 1. 用户域
-- =====================================================================

CREATE TABLE users (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL COMMENT 'BCrypt 加密',
    email         VARCHAR(100) UNIQUE,
    nickname      VARCHAR(50),
    avatar_url    VARCHAR(500),
    bio           VARCHAR(500) COMMENT '个人简介',
    role          TINYINT NOT NULL DEFAULT 0 COMMENT '0=普通,1=管理员',
    status        TINYINT NOT NULL DEFAULT 1 COMMENT '0=禁用,1=启用',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =====================================================================
-- 2. 字典域（先于 movies / persons 创建，方便外键参考）
-- =====================================================================

CREATE TABLE genres (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL UNIQUE COMMENT '类型名',
    description VARCHAR(200)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电影类型字典';

CREATE TABLE countries (
    id    INT PRIMARY KEY AUTO_INCREMENT,
    name  VARCHAR(50) NOT NULL UNIQUE,
    code  VARCHAR(10) COMMENT 'ISO 国家代码（可选）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='国家/地区字典';

CREATE TABLE languages (
    id    INT PRIMARY KEY AUTO_INCREMENT,
    name  VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语言字典';

CREATE TABLE tags (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL UNIQUE,
    usage_count INT DEFAULT 0 COMMENT '被多少电影打过',
    INDEX idx_usage (usage_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签字典';

CREATE TABLE award_ceremonies (
    id           INT PRIMARY KEY AUTO_INCREMENT,
    name         VARCHAR(200) NOT NULL UNIQUE COMMENT '奥斯卡金像奖/金球奖/...',
    organization VARCHAR(200),
    description  VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='颁奖典礼字典';

-- =====================================================================
-- 3. 内容主域：movies + persons
-- =====================================================================

CREATE TABLE movies (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    douban_id         VARCHAR(20) UNIQUE COMMENT '豆瓣 subject_id',
    imdb_id           VARCHAR(20) COMMENT 'tt0111161',

    title             VARCHAR(200) NOT NULL COMMENT '主标题，"肖申克的救赎 The Shawshank Redemption"',
    title_cn          VARCHAR(200) COMMENT '中文名',
    title_en          VARCHAR(200) COMMENT '英文/原名',
    title_pinyin      VARCHAR(300) COMMENT '中文拼音，搜索用',

    summary           TEXT COMMENT '完整剧情简介',
    summary_short     VARCHAR(500) COMMENT '截断到200字的简介，列表页用',

    year              SMALLINT,
    duration_minutes  SMALLINT COMMENT '解析后的分钟数',
    duration_text     VARCHAR(100) COMMENT '"142分钟" / "120分钟(导演剪辑版)"',
    release_date      DATE COMMENT '最早一次上映',

    poster_url        VARCHAR(500) COMMENT '远程海报 URL',
    poster_local_path VARCHAR(500) COMMENT '/static/posters/{douban_id}.jpg',
    backdrop_url      VARCHAR(500) COMMENT '横版大图（详情页 hero）',
    trailer_url       VARCHAR(500),
    official_site     VARCHAR(500),

    douban_rating     DECIMAL(3,1) COMMENT '豆瓣评分',
    douban_votes      INT DEFAULT 0,
    local_rating      DECIMAL(3,1) DEFAULT 0.0 COMMENT '站内评分（基于 ratings 计算）',
    local_votes       INT DEFAULT 0,

    wish_count        INT DEFAULT 0 COMMENT '豆瓣 interest_counts.wish 想看人数',
    collect_count     INT DEFAULT 0 COMMENT '豆瓣 interest_counts.collect 看过人数',

    -- 推荐用打分（离线 job 写入）
    popularity_score  DECIMAL(7,3) DEFAULT 0 COMMENT '热门度：log(votes)*rating + 时间衰减',
    quality_score     DECIMAL(7,3) DEFAULT 0 COMMENT '质量分：rating + 奖项加成',
    freshness_score   DECIMAL(7,3) DEFAULT 0 COMMENT '新鲜度：基于 release_date',

    detail_fetched_at DATETIME NULL COMMENT '最近一次成功抓详情页的时间',
    syn_scanned_at    DATETIME NULL COMMENT 'syn_ 人物解析已扫描过此电影',
    last_crawled_at   DATETIME NULL,
    status            TINYINT NOT NULL DEFAULT 1 COMMENT '0=隐藏,1=正常',
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_douban_id     (douban_id),
    INDEX idx_year          (year),
    INDEX idx_douban_rating (douban_rating),
    INDEX idx_popularity    (popularity_score),
    INDEX idx_release_date  (release_date),
    FULLTEXT INDEX ft_title_summary (title, title_cn, title_en, summary) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电影主表';

CREATE TABLE persons (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    douban_person_id  VARCHAR(20) UNIQUE,
    name              VARCHAR(100) NOT NULL COMMENT '中文名',
    name_en           VARCHAR(150) COMMENT '英文名/原名',
    avatar_url        VARCHAR(500),
    avatar_local_path VARCHAR(500),
    profile_url       VARCHAR(500) COMMENT 'https://www.douban.com/personage/xxx',
    gender            TINYINT COMMENT '0=未知,1=男,2=女',
    birth_date        DATE,
    birth_place       VARCHAR(200),
    bio               TEXT,
    -- 推荐用聚合（离线 job 写入）
    movie_count       INT DEFAULT 0 COMMENT '参与的电影数',
    avg_movie_rating  DECIMAL(3,1) DEFAULT 0,
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人物表（导演/编剧/演员去重存储）';

-- =====================================================================
-- 4. 关联表（多对多）
-- =====================================================================

CREATE TABLE movie_genre (
    movie_id BIGINT NOT NULL,
    genre_id INT NOT NULL,
    PRIMARY KEY (movie_id, genre_id),
    INDEX idx_genre_movie (genre_id, movie_id),
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='电影-类型';

CREATE TABLE movie_country (
    movie_id   BIGINT NOT NULL,
    country_id INT NOT NULL,
    PRIMARY KEY (movie_id, country_id),
    INDEX idx_country_movie (country_id, movie_id),
    FOREIGN KEY (movie_id)   REFERENCES movies(id)    ON DELETE CASCADE,
    FOREIGN KEY (country_id) REFERENCES countries(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='电影-国家';

CREATE TABLE movie_language (
    movie_id    BIGINT NOT NULL,
    language_id INT NOT NULL,
    PRIMARY KEY (movie_id, language_id),
    INDEX idx_language_movie (language_id, movie_id),
    FOREIGN KEY (movie_id)    REFERENCES movies(id)    ON DELETE CASCADE,
    FOREIGN KEY (language_id) REFERENCES languages(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='电影-语言';

CREATE TABLE movie_tag (
    movie_id BIGINT NOT NULL,
    tag_id   INT NOT NULL,
    PRIMARY KEY (movie_id, tag_id),
    INDEX idx_tag_movie (tag_id, movie_id),
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id)   REFERENCES tags(id)   ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='电影-标签';

CREATE TABLE movie_director (
    movie_id   BIGINT NOT NULL,
    person_id  BIGINT NOT NULL,
    sort_order INT DEFAULT 0,
    PRIMARY KEY (movie_id, person_id),
    INDEX idx_person_movie (person_id, movie_id),
    FOREIGN KEY (movie_id)  REFERENCES movies(id)  ON DELETE CASCADE,
    FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='电影-导演';

CREATE TABLE movie_writer (
    movie_id   BIGINT NOT NULL,
    person_id  BIGINT NOT NULL,
    sort_order INT DEFAULT 0,
    PRIMARY KEY (movie_id, person_id),
    INDEX idx_person_movie (person_id, movie_id),
    FOREIGN KEY (movie_id)  REFERENCES movies(id)  ON DELETE CASCADE,
    FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='电影-编剧';

CREATE TABLE movie_actor (
    movie_id   BIGINT NOT NULL,
    person_id  BIGINT NOT NULL,
    role_name  VARCHAR(200) COMMENT '"饰 安迪·杜佛兰 Andy Dufresne"',
    sort_order INT DEFAULT 0,
    is_lead    TINYINT DEFAULT 0 COMMENT 'sort_order<=5 视为主演，冗余加速',
    PRIMARY KEY (movie_id, person_id),
    INDEX idx_person_movie (person_id, movie_id),
    INDEX idx_lead (is_lead, movie_id),
    FOREIGN KEY (movie_id)  REFERENCES movies(id)  ON DELETE CASCADE,
    FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电影-演员';

-- =====================================================================
-- 5. 富字段域（豆瓣特色：Top250 / 又名 / 多上映 / 奖项 / 相关 / 短评 / 评分分布 / 类型排名）
-- =====================================================================

CREATE TABLE movie_top250 (
    movie_id    BIGINT PRIMARY KEY,
    rank_no     SMALLINT NOT NULL UNIQUE COMMENT '1..250',
    list_title  VARCHAR(100),
    quote       VARCHAR(500) COMMENT '"希望让人自由" 这种一句话',
    snapshot_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rank (rank_no),
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Top250 排名快照';

CREATE TABLE movie_aka (
    id       BIGINT PRIMARY KEY AUTO_INCREMENT,
    movie_id BIGINT NOT NULL,
    title    VARCHAR(200) NOT NULL COMMENT '"月黑高飞" / "刺激1995"',
    INDEX idx_movie (movie_id),
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电影又名';

CREATE TABLE movie_release_date (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    movie_id   BIGINT NOT NULL,
    release_at DATE,
    region     VARCHAR(80) COMMENT '"美国" / "多伦多电影节" / "中国大陆"',
    raw_text   VARCHAR(120) COMMENT '原始文本，例如 "1994-09-10(多伦多电影节)"',
    INDEX idx_movie (movie_id),
    INDEX idx_release_at (release_at),
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电影上映日期（多地）';

CREATE TABLE movie_award (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    movie_id            BIGINT NOT NULL,
    ceremony_id         INT NULL COMMENT '关联 award_ceremonies 字典',
    ceremony_text       VARCHAR(200) NOT NULL COMMENT '"第67届奥斯卡金像奖"',
    category            VARCHAR(200) NOT NULL COMMENT '"最佳影片"/"最佳男主角"',
    status              ENUM('won','nominated','unknown') DEFAULT 'unknown',
    award_url           VARCHAR(500),
    recipient_person_id BIGINT NULL COMMENT '若颁给某人则关联 persons',
    recipient_text      VARCHAR(200) COMMENT '原始 recipients 文本',
    INDEX idx_movie (movie_id),
    INDEX idx_status (status),
    INDEX idx_ceremony (ceremony_id),
    FOREIGN KEY (movie_id)            REFERENCES movies(id)            ON DELETE CASCADE,
    FOREIGN KEY (ceremony_id)         REFERENCES award_ceremonies(id)  ON DELETE SET NULL,
    FOREIGN KEY (recipient_person_id) REFERENCES persons(id)           ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电影获奖/提名记录';

CREATE TABLE movie_related (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    movie_id          BIGINT NOT NULL,
    related_movie_id  BIGINT NULL COMMENT '若库内有则关联，否则 NULL',
    related_douban_id VARCHAR(20),
    related_title     VARCHAR(200),
    related_rating    DECIMAL(3,1),
    related_cover_url VARCHAR(500),
    sort_order        INT DEFAULT 0,
    INDEX idx_movie (movie_id),
    INDEX idx_related_movie (related_movie_id),
    INDEX idx_related_douban (related_douban_id),
    FOREIGN KEY (movie_id)         REFERENCES movies(id) ON DELETE CASCADE,
    FOREIGN KEY (related_movie_id) REFERENCES movies(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='豆瓣相关推荐电影';

CREATE TABLE movie_play_link (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    movie_id          BIGINT NOT NULL,
    platform          VARCHAR(50)  COMMENT '平台名称，如：腾讯视频、咪咕视频、bilibili',
    url               VARCHAR(500) COMMENT '播放链接',
    INDEX idx_movie (movie_id),
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流媒体播放链接';

CREATE TABLE movie_comment (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    movie_id          BIGINT NOT NULL,
    source            TINYINT NOT NULL DEFAULT 0 COMMENT '0=豆瓣抓取,1=站内用户',
    douban_comment_id VARCHAR(40) UNIQUE COMMENT '豆瓣短评 id；站内则 NULL',
    user_id           BIGINT NULL COMMENT '站内用户 id；豆瓣评论则 NULL',
    author_name       VARCHAR(100) COMMENT '豆瓣昵称（豆瓣源）',
    author_avatar     VARCHAR(500),
    author_location   VARCHAR(100),
    rating            TINYINT COMMENT '1-5 星，对应豆瓣 rating.value',
    rating_label      VARCHAR(20) COMMENT '"力荐"/"推荐"',
    content           TEXT NOT NULL,
    votes             INT DEFAULT 0 COMMENT '点赞数',
    posted_at         DATETIME,
    source_url        VARCHAR(500),
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_movie_votes (movie_id, votes DESC),
    INDEX idx_user (user_id),
    INDEX idx_source (source),
    FULLTEXT INDEX ft_content (content) WITH PARSER ngram,
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)  REFERENCES users(id)  ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电影短评（豆瓣 + 站内）';

CREATE TABLE comment_vote (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_comment_user (comment_id, user_id),
    INDEX idx_comment (comment_id),
    INDEX idx_user (user_id),
    FOREIGN KEY (comment_id) REFERENCES movie_comment(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)    REFERENCES users(id)         ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞';

CREATE TABLE movie_rating_dist (
    movie_id   BIGINT NOT NULL,
    star       TINYINT NOT NULL COMMENT '1-5',
    label      VARCHAR(20) COMMENT '"力荐" 等',
    percentage DECIMAL(5,2) NOT NULL,
    PRIMARY KEY (movie_id, star),
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电影 5 星评分分布';

CREATE TABLE movie_genre_rank (
    movie_id   BIGINT NOT NULL,
    genre_name VARCHAR(50) NOT NULL,
    percentile DECIMAL(5,2) NOT NULL COMMENT '"99% 好于剧情片"',
    rank_url   VARCHAR(500),
    PRIMARY KEY (movie_id, genre_name),
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='在该类型中的百分位排名';

-- =====================================================================
-- 6. 用户行为域
-- =====================================================================

CREATE TABLE ratings (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT NOT NULL,
    movie_id   BIGINT NOT NULL,
    score      TINYINT NOT NULL COMMENT '1-10',
    comment    TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_movie (user_id, movie_id),
    INDEX idx_movie   (movie_id),
    INDEX idx_user    (user_id),
    INDEX idx_created (created_at),
    FOREIGN KEY (user_id)  REFERENCES users(id)  ON DELETE CASCADE,
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内用户评分';

CREATE TABLE favorites (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT NOT NULL,
    movie_id   BIGINT NOT NULL,
    status     TINYINT NOT NULL DEFAULT 0 COMMENT '0=想看,1=已看',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_movie (user_id, movie_id),
    INDEX idx_user_status (user_id, status),
    INDEX idx_movie       (movie_id),
    FOREIGN KEY (user_id)  REFERENCES users(id)  ON DELETE CASCADE,
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='想看/已看 收藏';

CREATE TABLE view_history (
    id        BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id   BIGINT NOT NULL,
    movie_id  BIGINT NOT NULL,
    viewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_viewed (user_id, viewed_at DESC),
    INDEX idx_movie       (movie_id),
    FOREIGN KEY (user_id)  REFERENCES users(id)  ON DELETE CASCADE,
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='浏览详情页历史';

CREATE TABLE search_history (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NULL,
    keyword     VARCHAR(200) NOT NULL,
    result_cnt  INT,
    searched_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_searched (user_id, searched_at DESC),
    INDEX idx_keyword       (keyword),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搜索历史';

-- =====================================================================
-- 7. 推荐特征域（推荐系统的核心数据：特征、相似度、共现、用户偏好、缓存）
-- =====================================================================

CREATE TABLE movie_features (
    movie_id        BIGINT PRIMARY KEY,
    genre_vector    JSON COMMENT '类型 one-hot/TF-IDF：{"剧情":0.5,"犯罪":0.5}',
    tag_vector      JSON COMMENT '标签 TF-IDF',
    cast_vector     JSON COMMENT '主创 TF-IDF：{"person_id_1":w1,...}',
    decade          SMALLINT COMMENT '年代分桶：1990s -> 1990',
    cluster_id      INT COMMENT 'KMeans 聚类 id（可选）',
    embedding       JSON COMMENT '可选：低维 dense 向量',
    feature_version VARCHAR(20) NOT NULL DEFAULT 'v1',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cluster (cluster_id),
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='电影特征向量（离线计算）';

CREATE TABLE user_features (
    user_id          BIGINT PRIMARY KEY,
    genre_vector     JSON,
    tag_vector       JSON,
    cast_vector      JSON,
    avg_year         SMALLINT COMMENT '常看电影的平均年代',
    avg_rating_given DECIMAL(3,1) COMMENT '该用户的打分均值',
    rating_count     INT DEFAULT 0,
    favorite_count   INT DEFAULT 0,
    view_count       INT DEFAULT 0,
    feature_version  VARCHAR(20) NOT NULL DEFAULT 'v1',
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='用户偏好向量（基于行为聚合）';

CREATE TABLE user_genre_pref (
    user_id    BIGINT NOT NULL,
    genre_id   INT NOT NULL,
    score      DECIMAL(6,3) NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, genre_id),
    INDEX idx_user_score (user_id, score DESC),
    FOREIGN KEY (user_id)  REFERENCES users(id)  ON DELETE CASCADE,
    FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='用户对类型的偏好分';

CREATE TABLE user_person_pref (
    user_id    BIGINT NOT NULL,
    person_id  BIGINT NOT NULL,
    role_kind  ENUM('director','actor','writer') NOT NULL,
    score      DECIMAL(6,3) NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, person_id, role_kind),
    INDEX idx_user_score (user_id, score DESC),
    FOREIGN KEY (user_id)   REFERENCES users(id)   ON DELETE CASCADE,
    FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='用户对导演/演员/编剧的偏好分';

CREATE TABLE movie_similarity (
    movie_id_a BIGINT NOT NULL,
    movie_id_b BIGINT NOT NULL,
    algorithm  VARCHAR(20) NOT NULL COMMENT 'CONTENT/CF_ITEM/EMBEDDING',
    score      DECIMAL(6,4) NOT NULL,
    rank_no    SMALLINT COMMENT '在该算法下 a 的相似排名',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (movie_id_a, movie_id_b, algorithm),
    INDEX idx_a_alg_rank  (movie_id_a, algorithm, rank_no),
    INDEX idx_a_alg_score (movie_id_a, algorithm, score DESC),
    FOREIGN KEY (movie_id_a) REFERENCES movies(id) ON DELETE CASCADE,
    FOREIGN KEY (movie_id_b) REFERENCES movies(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='电影间相似度（多算法分行存）';

CREATE TABLE movie_co_occurrence (
    movie_id_a BIGINT NOT NULL,
    movie_id_b BIGINT NOT NULL,
    co_count   INT NOT NULL COMMENT '共同被多少用户喜欢/看过',
    pmi        DECIMAL(7,4) COMMENT '点互信息：log(p(a,b)/p(a)/p(b))',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (movie_id_a, movie_id_b),
    INDEX idx_a_count (movie_id_a, co_count DESC),
    FOREIGN KEY (movie_id_a) REFERENCES movies(id) ON DELETE CASCADE,
    FOREIGN KEY (movie_id_b) REFERENCES movies(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='共现矩阵（用户行为聚合）';

CREATE TABLE user_reco_cache (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT NOT NULL,
    strategy   VARCHAR(20) NOT NULL COMMENT 'CONTENT/CF/LLM/HYBRID/COLD_START',
    movie_ids  JSON NOT NULL,
    reasons    JSON COMMENT '每部为什么推（"因为你看过 X"）',
    score_map  JSON COMMENT '推荐分 movie_id -> score',
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_strategy (user_id, strategy, expires_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='推荐结果缓存';

CREATE TABLE llm_reco_log (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id       BIGINT NOT NULL,
    provider      VARCHAR(30) COMMENT 'CLAUDE/GEMINI/OPENAI',
    model         VARCHAR(50),
    prompt        TEXT,
    raw_response  TEXT,
    parsed_movies JSON,
    input_tokens  INT,
    output_tokens INT,
    cost_usd      DECIMAL(7,4),
    latency_ms    INT,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_created (user_id, created_at),
    INDEX idx_provider     (provider),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LLM 推荐调用记录';

-- =====================================================================
-- 8. 系统日志域
-- =====================================================================

CREATE TABLE crawl_logs (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_type     VARCHAR(50) NOT NULL,
    status        TINYINT NOT NULL COMMENT '0=运行中,1=成功,2=失败',
    total_count   INT DEFAULT 0,
    success_count INT DEFAULT 0,
    fail_count    INT DEFAULT 0,
    error_message TEXT,
    started_at    DATETIME NOT NULL,
    finished_at   DATETIME,
    INDEX idx_task_type  (task_type),
    INDEX idx_started_at (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='爬虫任务日志';

CREATE TABLE recommendation_logs (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id       BIGINT NOT NULL,
    strategy_type VARCHAR(50) NOT NULL,
    request_data  JSON,
    response_data JSON,
    llm_provider  VARCHAR(30),
    latency_ms    INT,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_created (user_id, created_at),
    INDEX idx_strategy     (strategy_type),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='推荐请求/响应日志';

CREATE TABLE import_logs (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    source       VARCHAR(50) NOT NULL COMMENT 'TOP250_JSON / ...',
    file_path    VARCHAR(500),
    movies_total INT DEFAULT 0,
    movies_ok    INT DEFAULT 0,
    movies_fail  INT DEFAULT 0,
    persons_ok   INT DEFAULT 0,
    comments_ok  INT DEFAULT 0,
    errors       JSON COMMENT '失败明细',
    started_at   DATETIME NOT NULL,
    finished_at  DATETIME,
    INDEX idx_source_started (source, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='种子/批量导入日志';

-- =====================================================================
-- 9. 年度榜单域
-- =====================================================================

CREATE TABLE IF NOT EXISTS movie_annual (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    annual_year  SMALLINT     NOT NULL COMMENT '年度，如2025',
    board_title  VARCHAR(100) NOT NULL COMMENT '榜单分类名，如评分最高华语电影',
    board_order  INT          DEFAULT 0 COMMENT '榜单在页面上的显示顺序',
    movie_id     BIGINT       NOT NULL COMMENT '关联movies.id',
    rank_no      INT          COMMENT '排名',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_year_board_movie (annual_year, board_title, movie_id),
    KEY idx_year (annual_year),
    CONSTRAINT fk_annual_movie FOREIGN KEY (movie_id) REFERENCES movies (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='年度榜单';

-- =====================================================================
-- 种子数据：字典 + 默认管理员
-- =====================================================================

-- 类型字典（豆瓣常见 30 类）
INSERT INTO genres (name) VALUES
 ('剧情'),('喜剧'),('动作'),('爱情'),('科幻'),('悬疑'),('恐怖'),('动画'),
 ('犯罪'),('冒险'),('奇幻'),('战争'),('历史'),('传记'),('音乐'),('歌舞'),
 ('纪录片'),('家庭'),('武侠'),('古装'),('西部'),('运动'),('情色'),('灾难'),
 ('黑色电影'),('儿童'),('短片'),('真人秀'),('脱口秀'),('同性');

-- 国家字典（top250.json 里出现的常见国家/地区）
INSERT INTO countries (name) VALUES
 ('美国'),('中国大陆'),('中国香港'),('中国台湾'),('日本'),('韩国'),('英国'),
 ('法国'),('德国'),('意大利'),('西班牙'),('印度'),('俄罗斯'),('加拿大'),
 ('澳大利亚'),('泰国'),('伊朗'),('巴西'),('墨西哥'),('阿根廷'),('瑞典'),
 ('丹麦'),('荷兰'),('爱尔兰'),('新西兰');

-- 语言字典
INSERT INTO languages (name) VALUES
 ('汉语普通话'),('英语'),('粤语'),('日语'),('韩语'),('法语'),('德语'),
 ('意大利语'),('西班牙语'),('俄语'),('印地语'),('泰语'),('阿拉伯语'),
 ('葡萄牙语'),('希伯来语'),('拉丁语'),('波斯语');

-- 颁奖典礼字典
INSERT INTO award_ceremonies (name, organization) VALUES
 ('奥斯卡金像奖', '美国电影艺术与科学学院'),
 ('金球奖', '好莱坞外国记者协会'),
 ('英国电影学院奖', '英国电影和电视艺术学院 BAFTA'),
 ('戛纳电影节', '法国戛纳国际电影节组委会'),
 ('威尼斯电影节', '威尼斯国际电影节组委会'),
 ('柏林国际电影节', '柏林国际电影节组委会'),
 ('金马奖', '台湾电影金马奖执行委员会'),
 ('金像奖', '香港电影金像奖协会'),
 ('华表奖', '中国电影华表奖'),
 ('百花奖', '中国电影百花奖'),
 ('金鸡奖', '中国电影金鸡奖'),
 ('日本电影学院奖', '日本电影学院');

-- 默认管理员（密码 admin123，BCrypt cost=12）
INSERT INTO users (username, password, email, nickname, role, status) VALUES
 ('admin',
  '$2a$12$i9auj7VDf1e4SamjYQuM7eRAQm8CRfCn0OHre5MurWd4f17G./KuG',
  'admin@moveme.com',
  '管理员',
  1, 1);
