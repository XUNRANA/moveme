-- =============================================
-- MovieMe 电影推荐系统 - 数据库初始化脚本
-- =============================================

CREATE DATABASE IF NOT EXISTS moveme
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE moveme;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL COMMENT 'BCrypt加密',
    email         VARCHAR(100) UNIQUE,
    nickname      VARCHAR(50),
    avatar_url    VARCHAR(500),
    role          TINYINT      NOT NULL DEFAULT 0 COMMENT '0=普通用户, 1=管理员',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '0=禁用, 1=启用',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 电影表
CREATE TABLE IF NOT EXISTS movies (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    douban_id       VARCHAR(20) UNIQUE COMMENT '豆瓣ID',
    title           VARCHAR(200) NOT NULL COMMENT '电影名称',
    original_title  VARCHAR(200) COMMENT '原始名称',
    poster_url      VARCHAR(500) COMMENT '海报URL',
    year            SMALLINT COMMENT '上映年份',
    douban_rating   DECIMAL(3,1) COMMENT '豆瓣评分',
    douban_votes    INT DEFAULT 0 COMMENT '豆瓣评分人数',
    local_rating    DECIMAL(3,1) DEFAULT 0.0 COMMENT '本站评分',
    local_votes     INT DEFAULT 0 COMMENT '本站评分人数',
    summary         TEXT COMMENT '剧情简介',
    country         VARCHAR(100) COMMENT '制片国家',
    language        VARCHAR(100) COMMENT '语言',
    duration        VARCHAR(50) COMMENT '片长',
    release_date    DATE COMMENT '上映日期',
    imdb_id         VARCHAR(20) COMMENT 'IMDb ID',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '0=隐藏, 1=正常',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_douban_id (douban_id),
    INDEX idx_title (title),
    INDEX idx_year (year),
    INDEX idx_douban_rating (douban_rating),
    FULLTEXT INDEX ft_title_summary (title, summary) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电影表';

-- 类型表
CREATE TABLE IF NOT EXISTS genres (
    id    INT PRIMARY KEY AUTO_INCREMENT,
    name  VARCHAR(50) NOT NULL UNIQUE COMMENT '类型名称'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电影类型表';

-- 电影-类型关联表
CREATE TABLE IF NOT EXISTS movie_genre (
    movie_id  BIGINT NOT NULL,
    genre_id  INT NOT NULL,
    PRIMARY KEY (movie_id, genre_id),
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='电影类型关联表';

-- 电影演员表
CREATE TABLE IF NOT EXISTS movie_actors (
    id        BIGINT PRIMARY KEY AUTO_INCREMENT,
    movie_id  BIGINT NOT NULL,
    name      VARCHAR(100) NOT NULL COMMENT '演员姓名',
    role_name VARCHAR(100) COMMENT '角色名',
    sort_order INT DEFAULT 0,
    INDEX idx_movie_id (movie_id),
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电影演员表';

-- 电影导演表
CREATE TABLE IF NOT EXISTS movie_directors (
    id        BIGINT PRIMARY KEY AUTO_INCREMENT,
    movie_id  BIGINT NOT NULL,
    name      VARCHAR(100) NOT NULL COMMENT '导演姓名',
    INDEX idx_movie_id (movie_id),
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电影导演表';

-- 评分表
CREATE TABLE IF NOT EXISTS ratings (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT NOT NULL,
    movie_id   BIGINT NOT NULL,
    score      TINYINT NOT NULL COMMENT '评分1-10',
    comment    TEXT COMMENT '短评',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_movie (user_id, movie_id),
    INDEX idx_movie_id (movie_id),
    INDEX idx_user_id (user_id),
    FOREIGN KEY (user_id)  REFERENCES users(id)  ON DELETE CASCADE,
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评分表';

-- 收藏表
CREATE TABLE IF NOT EXISTS favorites (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT NOT NULL,
    movie_id   BIGINT NOT NULL,
    status     TINYINT NOT NULL DEFAULT 0 COMMENT '0=想看, 1=已看',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_movie (user_id, movie_id),
    INDEX idx_user_id (user_id),
    FOREIGN KEY (user_id)  REFERENCES users(id)  ON DELETE CASCADE,
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='收藏表';

-- 爬虫日志表
CREATE TABLE IF NOT EXISTS crawl_logs (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_type       VARCHAR(50) NOT NULL COMMENT '任务类型',
    status          TINYINT NOT NULL COMMENT '0=运行中, 1=成功, 2=失败',
    total_count     INT DEFAULT 0,
    success_count   INT DEFAULT 0,
    fail_count      INT DEFAULT 0,
    error_message   TEXT,
    started_at      DATETIME NOT NULL,
    finished_at     DATETIME,
    INDEX idx_task_type (task_type),
    INDEX idx_started_at (started_at)
) ENGINE=InnoDB COMMENT='爬虫日志表';

-- 推荐日志表
CREATE TABLE IF NOT EXISTS recommendation_logs (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    strategy_type   VARCHAR(50) NOT NULL COMMENT 'COLLABORATIVE/CONTENT_BASED/LLM',
    request_data    JSON COMMENT '请求数据',
    response_data   JSON COMMENT '响应数据',
    llm_provider    VARCHAR(30) COMMENT 'GEMINI/OPENAI/CLAUDE',
    latency_ms      INT COMMENT '响应耗时ms',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_strategy (strategy_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='推荐日志表';
