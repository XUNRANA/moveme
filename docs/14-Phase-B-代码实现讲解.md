# 14 - Phase B：代码实现讲解（逐方法 walkthrough）

> 配合 [13 - 业务逻辑实现细节](./13-Phase-B-业务逻辑实现细节.md)。
>
> Phase B 现在已经完整实现 —— 17 个 stub 全部填好。本文按方法顺序讲**为什么这么写**：
> 设计取舍、数据陷阱、性能考虑、可调优点。
>
> 读完这份文档你不仅会用，还能根据将来需求魔改它。

源代码：`moveme-backend/src/main/java/com/moveme/module/seed/service/impl/Top250SeedImportServiceImpl.java`

---

## 0. 总体架构

```
SeedAutoRunner (启动时)
    └─> SeedImportService.importAll()
            ├─ loadJson() → List<SeedMovieDTO>
            ├─ for each movie:
            │     transactionTemplate.execute(() -> importOne(dto, ctx))
            │       └─ 8 个步骤：textRepair → upsertAllPersons → upsertMovie
            │                  → ensureXxx → replaceXxx → upsertTop250 → poster → markFetched
            ├─ backfillRelatedMovieIds()   // 全量后回填
            └─ aggregatePersonStats()       // 全量后聚合
```

关键设计：**每部一个独立事务（REQUIRES_NEW）**。一部失败回滚那一部，不影响其他部。失败明细写 `import_logs.errors`。

---

## 1. `importAll()` —— 流程编排

```java
@Override
public ImportResult importAll() {
    long t0 = System.currentTimeMillis();
    ImportLog logRow = startLog("TOP250_JSON", jsonResource.getDescription());
    List<SeedMovieDTO> movies = loadJson();
    ImportContext ctx = new ImportContext();

    for (SeedMovieDTO dto : movies) {
        try {
            transactionTemplate.execute(status -> {
                importOne(dto, ctx);
                return null;
            });
            ok++;
        } catch (Exception e) {
            fail++;
            errors.add(...);
        }
    }
    movieRelatedMapper.backfillRelatedMovieIds();
    aggregatePersonStats();
    finishLog(...);
    return new ImportResult(...);
}
```

### 设计要点

**TransactionTemplate vs `@Transactional`**：用 `TransactionTemplate.execute` 而不是给 `importOne` 加 `@Transactional`。原因：在同类内部调用的 `@Transactional` 不走 AOP 代理 → 注解失效。`TransactionTemplate` 显式编程式事务，没这个坑。

**ImportContext 跨电影共享**：`personCache` 让"摩根·弗里曼"在 4 部电影里出现也只查/插一次 DB。计数器 `personsUpserted/commentsImported` 给 import_logs 用。

**全量后两步收尾**：
- `backfillRelatedMovieIds`：第一遍每部把 related 写进去时，被推荐的电影可能还没插入；最后用一条 SQL JOIN movies 统一补 `related_movie_id`。
- `aggregatePersonStats`：persons 的 `movie_count / avg_movie_rating` 是聚合字段，每部插入时算不准（其他部还没进来），全量后两条 UPDATE...JOIN 一次性算完。

---

## 2. `upsertMovie(dto)` —— 主表 upsert

### 2.1 字段映射表

| Movie | 来源 | 处理 |
|---|---|---|
| `doubanId` | `dto.subjectId` | UNIQUE 业务键 |
| `imdbId` | `dto.imdb` | 可空 |
| `title` | `dto.title` | 原文 |
| `titleCn / titleEn` | `dto.title` 拆分 | `splitTitle()` 按首个空格分中英 |
| `summary` | `dto.summary` | TEXT |
| `summaryShort` | `dto.summary[:200]` | 列表页用，避免拖大 TEXT |
| `year` | `dto.year` | Integer → Short 收敛 |
| `durationMinutes / durationText` | `dto.runtimes[0]` | 正则 `(\d+)` 提分钟 |
| `releaseDate` | `dto.releaseDates` 解析最早 | `earliestReleaseDate()` |
| `posterUrl` | `dto.coverImage` | 远程 URL |
| `posterLocalPath` | — | 由 PosterFileImporter 单独写，**这里不动** |
| `doubanRating` | `dto.rating.value` | `BigDecimal.valueOf(double)` 防精度问题 |
| `doubanVotes` | `dto.rating.votes` | Long → Int 饱和转换 |
| `wishCount / collectCount` | `dto.interestCounts` | |
| `status` | 固定 `1` | 0=隐藏 1=正常 |

### 2.2 幂等 upsert 模式

```java
Movie existing = movieMapper.selectOne(
    new QueryWrapper<Movie>().eq("douban_id", ...).last("LIMIT 1"));
if (existing == null) {
    movieMapper.insert(m);
} else {
    m.setId(existing.getId());
    m.setCreatedAt(existing.getCreatedAt());        // 不覆盖创建时间
    m.setLocalRating(existing.getLocalRating());    // 站内字段保留
    m.setPopularityScore(existing.getPopularityScore());
    m.setPosterLocalPath(existing.getPosterLocalPath());
    movieMapper.updateById(m);
}
```

**为什么不用 `INSERT ... ON DUPLICATE KEY UPDATE`**：因为 `local_rating / popularity_score / poster_local_path` 这些**不该被导入覆盖**的字段，要写 COALESCE 嵌套 SQL 很难看；用 `selectOne + updateById` 在 Java 侧保留字段更清晰。

### 2.3 `splitTitle("肖申克的救赎 The Shawshank Redemption")`

```java
public static String[] splitTitle(String full) {
    int sp = full.indexOf(' ');
    if (sp <= 0) return new String[]{full, null};
    String head = full.substring(0, sp);
    String tail = full.substring(sp + 1).trim();
    return containsLatin(tail) ? new String[]{head, tail} : new String[]{full, null};
}
```

为什么要 `containsLatin` 校验？有的中文电影标题就含空格（如 `"霸王别姬 1993"`），不能瞎拆。tail 必须含拉丁字母才算英文标题。

### 2.4 `earliestReleaseDate(rawList)`

豆瓣的 `release_dates` 是字符串数组，里面格式不一：
- `"1994-09-10(多伦多电影节)"`
- `"1994-10-14(美国)"`
- `"1995"`（仅年份，少见）

我们要从中取最早一个 `LocalDate` 给 `movies.release_date`：

```java
return rawList.stream()
    .map(SeedTextUtils::parseRelease)        // 正则解析
    .map(SeedTextUtils.ReleaseParse::date)
    .filter(Objects::nonNull)
    .min(LocalDate::compareTo)
    .orElse(null);
```

`parseRelease` 正则 `^(\d{4}(?:-\d{2}-\d{2})?)(?:\((.+)\))?$`：
- group1：年份或完整日期
- group2：括号里的地区
- 解析失败 → date 为 null（不影响其他条目）

### 2.5 `toIntSaturated(Long)`

`douban_votes` 在 JSON 里是 long（豆瓣给 `Long`，可能上千万），`movies.douban_votes` 是 `INT`（最大 ~21 亿）。理论上不会溢出，但写个饱和转换防御：

```java
private Integer toIntSaturated(Long v) {
    if (v == null) return null;
    if (v > Integer.MAX_VALUE) return Integer.MAX_VALUE;
    if (v < Integer.MIN_VALUE) return Integer.MIN_VALUE;
    return v.intValue();
}
```

将来如果票数真的破 21 亿，schema 改成 BIGINT 再说。

---

## 3. `upsertAllPersons(dto, ctx)` —— 人物去重 + upsert

### 3.1 五个人物来源

| 来源字段 | 含 id | 含 avatar | 含 role |
|---|---|---|---|
| `directorDetails` | ✅ | ❌ | ❌ |
| `writerDetails` | ✅ | ❌ | ❌ |
| `actorDetails` | ✅ | ❌ | ❌ |
| `celebrityPreview` | ✅ | ✅ | ✅ |
| `awards[].recipients` | ✅ | ❌ | ❌ |

`celebrity_preview` 是富数据源（含头像 + 角色 + 中英对照名），`*_details` 是全量（演员阵容完整 25 个，但仅 id/name/url）。

### 3.2 合并策略：`addRefs()`

```java
private void addRefs(Map<String, SeedPersonRefDTO> map, List<SeedPersonRefDTO> list) {
    for (SeedPersonRefDTO r : list) {
        String k = personKey(r);
        SeedPersonRefDTO old = map.get(k);
        if (old == null) { map.put(k, r); continue; }
        if (old.getAvatar() == null && r.getAvatar() != null) old.setAvatar(r.getAvatar());
        if (old.getTitle() == null && r.getTitle() != null) old.setTitle(r.getTitle());
        if (old.getRole() == null && r.getRole() != null) old.setRole(r.getRole());
    }
}
```

**关键：**第二次见到同 id 时不覆盖，而是补全缺失字段。这样：
1. director_details 先放进去（无 avatar）
2. celebrity_preview 后来时把 avatar 补上
3. award.recipients 再来时（已有 avatar）就什么也不做

最终 map 里每个 person 字段尽可能丰富。

### 3.3 personKey —— UNIQUE 兜底

```java
private String personKey(SeedPersonRefDTO ref) {
    if (StringUtils.hasText(ref.getId())) return ref.getId();
    return "_synthetic_:" + safe(ref.getName()) + "|" + safe(ref.getTitle());
}
```

`persons.douban_person_id` 是 UNIQUE 列。极少数老条目没 id，用 `_synthetic_:name|title` 合成 key 占位（保持唯一性）。

### 3.4 ctx.personCache 加速

跨电影场景：250 部电影约 1500-2500 个不同 person，但有大量重复（一线演员频繁出镜）。
- 第 1 次见：查 DB → 不存在 → INSERT
- 第 2 次见（同次 importAll）：直接读 ctx.personCache，零 DB 调用
- 第 N 次重导（新一轮 importAll）：ctx 重置，但 selectOne 命中老记录 → UPDATE 补缺字段

### 3.5 `extractNameEn(ref)`

`celebrity_preview.title = "弗兰克·德拉邦特 Frank Darabont"`：取空格后含拉丁字母的尾段为英文名。

---

## 4. `ensureGenreIds / Country / Language / Tag` —— 字典懒插

```java
for (String raw : names) {
    String name = raw.trim();
    Genre g = genreMapper.selectOne(new QueryWrapper<Genre>().eq("name", name).last("LIMIT 1"));
    if (g == null) {
        g = new Genre();
        g.setName(name);
        genreMapper.insert(g);
    }
    ids.add(g.getId());
}
```

### 设计取舍

- **LIMIT 1**：name 有 UNIQUE 索引，理论上 selectOne 已经只有一行；显式 limit 1 是双重保险（个别历史脏数据 case）。
- **不批量 IN 查**：本可以 `WHERE name IN (...)` 一次性查所有，但 250 部规模下没必要。后续遇到性能问题再优化。
- **字典 30/25/17 条预置**：导入时绝大多数走 selectOne 命中分支。新国家如"南非"不在预置里，会自动 INSERT。

### Tag 特殊性

豆瓣 tags 在 top250.json 大多空（豆瓣后续才补的字段）。但保留接口：将来如果爬虫扩展抓 tag，导入逻辑无需改。

---

## 5. `replaceMovie{Genre,Country,Language,Tag}` —— delete-then-insert

```java
private void replaceMovieGenre(Long movieId, Set<Integer> genreIds) {
    movieGenreMapper.delete(new QueryWrapper<MovieGenre>().eq("movie_id", movieId));
    for (Integer gid : genreIds) {
        MovieGenre row = new MovieGenre();
        row.setMovieId(movieId);
        row.setGenreId(gid);
        movieGenreMapper.insert(row);
    }
}
```

### 为什么 delete-then-insert 而不是 diff？

简单且**幂等**：不管表里现在是什么状态，跑完一定就是 dto 里的最新。逻辑像声明式而非命令式，更易推理。

代价：每部 4 张关联表至少 4 条 DELETE + 平均 8 条 INSERT。250 部 ≈ 3000 条小 SQL。开发期间无所谓；要进一步优化可以：
- 先 SELECT 现有 ids 集合，diff 出 toDelete/toInsert，只跑差异
- 用批量 INSERT `VALUES (?,?), (?,?)...` 减少 round-trip

---

## 6. `replaceMovie{Directors,Writers,Actors}` —— 演职员

### 6.1 排序

```java
int order = 0;
for (SeedPersonRefDTO ref : details) {
    Long pid = personIdByKey.get(personKey(ref));
    if (pid == null) continue;
    order++;
    row.setSortOrder(order);
}
```

`sort_order` 从 1 开始递增。**person upsert 失败的不计**（continue 不递增 order），保证连续性。

### 6.2 actor 特殊：is_lead

```java
row.setIsLead(order <= 5 ? 1 : 0);
```

约定 sort_order ≤ 5 为主演。冗余字段加速"查这部电影主演"，避免 ORDER BY + LIMIT 5 的扫描。

### 6.3 同部同人重复防御

```java
Set<Long> seen = new HashSet<>();
for (...) {
    if (pid == null || !seen.add(pid)) continue;
    ...
}
```

豆瓣个别老电影 JSON 里同一人在 actor_details + celebrity_preview 都列了。复合主键 `(movie_id, person_id)` 会拒绝第二次插入 → DuplicateKeyException 让事务回滚整部失败。所以代码里先 `Set` 去重。

director / writer 用 `try-catch DuplicateKeyException` 兜底（数据特殊场景双保险）。

---

## 7. `replaceMovieAka` —— 又名

最简单的关联表：每条字符串一行。

```java
movieAkaMapper.delete(new QueryWrapper<MovieAka>().eq("movie_id", movieId));
for (String t : akaList) {
    MovieAka row = new MovieAka();
    row.setMovieId(movieId);
    row.setTitle(t.trim());
    movieAkaMapper.insert(row);
}
```

`movie_aka` 主键是自增 id（每次重导 id 会变化），所以 delete-then-insert 不会主键冲突。

---

## 8. `replaceMovieReleaseDates` —— 上映日期解析

```java
for (String raw : rawList) {
    SeedTextUtils.ReleaseParse parsed = SeedTextUtils.parseRelease(raw);
    row.setRawText(raw.trim());
    row.setReleaseAt(parsed.date());      // 可能 null
    row.setRegion(parsed.region());        // 可能 null
    movieReleaseDateMapper.insert(row);
}
```

**raw_text 永远填原文**，方便后续重新解析。`release_at` 解析失败留 null，不报错。

---

## 9. `replaceMovieAwards` —— 奖项 + 颁奖典礼模糊匹配

### 9.1 ceremony_id 模糊匹配

```java
private Integer matchCeremonyId(String ceremonyText) {
    for (AwardCeremony c : awardCeremonyMapper.selectList(null)) {
        if (c.getName() != null && ceremonyText.contains(c.getName())) {
            return c.getId();
        }
    }
    return null;
}
```

豆瓣的 ceremony 文本：`"第67届奥斯卡金像奖"`，字典里是 `"奥斯卡金像奖"`。简单 `contains` 匹配。

**为什么全表捞下来内存匹配，不走 SQL LIKE？** 字典只有 12 条，捞下来 0.x ms，**还可以缓存到内存**（这里没做，懒，250 部 × 12 条 = 3000 次小查询也无所谓）。下一步优化可以加 `@PostConstruct` 加载到 `Map<Integer, String>`。

### 9.2 status 归一化

JSON 里有 `"won"` / `"nominated"` 也有 `"获奖"` / `"提名"`：

```java
public static String normalizeAwardStatus(String s) {
    return switch (s.toLowerCase().trim()) {
        case "won", "获奖" -> "won";
        case "nominated", "提名" -> "nominated";
        default -> "unknown";
    };
}
```

数据库 ENUM 列是 `('won','nominated','unknown')`，Java 这层把多语言 / 大小写统一掉。

### 9.3 recipients

```java
SeedPersonRefDTO first = a.getRecipients().get(0);
row.setRecipientPersonId(personIdByKey.get(personKey(first)));
row.setRecipientText(a.getRecipients().stream()
    .map(SeedPersonRefDTO::getName)
    .filter(StringUtils::hasText)
    .collect(Collectors.joining(", ")));
```

第一个作为 FK，全部名字拼字符串。这个简化让"《XXX》获奖人是谁"既能精确点开（FK），又能一行展示全员（text）。

---

## 10. `replaceMovieRelated` —— 相关电影

```java
row.setRelatedDoubanId(r.getSubjectId());
// related_movie_id 留 NULL，全量后 backfill 统一填
```

**第一遍循环时所有 related_movie_id 都是 NULL**。比如导入肖申克时，related 列出了"阿甘正传"，但阿甘还没进 movies 表 → 没法拿到 movie.id。

**全量结束后**，`backfillRelatedMovieIds()` 跑一条 SQL：

```sql
UPDATE movie_related r
JOIN movies m ON r.related_douban_id = m.douban_id
SET r.related_movie_id = m.id
WHERE r.related_movie_id IS NULL;
```

一次性填完所有能匹配的（top250 内推荐基本都在库内）。

---

## 11. `replaceMovieComments` —— 短评（最大表）

### 11.1 不删旧的

```java
// 注意：不调用 movieCommentMapper.delete(...)
if (StringUtils.hasText(c.getCommentId())) {
    Long cnt = movieCommentMapper.selectCount(...);
    if (cnt > 0) continue;   // 已存在 → 跳过
}
movieCommentMapper.insert(row);
```

**关键差异**：和 genre/aka 等"replace"模式不同，comment 用"INSERT-IF-NOT-EXISTS"模式。

为什么？因为 `movie_comment` 同时承载：
- source = 0：豆瓣抓的（导入器写）
- source = 1：站内用户写的（业务代码写）

如果 delete-then-insert 会误删站内用户短评！所以严格只插新的（按 `douban_comment_id` UNIQUE 去重）。

### 11.2 douban_comment_id 缺失

极少数 JSON 短评没 comment_id（爬虫漏抓）。`if (StringUtils.hasText(c.getCommentId()))` 才走防重；缺失时直接 INSERT，可能多次重启导致重复。生产可以合成 `_synthetic_:movieId_userName_postedAt`。

### 11.3 字段映射坑

JSON 字段叫 `status`，字面意思是"评价状态"——其实就是"力荐/推荐/还行/较差/很差"。映射到 `movie_comment.rating_label`（避免和"won/nominated"那个 status 冲突）。

---

## 12. `replaceMovieRatingDist / GenreRank`

### 12.1 5 星分布
JSON 里 `rating_breakdown` 形如 `{star: 5, label: "5星", text: "力荐", percentage: 85.6}`。

```java
row.setLabel(StringUtils.hasText(d.getText()) ? d.getText() : d.getLabel());
```

数据库 `movie_rating_dist.label` 想要"力荐"这种带语义的字符串而不是"5星"。优先 `text`，fallback `label`。

### 12.2 类型百分位 genre_name 防重

```java
Set<String> seen = new HashSet<>();
for (...) {
    if (!seen.add(r.getGenre())) continue;
    ...
}
```

`movie_genre_rank` 主键 `(movie_id, genre_name)`。豆瓣 JSON 偶尔会列两次同一类型（如同时有"剧情"和"剧情片"被规范成同一个 genre），Set 去重防 dup key。

---

## 13. `upsertTop250` —— 1:1 排名

```java
if (rankNo == null) return;       // 这部不在 Top250
MovieTop250 existing = movieTop250Mapper.selectById(movieId);
MovieTop250 row = existing == null ? new MovieTop250() : existing;
row.setMovieId(movieId);
row.setRankNo(rankNo);
row.setQuote(quote);
row.setSnapshotAt(LocalDateTime.now());
if (existing == null) movieTop250Mapper.insert(row);
else movieTop250Mapper.updateById(row);
```

主键就是 `movie_id`（1:1 关系）。`rank_no` UNIQUE，所以理论上只能 250 部命中分支。如果数据集变化（新版 top250），rank 重新洗牌时可能命中冲突 → 这里假设单批导入数据自洽。

---

## 14. `aggregatePersonStats` —— 全量后聚合

```java
private void aggregatePersonStats() {
    personMapper.recalcMovieCount();
    personMapper.recalcAvgMovieRating();
}
```

两条 `UPDATE...JOIN`（在 `PersonMapper` 里 `@Update` 注解定义）。

```sql
UPDATE persons p
LEFT JOIN (
    SELECT person_id, COUNT(DISTINCT movie_id) cnt FROM (
        SELECT person_id, movie_id FROM movie_director
        UNION ALL
        SELECT person_id, movie_id FROM movie_writer
        UNION ALL
        SELECT person_id, movie_id FROM movie_actor
    ) u GROUP BY person_id
) s ON s.person_id = p.id
SET p.movie_count = COALESCE(s.cnt, 0);
```

**为什么单条 SQL 而不在 Java 里循环**：1500 个 person × 3 张关联表 = 内存里聚合很麻烦，SQL 一次搞定，毫秒级。

---

## 15. `reimport()` —— 全量重导

```java
@Override
public ImportResult reimport() {
    movieMapper.delete(null);    // 级联清掉所有 movie_xxx
    personMapper.delete(null);
    return importAll();
}
```

利用 schema 的 `ON DELETE CASCADE`：删 movies 一行 → 自动级联清掉 movie_genre/movie_actor/.../movie_top250 等所有关联+富字段。

`personMapper.delete(null)` 单独清，因为 persons 不在 movies 的 cascade 链上。

字典（genres/countries/languages/tags/award_ceremonies）和 users 不动。

**警告**：reimport 会丢站内用户短评（source=1）！后续优化：先把 source=1 的 movie_comment 备份（dump 到临时表），reimport 后再 restore。

---

## 16. SeedAutoRunner —— 启动条件触发

```java
@ConditionalOnProperty(name = "moveme.seed.auto-import", havingValue = "true")
public class SeedAutoRunner implements ApplicationRunner {
    public void run(...) {
        long count = movieMapper.selectCount(null);
        if (count >= 50) {
            log.info("Skip: movies already has {} rows", count);
            return;
        }
        seedImportService.importAll();
    }
}
```

两个开关：
1. `@ConditionalOnProperty` —— 配置 false 时这个 Bean 都不被创建（连依赖注入都不消耗）
2. 行数 < 50 守卫 —— 已有数据时跳过避免重复导入

调试技巧：调通业务后**关掉 auto-import**（或保留 true 但把行数阈值设高），免得每次启动都尝试导入。

---

## 17. PosterFileImporter —— 海报本地化

```java
public void copy(SeedMovieDTO dto, Long movieId) {
    Path source = locateSource(dto);   // 找 data/images/{rank}_{title}.jpg
    if (source == null) return;
    Path target = Paths.get(posterOutDir, dto.getSubjectId() + ".jpg");
    Files.copy(source, target, REPLACE_EXISTING);
    movieMapper.updatePosterLocalPath(movieId, "/static/posters/" + dto.getSubjectId() + ".jpg");
}
```

匹配优先级：
1. 文件名前缀 = 3 位数字 rank（`"001_xxx.jpg"`）—— 最准
2. 文件名包含 title 子串 —— 兜底

文件不存在时直接 return（不抛），允许用户没下载 images 也能跑。

**WebMvcConfig 已经把 `/static/posters/**` 映射到磁盘 `./data/posters/`**，所以前端 `<img src="/static/posters/1292052.jpg">` 直接能展示。

---

## 18. 测试调通建议

### 18.1 改步骤导入前 5 部
临时改 `importAll`：

```java
for (SeedMovieDTO dto : movies.subList(0, 5)) {  // 仅前 5
```

调通后改回 `for (SeedMovieDTO dto : movies)`.

### 18.2 看 import_logs
```sql
SELECT id, source, movies_total, movies_ok, movies_fail,
       persons_ok, comments_ok, started_at, finished_at,
       SUBSTRING(errors, 1, 500) AS errors_preview
FROM import_logs
ORDER BY id DESC LIMIT 5;
```

### 18.3 抽样验证（肖申克）
```sql
SELECT m.title, m.title_cn, m.title_en, t.rank_no, t.quote,
       m.douban_rating, m.wish_count, m.collect_count,
       (SELECT COUNT(*) FROM movie_actor WHERE movie_id=m.id) actors,
       (SELECT COUNT(*) FROM movie_comment WHERE movie_id=m.id AND source=0) cmts,
       (SELECT COUNT(*) FROM movie_award WHERE movie_id=m.id) awards
FROM movies m JOIN movie_top250 t ON m.id=t.movie_id
WHERE m.douban_id='1292052';
```

期望：rank=1, quote='希望让人自由。', douban_rating=9.7, actors=25, cmts=30, awards=3

---

## 19. 性能数据（参考）

250 部规模：
- JSON 加载（11 MB）：~200 ms
- 单部 importOne：~50-150 ms
- 总耗时：~30-60 s
- DB 写入次数：约 50,000+ 条小 SQL（关联表占大头）

**优化方向**（暂不做）：
1. 关联表批量 INSERT（`VALUES (?,?), (?,?)...`）—— 减 80% round-trip
2. persons 批量 SELECT IN —— 一次查所有当批 person_ids
3. 字典提前预热到内存 Map<name, id>
4. 评论表用 `INSERT IGNORE` 替代 selectCount + insert

预估优化后总耗时降到 ~5-10 s。当前 30 s 可以接受 —— 一次性活动，不在热路径。

---

## 20. 未来扩展

| 扩展 | 在哪改 | 大致工作量 |
|---|---|---|
| 支持非 Top250 电影增量导入 | 复用 importOne，绕过 SeedAutoRunner 的"行数<50"守卫 | 0.5 天 |
| Python 爬虫产新 JSON → 自动导入 | 加 admin REST 端点 `/admin/seed/import?path=...` | 0.5 天 |
| reimport 保留站内短评 | 先 SELECT 备份 source=1 的行，reimport 后 restore | 1 天 |
| 性能优化（批量 INSERT） | 关联表 mapper 加 `@Insert` 批量方法 | 1 天 |
| 多榜单支持 | top250 → 推广成 movie_list / movie_list_membership | 2 天 |

---

## 21. 文件清单速查

| 路径 | 作用 |
|---|---|
| `module/seed/dto/SeedMovieDTO.java` 等 11 个 | 贴 JSON 的 POJO |
| `module/seed/support/ImportContext.java` | personCache + 计数器 |
| `module/seed/support/ImportResult.java` | 导入结果 record |
| `module/seed/service/SeedImportService.java` | 接口 |
| `module/seed/service/impl/Top250SeedImportServiceImpl.java` | **★ 主实现** |
| `module/seed/util/SeedTextUtils.java` | splitTitle / parseRelease 等小工具 |
| `module/seed/util/PosterFileImporter.java` | 海报本地化 |
| `module/seed/runner/SeedAutoRunner.java` | 启动时触发 |
| `module/seed/entity/ImportLog.java` + mapper | 导入日志 |
| `module/movie/entity/*` 多个 entity | v2 表的 16 个新 entity |
| `module/movie/mapper/PersonMapper.java` | 含 recalcMovieCount/recalcAvgMovieRating |
| `module/movie/mapper/MovieRelatedMapper.java` | 含 backfillRelatedMovieIds |

完成 Phase B 后跳到 [12 - Phase C 实现指南](./12-Phase-C-推荐特征计算实现指南.md) 写推荐特征计算。
