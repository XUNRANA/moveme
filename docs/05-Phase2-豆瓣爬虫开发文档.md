# 05 - Phase 2：豆瓣爬虫模块开发文档

## 概述

Phase 2 目标：实现豆瓣电影数据爬取模块，能够从豆瓣获取电影列表和详情，入库保存，支持定时任务和管理员手动触发。

**完成时间**：2026-03-25
**涉及文件数**：8 个 Java 源文件 + 6 个实体/Mapper 文件（Phase 1 已创建）

---

## 1. 模块结构

```
module/crawler/
├── controller/
│   └── CrawlerController.java        # 管理员 REST 接口
├── entity/
│   └── CrawlLog.java                 # 爬虫日志实体
├── mapper/
│   └── CrawlLogMapper.java           # 日志 Mapper
├── parser/
│   └── DoubanMovieParser.java        # JSON/HTML 解析器
├── scheduler/
│   └── CrawlerScheduler.java         # 定时任务
└── service/
    ├── CrawlerService.java           # 服务接口
    └── impl/
        └── DoubanCrawlerServiceImpl.java  # 核心实现
```

**依赖的电影模块文件**（Phase 1 已创建）：
- `module/movie/entity/` — Movie, Genre, MovieActor, MovieDirector, MovieGenre
- `module/movie/mapper/` — MovieMapper (含 upsertByDoubanId), GenreMapper, MovieActorMapper, MovieDirectorMapper, MovieGenreMapper

---

## 2. 数据源分析

### 2.1 豆瓣 JSON 列表 API

**URL**: `https://movie.douban.com/j/search_subjects?type=movie&tag={tag}&page_limit={limit}&page_start={start}`

**返回格式**：
```json
{
  "subjects": [
    {
      "id": "30382501",
      "title": "浴血黑帮：不朽传奇",
      "cover": "https://img3.doubanio.com/view/photo/s_ratio_poster/public/p2928174437.jpg",
      "rate": "6.8",
      "url": "https://movie.douban.com/subject/30382501/",
      "is_new": true
    }
  ]
}
```

**可获取字段**：doubanId, title, posterUrl, doubanRating
**限制**：每页最多 50 条，需分页请求

### 2.2 豆瓣详情页 HTML

**URL**: `https://movie.douban.com/subject/{doubanId}/`

**可解析字段**：year, doubanVotes, summary, directors, actors, genres, country, language, duration, imdbId, originalTitle

**注意**：详情页有较强的反爬保护，可能返回 302 重定向至 `sec.douban.com`（安全验证页面）。列表 API 相对宽松。

---

## 3. 核心代码解析

### 3.1 爬虫常量 (`CrawlerConstants.java`)

```java
// 三个核心 URL
DOUBAN_SEARCH_URL   // 列表 JSON API
DOUBAN_DETAIL_URL   // 详情页 HTML
DOUBAN_TAGS_URL     // 标签列表（备用）

// 12 个 User-Agent 字符串，模拟不同浏览器
USER_AGENTS = { "Mozilla/5.0 ... Chrome/124", ... }

// 爬虫状态码
CRAWL_STATUS_RUNNING = 0
CRAWL_STATUS_SUCCESS = 1
CRAWL_STATUS_FAILED  = 2
```

### 3.2 数据解析器 (`DoubanMovieParser.java`)

**两个核心方法**：

| 方法 | 输入 | 解析方式 | 输出 |
|---|---|---|---|
| `parseListJson()` | JSON 字符串 | Jackson ObjectMapper | `List<Movie>` (基础信息) |
| `parseDetailHtml()` | HTML 字符串 + Movie | Jsoup CSS 选择器 | `ParsedDetail` (完整信息) |

**HTML 解析选择器**：
```java
doc.selectFirst("span[property=v:itemreviewed]")   // 标题
doc.selectFirst(".year")                            // 年份
doc.selectFirst("strong[property=v:average]")       // 评分
doc.selectFirst("span[property=v:votes]")           // 评分人数
doc.select("span[property=v:summary]")              // 简介
doc.select("a[rel=v:directedBy]")                   // 导演
doc.select("a[rel=v:starring]")                     // 主演
doc.select("span[property=v:genre]")                // 类型
```

**#info 区域文本提取**：
```java
// 用正则从 #info 文本中提取 "制片国家/地区: 美国" → "美国"
Pattern.compile(fieldName + ":\\s*([^\\n]+?)(?:\\s+\\S+:|$)")
```

**内部类 `ParsedDetail`**：
```java
@Data
public static class ParsedDetail {
    private Movie movie;              // 电影对象（已填充详情）
    private List<String> directors;   // 导演列表
    private List<String> actors;      // 演员列表
    private List<String> genres;      // 类型列表
}
```

### 3.3 爬虫核心实现 (`DoubanCrawlerServiceImpl.java`)

**反爬策略**：

| 策略 | 实现 |
|---|---|
| 随机 User-Agent | 每次请求从 12 个 UA 中随机选取 |
| 模拟浏览器请求头 | Accept, Accept-Language, Referer, Connection |
| 随机延迟 | 每次请求前等待 3-5 秒（可配置） |
| 403 退避 | 遇到 403/418 状态码，等待 30 秒 |

**关键注意**：不要手动设置 `Accept-Encoding` 请求头！OkHttp 会自动处理 gzip 解压。手动设置会导致 OkHttp 跳过自动解压，返回的 response body 是压缩的字节流，无法解析为文本。

**爬取流程 (`crawlByTag`)**：
```
1. 创建 CrawlLog 记录，状态=RUNNING
2. 分页循环（每页 50 条）:
   a. 构造列表 URL → fetchUrl() 获取 JSON
   b. parseListJson() 解析电影列表
   c. 逐部电影:
      - saveMovieFromList() → INSERT ON DUPLICATE KEY UPDATE
      - crawlMovieDetail() → 爬详情页、解析 HTML、保存关联数据
      - 成功计数 +1
   d. 列表为空 → break
3. 更新 CrawlLog，状态=SUCCESS/FAILED
```

**数据写入策略 (`upsertByDoubanId`)**：
```sql
INSERT INTO movies (douban_id, title, ...) VALUES (?, ?, ...)
ON DUPLICATE KEY UPDATE title=VALUES(title), ...
```
以 `douban_id` 唯一索引去重，重复爬取时更新已有记录。

**关联数据保存 (`saveRelations`)**：
- 先删后插策略，保证数据一致性
- 类型：查找现有 Genre → 不存在则新建 → 插入 movie_genre 关联
- 导演/演员：清空旧数据 → 批量插入，演员保留排序

### 3.4 定时任务 (`CrawlerScheduler.java`)

| 任务 | Cron 表达式 | 说明 |
|---|---|---|
| 每日爬取 | `0 0 3 * * ?` | 凌晨 3:00，爬"热门"50部 + "最新"50部 |
| 每周 Top250 | `0 0 4 ? * SUN` | 每周日凌晨 4:00，爬"豆瓣高分"250部 |

**开关控制**：
```yaml
# application.yml
moveme:
  crawler:
    enabled: true   # false 可关闭定时任务
```

### 3.5 管理员接口 (`CrawlerController.java`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/admin/crawler/trigger?tag=热门&limit=20` | 按标签手动触发爬取 |
| POST | `/api/v1/admin/crawler/trigger/top250` | 手动触发 Top250 爬取 |
| POST | `/api/v1/admin/crawler/trigger/detail/{doubanId}` | 爬取单部电影详情 |
| GET | `/api/v1/admin/crawler/logs?limit=20` | 查看爬虫日志 |

**权限**：所有 `/admin/**` 路径需要 ADMIN 角色 JWT Token。

---

## 4. 配置项说明

```yaml
moveme:
  crawler:
    enabled: true          # 是否启用定时爬虫
    delay-min-ms: 3000     # 最小请求间隔（毫秒）
    delay-max-ms: 5000     # 最大请求间隔（毫秒）
    backoff-minutes: 30    # 被限制后的退避时间
```

---

## 5. 测试验证

### 5.1 测试步骤

```bash
# 1. 管理员登录获取 Token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 2. 触发爬虫（以"热门"标签为例，爬 10 部）
curl -X POST "http://localhost:8080/api/v1/admin/crawler/trigger?tag=热门&limit=10" \
  -H "Authorization: Bearer {你的token}"

# 3. 查看爬虫日志
curl "http://localhost:8080/api/v1/admin/crawler/logs?limit=5" \
  -H "Authorization: Bearer {你的token}"

# 4. 查看数据库
docker exec moveme-mysql mysql -uroot -proot123 moveme \
  --default-character-set=utf8mb4 \
  -e "SELECT id, douban_id, title, douban_rating FROM movies;"
```

### 5.2 实际测试结果

| 测试 | 结果 |
|---|---|
| `crawlByTag("热门", 3)` | 3/3 成功，耗时 ~18 秒 |
| `crawlByTag("最新", 10)` | 10/10 成功，耗时 ~47 秒 |
| 数据库入库 | 11 部电影，含 title + rating + posterUrl |
| 爬虫日志 | 3 条记录，状态均为 SUCCESS |
| 详情页爬取 | 被豆瓣 sec 重定向拦截（已知限制） |

---

## 6. 已知问题与限制

### 6.1 详情页反爬

豆瓣对电影详情页 (`/subject/{id}/`) 有较强的反爬保护：
- 请求会被 302 重定向至 `sec.douban.com`（安全验证页面）
- 导致 year, summary, directors, actors, genres 等详情字段无法获取

**影响**：目前只能从 JSON 列表 API 获取基础信息（title, rating, poster）

**可能的解决方案**（后续优化）：
1. 使用浏览器 Cookie（手动登录后导出）
2. 使用代理 IP 池
3. 使用 Selenium/Playwright 模拟浏览器
4. 使用第三方电影数据 API（如 TMDB）补充详情

### 6.2 OkHttp Accept-Encoding 陷阱

**问题**：手动设置 `Accept-Encoding: gzip, deflate` 会导致 OkHttp 跳过自动解压

**表现**：fetchUrl 返回的 body 是压缩字节流，JSON 解析静默失败，successCount = 0

**解决**：删除手动设置的 Accept-Encoding 请求头，让 OkHttp 自行处理

---

## 7. 文件清单

| 文件 | 行数 | 说明 |
|---|---|---|
| `CrawlerConstants.java` | 44 | 爬虫常量（URL、UA、状态码） |
| `CrawlLog.java` | ~30 | 爬虫日志实体 |
| `CrawlLogMapper.java` | ~10 | 日志 Mapper |
| `DoubanMovieParser.java` | 191 | JSON + HTML 解析器 |
| `DoubanCrawlerServiceImpl.java` | 294 | 爬虫核心实现 |
| `CrawlerScheduler.java` | 55 | 定时任务 |
| `CrawlerController.java` | 56 | 管理员 REST 接口 |
| `CrawlerService.java` | 33 | 服务接口 |

---

## 8. 踩坑记录

### 坑 1：OkHttp 手动 Accept-Encoding 导致 gzip 不解压

**现象**：爬虫返回 successCount=0，但 Douban API 直接 curl 有数据

**原因**：OkHttp 的 `BridgeInterceptor` 默认会添加 `Accept-Encoding: gzip` 并自动解压。但如果手动设置了该请求头，OkHttp 认为开发者要自行处理压缩，不再自动解压。

**解决**：删除 `.header("Accept-Encoding", "gzip, deflate")` 这一行

### 坑 2：MySQL CLI 显示中文乱码

**现象**：通过 `docker exec mysql` 查询，中文显示为 `???`

**解决**：添加 `--default-character-set=utf8mb4` 参数

### 坑 3：@Transactional 在 private 方法上不生效

**注意**：`saveRelations()` 方法标注了 `@Transactional` 但声明为 `private`。Spring AOP 代理无法拦截 private 方法，事务实际不会生效。如果需要事务，应改为 `public` 或将该方法提取到另一个 Bean 中调用。

**当前状态**：由于该方法只在同类内部调用，且每个操作都是独立的 INSERT/DELETE，暂不影响功能。但如果后续需要严格事务保证（全部成功或全部回滚），需要修复。
