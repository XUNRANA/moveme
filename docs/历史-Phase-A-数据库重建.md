# 10 - Phase A：数据库重建（已完成）

> 本文档记录 Phase A 的完成里程碑：从 v1 (10 张表) 推倒重建为 v2 (38 张表)。
>
> 完成日期：2026-04-28

---

## 1. 背景

v1 schema 太薄，连 top250.json 1/3 的字段都装不下：
- 没有 persons 独立表
- 没有 comments / awards / related_movies / rating_dist / genre_rank
- 没有任何推荐特征数据结构（feature/similarity/co-occurrence/cache）
- 没有 view_history / search_history

决定推倒重建：DROP DATABASE → 全新 38 张表 schema。

---

## 2. 改动清单

### 2.1 新增/重写
- ✅ `docker/mysql/init/01-schema.sql`（完整重写，38 张表 + 46 个外键）
- ✅ `docker/mysql/init/02-seed-data.sql`（重写，30 genres + 25 countries + 17 languages + 12 ceremonies + admin）

### 2.2 删除
- ✅ `docker/mysql/init/03-crawler-upgrade.sql`（v1 → v1.5 迁移脚本，不再需要）
- ✅ `docs/04-开发计划与实现指南.md`（旧 Phase 1-7 路线，已被 10/11/12 取代）

### 2.3 重命名（标记历史归档）
- ✅ `docs/03-Phase1-基础骨架开发文档.md` → `docs/历史-Phase1-基础骨架开发文档.md`
- ✅ `docs/05-Phase2-豆瓣爬虫开发文档.md` → `docs/历史-Phase2-豆瓣爬虫开发文档.md`

---

## 3. 38 张表分布

| 域 | 表数 | 表名 |
|---|---|---|
| 用户域 | 1 | users |
| 内容字典域 | 5 | genres, countries, languages, tags, award_ceremonies |
| 内容主域 | 2 | movies, persons |
| 关联表域 | 7 | movie_genre, movie_country, movie_language, movie_tag, movie_director, movie_writer, movie_actor |
| 富字段域 | 8 | movie_top250, movie_aka, movie_release_date, movie_award, movie_related, movie_comment, movie_rating_dist, movie_genre_rank |
| 用户行为域 | 4 | ratings, favorites, view_history, search_history |
| 推荐特征域 | 8 | movie_features, user_features, user_genre_pref, user_person_pref, movie_similarity, movie_co_occurrence, user_reco_cache, llm_reco_log |
| 系统日志域 | 3 | crawl_logs, recommendation_logs, import_logs |
| **合计** | **38** | |

---

## 4. 重建步骤回顾

```bash
cd C:\Users\xunra\Desktop\moveme

# 1. 停容器并删数据卷（破坏性）
docker compose -f docker/docker-compose.yml down -v

# 2. 重写 SQL（已完成）
# docker/mysql/init/01-schema.sql
# docker/mysql/init/02-seed-data.sql

# 3. 启动容器，自动按新 SQL 初始化
docker compose -f docker/docker-compose.yml up -d

# 4. 等 MySQL 就绪
docker exec moveme-mysql mysqladmin ping -u root -proot123 --silent
```

---

## 5. 验证 SQL（确认 schema 就位）

```sql
-- 1) 表数 = 38
SELECT COUNT(*) FROM information_schema.tables
WHERE table_schema='moveme';

-- 2) 字典就位
SELECT COUNT(*) FROM genres;             -- 30
SELECT COUNT(*) FROM countries;          -- 25
SELECT COUNT(*) FROM languages;          -- 17
SELECT COUNT(*) FROM award_ceremonies;   -- 12

-- 3) admin 用户就位
SELECT username, role FROM users;        -- admin, role=1

-- 4) FULLTEXT 索引在
SHOW INDEX FROM movies WHERE Index_type='FULLTEXT';
SHOW INDEX FROM movie_comment WHERE Index_type='FULLTEXT';

-- 5) 关键字段类型核查
DESC movies;     -- 应有 popularity_score / quality_score / freshness_score / wish_count / collect_count / title_pinyin
DESC persons;    -- 应有 douban_person_id UNIQUE / movie_count / avg_movie_rating
DESC movie_features;          -- 应有 genre_vector JSON / cast_vector JSON
DESC movie_similarity;        -- 应有 algorithm + rank_no
DESC user_reco_cache;         -- 应有 movie_ids JSON / reasons JSON / expires_at

-- 6) 外键全建立（应有 46 个）
SELECT COUNT(*) FROM information_schema.table_constraints
WHERE table_schema='moveme' AND constraint_type='FOREIGN KEY';
```

---

## 6. 此后的下一步

| 阶段 | 文档 | 状态 |
|---|---|---|
| Phase B | [40-种子导入器](./40-种子导入器.md) | 已完成 |
| Phase C | [41-推荐特征计算](./41-推荐特征计算.md) | 已完成，默认关闭 |
| 后续 | [20-后续路线图](./20-后续路线图.md) | Phase E+ 路线图 |

---

## 7. 复盘

### 做得好的
- **一次性重建**而非多步迁移：v1 数据本来就少，重建比 ALTER TABLE 链快得多
- **字典先行**：genres/countries/languages 预置，导入时就不用现造
- **persons 独立**：消除了 v1 里"摩根·弗里曼"在多张表里字符串重复的问题

### 注意点
- DROP DATABASE 不可逆：重建前确认数据可丢
- Windows + Docker Desktop 必须先启动 Docker（不像 Linux 是 daemon）
- mysql 密码是 `root123`（来自 docker-compose `MYSQL_PASSWORD:-root123`），不是 `root`
