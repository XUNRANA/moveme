# 30 - REST API 参考

> 后端 controller 总览的可读版。Knife4j 自动生成的接口文档在 `http://localhost:8080/doc.html`，本文档侧重"权限分组 + 字段示例 + 设计意图"。
>
> 所有接口前缀：`/api/v1`。

---

## 0. 通用约定

### 0.1 响应包络 `Result<T>`

所有接口都返回统一信封：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": <T 或 null>
}
```

| code | 含义 |
|---|---|
| 200 | 成功 |
| 400 | 参数校验失败 / 业务异常 |
| 401 | 未登录 / token 失效 |
| 403 | 已登录但权限不足（非管理员访问 `/admin/**`） |
| 404 | 资源不存在（如 person id 不存在） |
| 500 | 后端未预期异常 |

异常由 `GlobalExceptionHandler` 统一拦截，前端不需要 try/catch 每个调用，只读 `code` 判分。

### 0.2 分页 `IPage<T>`

MyBatis-Plus 分页结构，前端类型对应 `PageResponse<T>`：

```json
{
  "records": [...],
  "total": 250,
  "size": 12,
  "current": 1,
  "pages": 21
}
```

约定：`page` 从 1 起；`size` 默认依接口不同，常见 10 / 12 / 20。

### 0.3 鉴权

需登录的接口请求头加：

```
Authorization: Bearer <accessToken>
```

`JwtAuthenticationFilter` 解析后把 `userId` 注入 `Authentication.principal`，Service 层用 `(Long) authentication.getPrincipal()` 拿到。

### 0.4 权限分组（来自 `SecurityConfig`）

| 路径 | 策略 |
|---|---|
| `/api/v1/auth/**` | 公开 |
| `GET /api/v1/movies/**` | 公开 |
| `GET /api/v1/persons/**` | 公开 |
| `/static/**` (GET/HEAD) | 公开 |
| `/doc.html` / `/v3/api-docs/**` / `/swagger-ui/**` | 公开 |
| `/api/v1/admin/**` | 需 `ROLE_ADMIN` |
| 其余 | 需登录 |

---

## 1. 认证模块（UserController · `/api/v1`）

### POST `/auth/register`
注册新用户。

请求体：
```json
{ "username": "alice", "password": "Passw0rd!", "email": "a@x.com", "nickname": "Alice" }
```
`email` / `nickname` 可空。`password` BCrypt cost=12 加密入库。

响应：`Result<Void>` `code=200` 表示注册成功。

### POST `/auth/login`
请求体：`{ "username": "...", "password": "..." }`

响应：
```json
{
  "code": 200,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 7200
  }
}
```
- access 2h、refresh 7d
- refreshToken 同时存 Redis（key `auth:refresh_token:{userId}`），便于服务端主动 revoke

### POST `/auth/refresh`
请求体：`{ "refreshToken": "..." }`

响应：新的 access + refresh 对。

---

## 2. 用户模块（UserController · `/api/v1/users/me/*`）

> 全部需登录。`userId` 自动从 token 取，前端不需要传。

### GET `/users/me`
返回当前用户档案 `UserVO`：
```json
{
  "id": 7, "username": "alice", "email": "a@x.com",
  "nickname": "Alice", "avatarUrl": "/static/avatars/users/7.jpg",
  "bio": "...", "role": 0, "createdAt": "2026-03-12 10:20:00"
}
```
`role`: 0=普通，1=管理员。

### PUT `/users/me`
请求体：`{ "nickname": "...", "email": "..." }` — 仅这两个字段。

### POST `/users/me/avatar`
`multipart/form-data`，字段名 `file`。
响应：`Result<String>`，data 是头像 URL（如 `/static/avatars/users/7.jpg`）。
文件大小上限：10MB（由 `spring.servlet.multipart.max-file-size` 控制）。

### GET `/users/me/stats`
返回 `UserStatsVO`：
```json
{ "ratingCount": 12, "wishCount": 5, "watchedCount": 18, "historyCount": 34 }
```

### GET `/users/me/ratings?page=1&size=10`
分页返回当前用户所有评分（含电影标题、海报、分数、评论、时间）。

### GET `/users/me/favorites?status=&page=1&size=10`
`status` 可空，0=想看、1=已看。分页返回收藏列表。

### GET `/users/me/history?page=1&size=10`
浏览历史（按时间倒序）。

### GET `/users/me/taste`
口味档案 `UserTasteVO`：
```json
{
  "genrePrefs": [{ "genreId": 3, "genreName": "剧情", "score": 0.82 }, ...],
  "personPrefs": [{ "personId": 12, "personName": "诺兰", "roleKind": "director", "score": 0.91 }, ...],
  "avgRatingGiven": 7.4,
  "ratingCount": 12
}
```

### 收藏 CRUD

| Method | Path | Body / Query | 用途 |
|---|---|---|---|
| GET | `/users/me/favorites/check?movieId=` | query | 查指定电影的收藏状态，未收藏返回 `data: null` |
| POST | `/users/me/favorites` | `{ movieId, status }` | 新增收藏（status 0/1） |
| DELETE | `/users/me/favorites?movieId=` | query | 取消收藏 |

### 评分 CRUD

| Method | Path | Body / Query | 用途 |
|---|---|---|---|
| GET | `/users/me/ratings/check?movieId=` | query | 查指定电影的评分状态 |
| POST | `/users/me/ratings` | `{ movieId, score, comment? }` | 新增或更新（score 1-10） |
| DELETE | `/users/me/ratings?movieId=` | query | 删除评分 |

### POST `/users/me/history`
`{ movieId }`，前端进入电影详情页时调用以记录浏览。不去重，按时间堆积。

---

## 3. 电影模块（MovieController · `/api/v1/movies`）

> GET 方法全部公开。

### GET `/movies` 列表
入参（`MovieQueryDTO`）：`year` / `genre` / `country` / `sort` / `page` / `size`，分页返回 `MovieVO`。

### GET `/movies/search?q=&page=1&size=12`
- `q` 必填，最长 100
- `page ≥ 1`，`size ∈ [1, 50]`
- 走 MySQL FULLTEXT + ngram（命中 title / title_cn / title_en / summary）
- 副作用：把 `(userId?, keyword, resultCount)` 写入 `search_history`

### GET `/movies/genres` / `/movies/chart-genres`
- `/genres` 全部电影类型字符串列表
- `/chart-genres` 有"分类排行榜"数据的子集

### GET `/movies/chart-genres/{genre}`
返回 `MovieChartVO`：
```json
{ "genreName": "剧情", "boardTitle": "...", "movies": [{ "movieId":1, "title":"...", "posterUrl":"...", "rating":9.7, "year":1994, "rankNo":1 }, ...] }
```

### GET `/movies/annual-years` / `/movies/annual/{year}`
- 年份列表 / 单年榜单（数组，每项一个 chart）

### GET `/movies/boards` / `/movies/boards/{boardName}`
- 榜单元数据列表（包含 `boardName / displayName / genreName / boardTitle`）
- 指定榜单的电影列表

### GET `/movies/top250`
返回 Top250 完整榜（一个 `MovieChartVO`，含 250 部排名信息）。

### GET `/movies/discover`
首页"发现"板块：返回多个 `DiscoverSectionVO`（key/title/movies），供前端横滑展示。

### GET `/movies/{id}` 详情
返回 `MovieDetailVO`，字段非常厚：

| 字段组 | 字段 |
|---|---|
| 标识 | id, doubanId, imdbId |
| 标题/简介 | title, titleCn, titleEn, summary, summaryShort, year, durationText |
| 评分 | doubanRating, doubanVotes, localRating, localVotes, wishCount, collectCount, popularityScore |
| 海报 | posterUrl, posterLocalPath（优先本地） |
| 关联 | directors[], writers[], actors[]（皆 PersonBrief） |
| 富字段 | akas[], tags[], releaseDates[], awards[], relatedMovies[], ratingDist[], genreRanks[], top250, playLinks[] |

### GET `/movies/{id}/comments?page=1&size=10&sort=hot`
分页评论列表。`sort=hot` 按 `votes DESC`，`sort=new` 按时间倒序。
返回 `MovieDetailVO.CommentVO`：
```json
{
  "id": 12, "authorName": "...", "authorAvatar": "...", "authorLocation": "...",
  "rating": 5, "ratingLabel": "力荐", "content": "...",
  "votes": 120, "postedAt": "2024-09-12 10:23:00", "sourceUrl": "...",
  "liked": true
}
```
`liked` 仅在带 token 时有意义（当前用户是否点过赞）。

### POST `/movies/{id}/comments`
需登录。Body：`{ "content": "...", "rating": 5 }` （rating 可空）
写入 `movie_comment(source=1, user_id=<当前>)`。

### POST `/movies/comments/{commentId}/like` / DELETE 同路径
点赞/取消。后端事务内同步 `movie_comment.votes ±1` 与 `comment_vote` 明细。

---

## 4. 人物模块（PersonController · `/api/v1/persons`）

### GET `/persons/{id}`
返回 `PersonDetailVO`：
```json
{
  "id": 42, "name": "克里斯托弗·诺兰", "nameEn": "Christopher Nolan",
  "avatarUrl": "...", "avatarLocalPath": "/static/avatars/persons/42.jpg",
  "gender": "男", "birthDate": "1970-07-30", "birthPlace": "伦敦",
  "bio": "...",
  "movieCount": 12, "avgMovieRating": 8.8,
  "directed": [{"movieId":1, "title":"...", "posterUrl":"...", "rating":8.8, "year":2010, "roleName":""}, ...],
  "written": [...],
  "acted": [...]
}
```

人物不存在时返回 `Result.error(404, "人物不存在")`。

---

## 5. 管理后台（AdminController · `/api/v1/admin`）

> 全部需 `ROLE_ADMIN`。

### GET `/admin/stats`
返回 `AdminStatsVO`，13+ 项汇总指标（movieCount / userCount / todayNewUsers / ratingCount / favoriteCount / viewHistoryCount / searchHistoryCount / personCount / genreCount / crawlLogCount / importLogCount / movieCommentCount / lastCrawlStatus / lastCrawlTime / recoLogCount）。

### GET `/admin/users?keyword=&role=&status=&page=1&size=10`
分页用户列表 `AdminUserVO`。所有过滤参数皆可空。

### PUT `/admin/users/{id}/status`
Body：`{ "status": 0 }`（0 禁用 / 1 启用）

### PUT `/admin/users/{id}/role`
Body：`{ "role": 1 }`（0 普通 / 1 管理员）

### GET `/admin/crawl-logs?page=1&size=10`
分页 `CrawlLog`：`{ id, taskType, status, totalCount, successCount, failCount, errorMessage, startedAt, finishedAt }`。

### GET `/admin/import-logs?page=1&size=10`
分页 `ImportLog`：`{ id, source, filePath, moviesTotal, moviesOk, moviesFail, personsOk, commentsOk, errors, startedAt, finishedAt }`。

### GET `/admin/reco-logs?page=1&size=10`
分页 `RecommendationLog`：`{ id, userId, strategyType, llmProvider, latencyMs, createdAt }`。

---

## 6. 爬虫管理（CrawlerController · `/api/v1/admin/crawler`）

> 全部需 `ROLE_ADMIN`。

### POST `/admin/crawler/trigger`
Body（`CrawlTriggerReq`）：
```json
{ "taskType": "CHART", "params": { "genre": "剧情" } }
```
`taskType` 取值：`CHART` / `COMMENTS` / `ANNUAL` / `ENRICH` / `SINGLE`。

响应：`Result<Long>`，data 是写入的 `crawl_logs.id`，便于前端 poll 状态。

### GET `/admin/crawler/status`
返回 `CrawlerStatusVO`，包含"是否有任务正在跑、最近一次任务结果摘要"。

### POST `/admin/crawler/movie/{subjectId}`
快捷入口：爬指定豆瓣 subject ID 的单部电影。等价于 `trigger` 带 `taskType=SINGLE`。

---

## 7. 静态资源

由 `WebMvcConfig` 映射本地目录：

| URL 前缀 | 本地路径 | 用途 |
|---|---|---|
| `/static/posters/**` | `./data/posters` | 电影海报 |
| `/static/avatars/users/**` | `./data/avatars/users` | 用户头像 |
| `/static/avatars/persons/**` | `./data/avatars/persons` | 人物头像 |

缓存 7 天（`setCachePeriod(60*60*24*7)`）。

---

## 8. AI 推荐（RecommendController · `/api/v1/recommend`）

> 全部需登录。基于小米 MiMO 大模型，详见 [34-AI推荐功能说明](./34-AI推荐功能说明.md)。

### POST `/recommend/chat`
聊天推荐（非流式）。

请求体：
```json
{
  "messages": [
    { "role": "user", "content": "推荐几部高分悬疑片" }
  ]
}
```
`messages` 支持多轮对话，传入完整消息历史。

响应：`Result<String>`，data 是 AI 回复的 markdown 文本。回复中如果提及数据库中的电影，会自动附上详情页链接 `[片名](/movies/{id})`。

### POST `/recommend/chat/stream`
聊天推荐（SSE 流式）。请求体同上。

响应：`text/event-stream`，每个 token 作为一个 SSE event 推送，结束时发送 `event: done`。

### POST `/recommend/quick`
一键推荐。无需请求体，系统自动构建用户画像 + 片单 prompt。

响应：`Result<String>`，data 是 AI 推荐的 markdown 文本（一部电影的详细介绍 + 详情页链接）。

---

## 9. 常见错误对照

| 现象 | 可能原因 | 解决 |
|---|---|---|
| 401 | token 缺失/过期 | 重新登录或调 `/auth/refresh` |
| 403 访问 `/admin/**` | 当前用户非管理员 | 见 `docs/02 第 2.7 节` 升级用户角色 |
| 400 q must not be blank | `/movies/search?q=` 空 | 前端确保 q 必填 |
| 404 人物不存在 | `personId` 没在 persons 表 | 用 `getPersonDetail` 之前先校验 |
| 列表海报 403（浏览器 console） | 用了豆瓣远程 URL，命中防盗链 | 前端用 `resolvePoster()` 优先 `posterLocalPath` |

---

## 10. 与 Knife4j 的关系

Knife4j（`/doc.html`）是接口的"机器视角"，参数/字段全部自动生成。本文档是"人类视角"，强调权限分组、典型用法与设计意图。
- 调试用 Knife4j 直接 Try it out
- 阅读/对齐设计用本文档
- 两者不一致时以源代码为准（Controller 的 `@RequestMapping` 注解）
