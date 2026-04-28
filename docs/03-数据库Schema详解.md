# 03 - 数据库 Schema 详解（v2，38 张表）

> 这是 v2 schema 的逐表说明。所有 DDL 在 `docker/mysql/init/01-schema.sql`，本文档讲**每张表为什么这样设计、字段语义、索引取舍**。

---

## 0. 总览

```
8 个域 × 共 38 张表

域 1：用户域（1）          users
域 2：内容字典域（5）      genres / countries / languages / tags / award_ceremonies
域 3：内容主域（2）        movies / persons
域 4：关联表域（7）        movie_{genre,country,language,tag,director,writer,actor}
域 5：富字段域（8）        movie_top250 / aka / release_date / award / related / comment / rating_dist / genre_rank
域 6：用户行为域（4）      ratings / favorites / view_history / search_history
域 7：推荐特征域（8）      movie_features / user_features / user_genre_pref / user_person_pref /
                          movie_similarity / movie_co_occurrence / user_reco_cache / llm_reco_log
域 8：系统日志域（3）      crawl_logs / recommendation_logs / import_logs
```

---

## 1. 用户域

### 1.1 `users`
| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| username | VARCHAR(50) UNIQUE | 登录名 |
| password | VARCHAR(255) | BCrypt cost=12 |
| email | VARCHAR(100) UNIQUE | 可空 |
| nickname | VARCHAR(50) | 显示名 |
| avatar_url | VARCHAR(500) | |
| bio | VARCHAR(500) | 个人简介 |
| role | TINYINT | 0=普通,1=管理员 |
| status | TINYINT | 0=禁用,1=启用 |
| created_at / updated_at | DATETIME | |

**索引**：`idx_username`, `idx_email`

**为什么把 admin 也放这表（不分 admin_users）**：单表 + role 区分够用，权限模型简单。

---

## 2. 内容字典域

### 2.1 `genres` 类型
- 30 条预置：剧情/喜剧/动作/...
- 字段：id, name UNIQUE, description

### 2.2 `countries` 国家
- 25 条预置
- 字段：id, name UNIQUE, code (ISO 可选)

### 2.3 `languages` 语言
- 17 条预置：汉语普通话/英语/粤语/...
- 字段：id, name UNIQUE

### 2.4 `tags` 标签
- 初始空，导入/爬取时增量
- 字段：id, name UNIQUE, **usage_count INT**（被多少电影打过，给推荐排序用）

### 2.5 `award_ceremonies` 颁奖典礼
- 12 条预置：奥斯卡/金球/戛纳/金马/金像/...
- 字段：id, name UNIQUE, organization, description

**字典域的设计原则**：
- name UNIQUE，name 拿来当业务键（不是 id）。
- 所有引用方走外键 + ON DELETE CASCADE/SET NULL 看场景。
- 字典扩展：导入时 `ensureDict()` 懒插入，避免预置不全卡住业务。

---

## 3. 内容主域

### 3.1 `movies` 电影主表
**核心字段（30+ 个，分组解释）**：

| 分组 | 字段 |
|---|---|
| **标识** | id, douban_id UNIQUE, imdb_id |
| **标题** | title (主), title_cn, title_en, title_pinyin（搜索用） |
| **简介** | summary (TEXT), summary_short (VARCHAR 500，列表页用) |
| **时间** | year, duration_minutes, duration_text, release_date |
| **海报** | poster_url, poster_local_path, backdrop_url, trailer_url, official_site |
| **评分** | douban_rating, douban_votes, local_rating, local_votes |
| **热度** | wish_count（豆瓣想看）, collect_count（豆瓣看过） |
| **推荐** | popularity_score, quality_score, freshness_score |
| **维护** | detail_fetched_at, last_crawled_at, status, created_at, updated_at |

**关键索引**：
- `idx_douban_id`, `idx_year`, `idx_douban_rating`, `idx_popularity`, `idx_release_date`
- **FULLTEXT(title, title_cn, title_en, summary) WITH PARSER ngram**：中文模糊搜索

**设计要点**：
- **主标题 vs 中/英标题分列**：豆瓣 `title` 是 "肖申克的救赎 The Shawshank Redemption"，需要拆开方便单独显示。
- **`title_pinyin`**：拼音搜索"xsk" → 肖申克。可用 pinyin4j 库生成。
- **summary_short 冗余 200 字摘要**：列表页不用每次截断 TEXT，省 IO。
- **三个推荐 score**：离线 job 算，业务查询直接读列。**为什么不放 movie_features？** 浏览页/榜单页天天用，主表拿走避免 JOIN。
- **`detail_fetched_at` vs `last_crawled_at`**：前者标记"完整详情抓过"，后者标记"列表里见过"。区分这两个让"补抓详情"job 能精确找出 detail 缺的电影。

### 3.2 `persons` 人物表
| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| douban_person_id | VARCHAR(20) UNIQUE | 豆瓣人物 id，业务键 |
| name | VARCHAR(100) | 中文名 |
| name_en | VARCHAR(150) | 英文/原名 |
| avatar_url, avatar_local_path | VARCHAR(500) | 头像 |
| profile_url | VARCHAR(500) | douban.com/personage/xxx |
| gender | TINYINT | 0=未知,1=男,2=女 |
| birth_date, birth_place, bio | | |
| **movie_count** | INT | 参与电影数（聚合，离线算） |
| **avg_movie_rating** | DECIMAL(3,1) | 平均豆瓣分（聚合） |

**为什么独立表**（v1 是直接 name 拼接进关联表）：
1. top250 里同一个人物（摩根·弗里曼）出现多次，每次都带头像/profile，独立表能去重避免数据膨胀。
2. 将来做"演员页"/"导演作品集"必须有独立 person id。
3. `user_person_pref` 推荐特征需要 person_id，没有独立表无从下手。

---

## 4. 关联表域（7 张）

所有关联表都是**多对多 + 复合主键**模式，外键 ON DELETE CASCADE。

| 表 | 复合主键 | 额外字段 |
|---|---|---|
| `movie_genre` | (movie_id, genre_id) | — |
| `movie_country` | (movie_id, country_id) | — |
| `movie_language` | (movie_id, language_id) | — |
| `movie_tag` | (movie_id, tag_id) | — |
| `movie_director` | (movie_id, person_id) | sort_order |
| `movie_writer` | (movie_id, person_id) | sort_order |
| `movie_actor` | (movie_id, person_id) | role_name, sort_order, **is_lead** |

**`movie_actor.role_name`**：'饰 安迪·杜佛兰 Andy Dufresne' 这种字符串，存原文。
**`movie_actor.is_lead`**：sort_order ≤ 5 视为主演，冗余字段加速"查这部电影主演"。

**索引取舍**：除主键外都加 `idx_{partner}_movie`（反向查询），如 `movie_actor.idx_person_movie`，让"摩根·弗里曼演过哪些电影"走索引。

---

## 5. 富字段域（8 张）

这是 v2 vs v1 最大的差异，全是为"详情页信息密度 + 推荐解释"准备的。

### 5.1 `movie_top250`
| 字段 | |
|---|---|
| movie_id | PK + FK |
| **rank_no** | SMALLINT UNIQUE（1-250） |
| list_title | VARCHAR(100) |
| **quote** | VARCHAR(500)（"希望让人自由"） |
| snapshot_at | DATETIME，重抓时更新 |

为什么单独建表（而非 `movies.top250_rank` 列）：将来榜单变了（比如还加豆瓣 250 华语榜），多榜单很自然扩展成 `movie_list_membership` 模式。

### 5.2 `movie_aka` 又名
- `id, movie_id, title`
- 一部电影多个又名，每个一行。

### 5.3 `movie_release_date` 上映日期（多地多日）
- `id, movie_id, release_at DATE, region VARCHAR(80), raw_text`
- 例：1994-09-10/多伦多电影节、1994-10-14/美国。

### 5.4 `movie_award` 奖项
| 字段 | |
|---|---|
| ceremony_id | FK award_ceremonies (可空) |
| ceremony_text | "第67届奥斯卡金像奖"（原文） |
| category | "最佳影片"/"最佳男主角" |
| status | ENUM('won','nominated','unknown') |
| recipient_person_id | FK persons (可空) |
| recipient_text | 原始 recipients 文本 |

为什么 `ceremony_text` 和 `ceremony_id` 都存：text 是源真相，id 是为了做"奥斯卡获奖电影"的快速查询。

### 5.5 `movie_related` 相关电影
- 豆瓣"喜欢这部的人也喜欢"那条数据。
- `related_movie_id` 在初次导入时可能 NULL（被推荐方还没插入），导入完所有电影后跑回填 SQL 把它填上。

### 5.6 `movie_comment` 短评（**最大的富字段表**）
| 字段 | |
|---|---|
| **source** | TINYINT 0=豆瓣抓 1=站内用户 |
| douban_comment_id | VARCHAR(40) UNIQUE（防重导）|
| user_id | FK users (可空，仅 source=1 时有值) |
| author_name, author_avatar, author_location | 豆瓣源时直接存 |
| rating, rating_label | 1-5 + "力荐"等 |
| content | TEXT |
| votes | 有用数 |
| posted_at | |
| source_url | |

**索引**：
- `idx_movie_votes (movie_id, votes DESC)` —— 按热度查电影热门评论
- `FULLTEXT ft_content WITH PARSER ngram` —— 短评全文搜索（"演技""特效"）

**为什么 source 字段而非两张表**：80% 的查询是"这部电影所有短评"，不分源；放一起 + idx_movie_votes 一次扫描搞定。

### 5.7 `movie_rating_dist` 5 星分布
- 一部电影 5 行（star=1..5）
- 字段：movie_id, star, label, percentage
- 用途：详情页"力荐 85.6% / 推荐 12.1% / ..."

### 5.8 `movie_genre_rank` 类型百分位
- "好于 99% 剧情片" / "好于 95% 犯罪片"
- 字段：movie_id, genre_name, percentile, rank_url
- 用途：推荐解释（"这部为什么值得看"）

---

## 6. 用户行为域

### 6.1 `ratings` 评分
- `(user_id, movie_id) UNIQUE` —— 一人一部一评
- score TINYINT 1-10
- comment TEXT（评分时附带短评，将来可以同步写入 `movie_comment` source=1）

### 6.2 `favorites` 收藏
- `(user_id, movie_id) UNIQUE`
- **status**: 0=想看, 1=已看
- 单表两态，避免 wishlists 和 watched 两张表。

### 6.3 `view_history` 浏览历史
- 用户每次进详情页就插一行（不去重，按时间堆积）
- 索引：`(user_id, viewed_at DESC)` 给"我的最近浏览"用
- 推荐冷启动时这是重要弱信号

### 6.4 `search_history` 搜索历史
- 字段：user_id (可空，匿名也记), keyword, result_cnt
- 用途：搜索词云、热搜词、个人搜索回放

---

## 7. 推荐特征域（8 张，v2 重点）

### 7.1 `movie_features` 电影特征向量
| 字段 | 说明 |
|---|---|
| movie_id | PK |
| genre_vector | JSON {"剧情":1,"犯罪":1} |
| tag_vector | JSON TF-IDF |
| cast_vector | JSON {"person_id":weight,...} |
| decade | TINYINT (1990s=199) |
| cluster_id | INT (KMeans 簇号) |
| embedding | JSON 32 维 dense（可选） |
| feature_version | VARCHAR(20) |
| updated_at | |

为什么 JSON 列：MySQL 8 支持原生 JSON + 函数；250 部规模下读写无压力；变长向量比固定列优雅。

### 7.2 `user_features` 用户偏好
| 字段 | 说明 |
|---|---|
| user_id | PK |
| genre_vector / tag_vector / cast_vector | 加权聚合 |
| avg_year | 用户偏好年代 |
| avg_rating_given | 用户打分均值（用于 z-score 归一化） |
| rating_count / favorite_count / view_count | |
| feature_version, updated_at | |

行为后异步刷新（@Async）。

### 7.3 `user_genre_pref` 用户对类型的偏好分
- 复合主键 (user_id, genre_id), score
- 索引 `(user_id, score DESC)` 给"用户最爱类型 top 3"用
- **为什么单独存而不只放 user_features.genre_vector**：JSON 列查询不能命中索引，做"按类型偏好排序的用户列表"难；扁平化关联表查得快。

### 7.4 `user_person_pref` 用户对人物的偏好
- 复合主键 (user_id, person_id, role_kind ENUM)
- role_kind: director/actor/writer
- 用途："用户最爱导演前 5"

### 7.5 `movie_similarity` 电影相似度（多算法）
| 字段 | |
|---|---|
| movie_id_a, movie_id_b, algorithm | 复合主键 |
| algorithm | VARCHAR(20) CONTENT/CF_ITEM/EMBEDDING |
| score | DECIMAL(6,4) |
| **rank_no** | SMALLINT，a 在该算法下的相似排名 |
| updated_at | |

索引：`(movie_id_a, algorithm, rank_no)` —— 直接 top-K 检索。

为什么 algorithm 行存而非列存（多算法各开一列）：
- 新增算法不用改表。
- 每对 (a,b) 在不同算法下分数差异大，同时取多算法时多行 OR 即可。

### 7.6 `movie_co_occurrence` 共现矩阵
- 主键 (movie_id_a, movie_id_b)
- co_count（共同被多少用户喜欢/看过）
- pmi（点互信息 log(p(a,b)/p(a)p(b))，比纯 count 更鲁棒）
- 索引：`(movie_id_a, co_count DESC)`

### 7.7 `user_reco_cache` 推荐缓存
| 字段 | |
|---|---|
| id | PK |
| user_id | |
| strategy | CONTENT/CF/LLM/HYBRID/COLD_START |
| **movie_ids** | JSON 推荐 id 列表 |
| **reasons** | JSON 每部为什么推（"因为你看过 X"） |
| score_map | JSON {movie_id: score} |
| expires_at | DATETIME (24h 后过期) |

**为什么落库而非只放 Redis**：方便审查/回放/A-B 测试。Redis 失效后能从这里冷启重建。

### 7.8 `llm_reco_log` LLM 调用日志
- 字段：provider, model, prompt, raw_response, parsed_movies, input_tokens, output_tokens, cost_usd, latency_ms
- 用途：成本监控、效果回归测试。

---

## 8. 系统日志域

### 8.1 `crawl_logs`
- 每次爬虫批次记录一行：task_type, status, total/success/fail counts, error_message, started_at, finished_at

### 8.2 `recommendation_logs`
- 每次给用户出推荐记录一行：strategy_type, request_data, response_data, llm_provider, latency_ms

### 8.3 `import_logs`
- 种子导入批次：source, file_path, movies_total/ok/fail, persons_ok, comments_ok, errors JSON

---

## 9. ER 关系一图（核心）

```
                       users ─┬───< ratings >───────┐
                              ├───< favorites >─────┤
                              ├───< view_history >──┤
                              ├───< user_features  >┤
                              ├───< user_*_pref >───┤
                              └───< user_reco_cache>┤
                                                    │
persons ─┬─< movie_director >─┐                     │
         ├─< movie_writer   >─┼──> movies <─────────┘
         └─< movie_actor    >─┤    │
                              │    ├─< movie_top250 (1:1)
                              │    ├─< movie_aka (1:N)
                              │    ├─< movie_release_date >
                              │    ├─< movie_award >─────> award_ceremonies
                              │    ├─< movie_related (self-ref) >
                              │    ├─< movie_comment >
                              │    ├─< movie_rating_dist >
                              │    └─< movie_genre_rank >
                              │
                              ├─< movie_features (1:1) >
                              └─< movie_similarity / movie_co_occurrence (self-N) >

genres / countries / languages / tags ── 通过关联表 movie_* 与 movies 多对多
```

---

## 10. 索引/性能注意事项

- **`movies` 主表**：年份/评分/popularity 查询频繁，三个 b-tree 索引足够。FULLTEXT 单独走 MATCH 语法，不要和 b-tree 混用。
- **关联表反向查询**：所有 `movie_xxx` 都加 `(xxx_id, movie_id)` 二级索引，让"演员页/类型页"高效。
- **复合索引顺序**：永远是"等值条件列在前，范围/排序列在后"。例 `(user_id, viewed_at DESC)`：user_id 等值先匹配，再按时间倒序扫。
- **JSON 列**：不要在 JSON 列上 WHERE 等值，效率低。需要快速索引就把字段冗余成扁平表（user_genre_pref 就是这个理由）。

---

## 11. v1 → v2 改动清单

| 维度 | v1 | v2 |
|---|---|---|
| 表数 | 10 | 38 |
| 人物 | 字符串散落关联表 | 独立 persons 表 |
| 国家/语言 | movies 单值列 | 关联表多值 |
| 短评 | 无 | movie_comment（豆瓣 + 站内统一表） |
| 奖项 | 无 | movie_award + award_ceremonies |
| 相关电影 | 无 | movie_related + 回填关联 |
| 5 星分布 | 无 | movie_rating_dist |
| 类型百分位 | 无 | movie_genre_rank |
| 推荐特征 | 无 | 8 张表 |
| 浏览/搜索历史 | 无 | view_history / search_history |
| 又名 | original_title 单字段 | movie_aka 多值 |
| 上映日期 | release_date 单字段 | movie_release_date 多地多日 |
| Top250 | 无标记 | movie_top250 单独表（含 quote） |
| 系统日志 | 1 张 | 3 张（crawl/reco/import） |

---

## 12. 后续可能的扩展

- 多榜单：`movie_list / movie_list_membership` 把 top250 推广成多个榜单
- 用户社交：`user_follow / user_collection / user_review_like`
- 标签层级：`tag_parent_id` 父子标签
- 多语言简介：`movie_summary_i18n(movie_id, lang, summary)`
- 全文搜索 ES：MySQL FULLTEXT 顶不住时切 Elasticsearch

这些都是 v2 schema 的自然扩展，不需要重建。
