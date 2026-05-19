# 13 - Phase B：业务逻辑实现细节（你来写的部分）

> 本文档配合 [11 - Phase B 实现指南](./11-Phase-B-种子导入器实现指南.md)。
>
> Phase B 的"外壳代码"我已经写好（流程编排 / JSON 加载 / 事务 / 错误兜底 / 日志 / 海报 / Runner）。
> 这里详细讲**留给你写**的 17 个 stub 方法 —— 算法、字段映射、坑点、推荐写法。
>
> 文件位置：`moveme-backend/src/main/java/com/moveme/module/seed/service/impl/Top250SeedImportServiceImpl.java`
>
> 完成顺序建议：先写 `upsertMovie` + `upsertAllPersons`（最核心），再 `ensure*` + `replace*`。

---

## 0. 准备工作

打开 IDE，把 `Top250SeedImportServiceImpl.java` 边看边对照本文档实现。运行验证：把 `application.yml` 的 `moveme.seed.auto-import` 改 true，启动后端。看日志期望出现：

```
Top250 seed import finished: total=250 ok=250 fail=0 persons=>=1500 comments=>=10000
```

调试期可临时让 `SeedAutoRunner` 只导前 5 部（`movies.subList(0, 5)`）加快迭代。

---

## 1. `upsertMovie(SeedMovieDTO dto)` — 最核心

把 SeedMovieDTO 灌入 `movies` 主表。

### 1.1 字段映射对照

| Movie 字段 | 来源 | 备注 |
|---|---|---|
| `doubanId` | `dto.subjectId` | UNIQUE，幂等键 |
| `imdbId` | `dto.imdb` | 可空 |
| `title` | `dto.title` | 主标题 |
| `titleCn` | 拆 `dto.title` | 见 1.2 |
| `titleEn` | 拆 `dto.title` | 见 1.2 |
| `titlePinyin` | 由 `titleCn` 生成 | 可空（要装 pinyin4j），先留 null |
| `summary` | `dto.summary` | TEXT |
| `summaryShort` | `dto.summary` 截 200 字 | 注意中文 `String.codePointCount` |
| `year` | `dto.year` | |
| `durationMinutes` | 从 `dto.runtimes[0]` 提取数字 | "142分钟" → 142 |
| `durationText` | `dto.runtimes[0]` 原文 | |
| `releaseDate` | 从 `dto.releaseDates` 解析最早日期 | 见 8 节 |
| `posterUrl` | `dto.coverImage` | |
| `posterLocalPath` | 由 `PosterFileImporter` 单独写 | 这里别动 |
| `officialSite` | `dto.officialSite` | |
| `doubanRating` | `dto.rating.value` 转 BigDecimal | scale=1 |
| `doubanVotes` | `dto.rating.votes` 转 Integer | 可能溢出 long → int，看下界 |
| `wishCount` | `dto.interestCounts.wish` | |
| `collectCount` | `dto.interestCounts.collect` | |
| `status` | 固定 1 | 0=隐藏 1=正常 |
| `detailFetchedAt` | 这里别填 | importOne 末尾外壳已经 set |

### 1.2 拆 title 成中英两段

`dto.title` 形如 `"肖申克的救赎 The Shawshank Redemption"`。约定空格首次出现是分隔。

```java
String full = dto.getTitle().trim();
int sp = full.indexOf(' ');
String cn, en = null;
if (sp > 0 && containsLatin(full.substring(sp + 1))) {
    cn = full.substring(0, sp);
    en = full.substring(sp + 1).trim();
} else {
    cn = full;
}
// containsLatin 自己写：return s.codePoints().anyMatch(cp -> cp < 0x4E00);
```

豆瓣个别标题没有英文（如纯中文电影），cn 取全文，en = null。

### 1.3 幂等 upsert 套路

```java
private Long upsertMovie(SeedMovieDTO dto) {
    Movie m = mapToEntity(dto);   // 把 dto → Movie

    Movie existing = movieMapper.selectOne(
        new QueryWrapper<Movie>().eq("douban_id", dto.getSubjectId()));
    if (existing == null) {
        movieMapper.insert(m);
        return m.getId();
    } else {
        m.setId(existing.getId());
        m.setCreatedAt(existing.getCreatedAt());   // 不覆盖创建时间
        movieMapper.updateById(m);
        return existing.getId();
    }
}
```

### 1.4 坑点

- **`releaseDate` 的解析见 8 节**。最早日期：从 `release_dates` 数组解析所有 `LocalDate`，取 `min`。
- **`durationMinutes` 提取**：`runtimes[0].replaceAll("\\D","")` 太粗（"120分钟(导演剪辑版)" 会变 120 但没问题）；保险用正则 `^\d+`。
- **小心 `BigDecimal`**：`new BigDecimal(double)` 有精度问题，用 `BigDecimal.valueOf(double)`。

---

## 2. `upsertAllPersons(SeedMovieDTO dto, ImportContext ctx)` — 核心

合并所有人物源 → upsert `persons` 表。

### 2.1 人物来源

| 来源字段 | 含 id？ | 含 avatar？ | 含 role？ |
|---|---|---|---|
| `directorDetails` | ✅ | ❌ | ❌ |
| `writerDetails` | ✅ | ❌ | ❌ |
| `actorDetails` | ✅ | ❌ | ❌ |
| `celebrityPreview` | ✅ | ✅ | ✅ |
| `awards[].recipients` | ✅ | ❌ | ❌ |

合并策略：以 `id` 为去重 key；同一 id 多个引用时，按 avatar/title 优先取**最丰富**的那条。

### 2.2 personKey 工具方法

```java
private String personKey(SeedPersonRefDTO ref) {
    if (StringUtils.hasText(ref.getId())) return ref.getId();
    // id 缺失（罕见）：用合成 key 保 UNIQUE
    return "_synthetic_:" + nullSafe(ref.getName()) + "|" + nullSafe(ref.getTitle());
}
```

### 2.3 流程

```java
private Map<String, Long> upsertAllPersons(SeedMovieDTO dto, ImportContext ctx) {
    // 1. 收集（保留最佳那条 ref）
    Map<String, SeedPersonRefDTO> refs = new LinkedHashMap<>();
    addAllRefs(refs, dto.getDirectorDetails());
    addAllRefs(refs, dto.getWriterDetails());
    addAllRefs(refs, dto.getActorDetails());
    addAllRefs(refs, dto.getCelebrityPreview());     // 最后加，避免覆盖含 avatar 的条目 — 反过来：先加无 avatar 的，再 merge 含 avatar 的
    if (dto.getAwards() != null) {
        for (SeedAwardDTO a : dto.getAwards()) addAllRefs(refs, a.getRecipients());
    }

    // 2. 对每个 ref upsert
    Map<String, Long> result = new HashMap<>();
    for (SeedPersonRefDTO ref : refs.values()) {
        String key = personKey(ref);
        Long id = ctx.getCachedPersonId(key);
        if (id != null) {
            result.put(key, id);
            continue;
        }
        Person p = personMapper.selectOne(
            new QueryWrapper<Person>().eq("douban_person_id", key));
        if (p == null) {
            p = new Person();
            p.setDoubanPersonId(key);
            p.setName(ref.getName());
            p.setNameEn(extractNameEn(ref));   // 见 2.4
            p.setAvatarUrl(ref.getAvatar());
            p.setProfileUrl(ref.getUrl());
            personMapper.insert(p);
            ctx.incrementPersons();
        } else {
            // 老条目可能没 avatar/name_en，新引用补上
            boolean dirty = false;
            if (p.getAvatarUrl() == null && ref.getAvatar() != null) {
                p.setAvatarUrl(ref.getAvatar()); dirty = true;
            }
            String nameEn = extractNameEn(ref);
            if (p.getNameEn() == null && nameEn != null) {
                p.setNameEn(nameEn); dirty = true;
            }
            if (dirty) personMapper.updateById(p);
        }
        ctx.cachePersonId(key, p.getId());
        result.put(key, p.getId());
    }
    return result;
}

private void addAllRefs(Map<String, SeedPersonRefDTO> map, List<SeedPersonRefDTO> list) {
    if (list == null) return;
    for (SeedPersonRefDTO r : list) {
        if (r == null) continue;
        String k = personKey(r);
        // 已有时合并：保留含 avatar/title 的那个
        SeedPersonRefDTO old = map.get(k);
        if (old == null) { map.put(k, r); continue; }
        if (old.getAvatar() == null && r.getAvatar() != null) old.setAvatar(r.getAvatar());
        if (old.getTitle() == null && r.getTitle() != null) old.setTitle(r.getTitle());
        if (old.getRole() == null && r.getRole() != null) old.setRole(r.getRole());
    }
}
```

### 2.4 提取 nameEn

`celebrity_preview.title` 例 `"弗兰克·德拉邦特 Frank Darabont"`：用空格分隔，取后半段。`*_details` 没 title 字段，name_en 留 null。

```java
private String extractNameEn(SeedPersonRefDTO ref) {
    String t = ref.getTitle();
    if (!StringUtils.hasText(t)) return null;
    int sp = t.indexOf(' ');
    if (sp <= 0) return null;
    String tail = t.substring(sp + 1).trim();
    return containsLatin(tail) ? tail : null;
}
```

---

## 3. （已实现 — 略）

`importAll / importOne / loadJson / pickActorRefs / repairText / setDetailFetchedAt` 我已写完。

---

## 4. `ensureGenreIds / ensureCountryIds / ensureLanguageIds / ensureTagIds`

字典懒插入。注意 `genres` 已预置 30 个，`countries` / `languages` 也大半在，**导入时基本走 SELECT 命中分支**。

### 通用模板

```java
private Set<Integer> ensureGenreIds(List<String> names) {
    if (names == null || names.isEmpty()) return Set.of();
    Set<Integer> ids = new LinkedHashSet<>();
    for (String raw : names) {
        if (!StringUtils.hasText(raw)) continue;
        String name = raw.trim();
        Genre existing = genreMapper.selectOne(
            new QueryWrapper<Genre>().eq("name", name));
        if (existing != null) {
            ids.add(existing.getId());
        } else {
            Genre g = new Genre();
            g.setName(name);
            genreMapper.insert(g);
            ids.add(g.getId());
        }
    }
    return ids;
}
```

`ensureCountryIds` / `ensureLanguageIds` 完全照搬，换 mapper + entity。

`ensureTagIds`：top250 里 `tags` 大多空，stub 里我已经默认返回 `Collections.emptySet()` 不强求。

---

## 5. `replaceMovieGenre / replaceMovieCountry / replaceMovieLanguage / replaceMovieTag`

简单关联表：先删旧的，再批量插。

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

其他 3 个一模一样换 entity / 字段名。`MovieTag.genreId → tagId` 等同样套路。

> **优化（可不做）**：批量 insertBatch 减少 round-trip。MyBatis-Plus 提供 `saveBatch`（要 IService）或自己写 `insert ... values(?,?),(?,?)...`。250 部规模不优化也无所谓。

---

## 6. `replaceMovieDirectors / Writers / Actors`

类似 5，但 entity 字段更多（`sortOrder`、`isLead`、`roleName`）。

### 6.1 directors / writers

```java
private void replaceMovieDirectors(Long movieId, List<SeedPersonRefDTO> details,
                                   Map<String, Long> personIdByKey) {
    movieDirectorMapper.delete(new QueryWrapper<MovieDirector>().eq("movie_id", movieId));
    if (details == null) return;
    int order = 0;
    for (SeedPersonRefDTO ref : details) {
        Long pid = personIdByKey.get(personKey(ref));
        if (pid == null) continue;        // 防御：person upsert 应已成功
        order++;
        MovieDirector row = new MovieDirector();
        row.setMovieId(movieId);
        row.setPersonId(pid);
        row.setSortOrder(order);
        movieDirectorMapper.insert(row);
    }
}
```

`replaceMovieWriters` 完全一样改 entity 名。

### 6.2 actors（多 isLead + roleName）

```java
private void replaceMovieActors(Long movieId, List<SeedPersonRefDTO> refs,
                                Map<String, Long> personIdByKey) {
    movieActorMapper.delete(new QueryWrapper<MovieActor>().eq("movie_id", movieId));
    if (refs == null) return;
    int order = 0;
    for (SeedPersonRefDTO ref : refs) {
        Long pid = personIdByKey.get(personKey(ref));
        if (pid == null) continue;
        order++;
        MovieActor row = new MovieActor();
        row.setMovieId(movieId);
        row.setPersonId(pid);
        row.setRoleName(ref.getRole());     // celebrity_preview 才有
        row.setSortOrder(order);
        row.setIsLead(order <= 5 ? 1 : 0);
        movieActorMapper.insert(row);
    }
}
```

---

## 7. `replaceMovieAka(Long movieId, List<String> akaList)`

```java
private void replaceMovieAka(Long movieId, List<String> akaList) {
    movieAkaMapper.delete(new QueryWrapper<MovieAka>().eq("movie_id", movieId));
    if (akaList == null) return;
    for (String t : akaList) {
        if (!StringUtils.hasText(t)) continue;
        MovieAka row = new MovieAka();
        row.setMovieId(movieId);
        row.setTitle(t.trim());
        movieAkaMapper.insert(row);
    }
}
```

---

## 8. `replaceMovieReleaseDates(Long movieId, List<String> rawList)`

输入字符串形如：
- `"1994-09-10(多伦多电影节)"`
- `"1994-10-14(美国)"`
- `"1995"`（仅年份，少见）

正则 + 容错：

```java
private static final java.util.regex.Pattern RELEASE_RE =
    java.util.regex.Pattern.compile("^(\\d{4}(?:-\\d{2}-\\d{2})?)(?:\\((.+)\\))?$");

private void replaceMovieReleaseDates(Long movieId, List<String> rawList) {
    movieReleaseDateMapper.delete(new QueryWrapper<MovieReleaseDate>().eq("movie_id", movieId));
    if (rawList == null) return;
    for (String raw : rawList) {
        if (!StringUtils.hasText(raw)) continue;
        MovieReleaseDate row = new MovieReleaseDate();
        row.setMovieId(movieId);
        row.setRawText(raw.trim());

        java.util.regex.Matcher m = RELEASE_RE.matcher(raw.trim());
        if (m.matches()) {
            String dateStr = m.group(1);
            String region = m.group(2);
            row.setRegion(region);
            try {
                row.setReleaseAt(dateStr.length() == 10
                    ? LocalDate.parse(dateStr)
                    : LocalDate.of(Integer.parseInt(dateStr), 1, 1));
            } catch (Exception ignored) { /* 留 null */ }
        }
        movieReleaseDateMapper.insert(row);
    }
}
```

`upsertMovie` 里 `movies.releaseDate` 取**最早**那个：把 rawList 解析后取 `min(releaseAt)`，可以独立写个 helper。

---

## 9. `replaceMovieAwards(Long movieId, List<SeedAwardDTO> awards, Map<String,Long> personIdByKey)`

```java
private void replaceMovieAwards(Long movieId, List<SeedAwardDTO> awards,
                                Map<String, Long> personIdByKey) {
    movieAwardMapper.delete(new QueryWrapper<MovieAward>().eq("movie_id", movieId));
    if (awards == null) return;
    for (SeedAwardDTO a : awards) {
        if (a == null || !StringUtils.hasText(a.getName())) continue;
        MovieAward row = new MovieAward();
        row.setMovieId(movieId);
        row.setCeremonyId(matchCeremonyId(a.getName()));   // LIKE 匹配，匹不上留 null
        row.setCeremonyText(a.getName());
        row.setCategory(a.getCategory());
        row.setStatus(normalizeStatus(a.getStatus()));     // 'won'/'nominated'/'unknown'
        row.setAwardUrl(a.getUrl());

        if (a.getRecipients() != null && !a.getRecipients().isEmpty()) {
            // 第一个作为 recipient_person_id
            SeedPersonRefDTO first = a.getRecipients().get(0);
            row.setRecipientPersonId(personIdByKey.get(personKey(first)));
            // 全部 name 拼字符串
            row.setRecipientText(a.getRecipients().stream()
                .map(SeedPersonRefDTO::getName)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.joining(", ")));
        }
        movieAwardMapper.insert(row);
    }
}

private Integer matchCeremonyId(String ceremonyText) {
    if (!StringUtils.hasText(ceremonyText)) return null;
    // 尝试匹配 "奥斯卡" / "金球" / "戛纳" / "金马" / "金像" / ...
    AwardCeremony c = awardCeremonyMapper.selectOne(
        new QueryWrapper<AwardCeremony>()
            .last("LIMIT 1")
            .apply("{0} LIKE CONCAT('%', name, '%')", ceremonyText));
    return c == null ? null : c.getId();
}

private String normalizeStatus(String s) {
    if (s == null) return "unknown";
    return switch (s.toLowerCase()) {
        case "won", "获奖" -> "won";
        case "nominated", "提名" -> "nominated";
        default -> "unknown";
    };
}
```

---

## 10. `replaceMovieRelated(Long movieId, List<SeedRelatedMovieDTO> related)`

```java
private void replaceMovieRelated(Long movieId, List<SeedRelatedMovieDTO> related) {
    movieRelatedMapper.delete(new QueryWrapper<MovieRelated>().eq("movie_id", movieId));
    if (related == null) return;
    int order = 0;
    for (SeedRelatedMovieDTO r : related) {
        if (r == null) continue;
        order++;
        MovieRelated row = new MovieRelated();
        row.setMovieId(movieId);
        row.setRelatedDoubanId(r.getSubjectId());
        row.setRelatedTitle(r.getTitle());
        row.setRelatedRating(r.getRating() == null ? null : BigDecimal.valueOf(r.getRating()));
        row.setRelatedCoverUrl(r.getCoverImage());
        row.setSortOrder(order);
        // related_movie_id 留 NULL — 全量结束后由 backfillRelatedMovieIds 统一填
        movieRelatedMapper.insert(row);
    }
}
```

---

## 11. `replaceMovieComments(Long movieId, List<SeedCommentDTO> comments) → int`

```java
private int replaceMovieComments(Long movieId, List<SeedCommentDTO> comments) {
    if (comments == null) return 0;
    int inserted = 0;
    for (SeedCommentDTO c : comments) {
        if (c == null || !StringUtils.hasText(c.getContent())) continue;
        // 防重：先查 douban_comment_id
        if (StringUtils.hasText(c.getCommentId())) {
            Long cnt = movieCommentMapper.selectCount(
                new QueryWrapper<MovieComment>().eq("douban_comment_id", c.getCommentId()));
            if (cnt > 0) continue;
        }
        MovieComment row = new MovieComment();
        row.setMovieId(movieId);
        row.setSource(0);                      // 豆瓣源
        row.setDoubanCommentId(c.getCommentId());
        if (c.getUser() != null) {
            row.setAuthorName(c.getUser().getName());
            row.setAuthorAvatar(c.getUser().getAvatar());
        }
        row.setAuthorLocation(c.getLocation());
        row.setRating(c.getRating());
        row.setRatingLabel(c.getStatus());     // "力荐" 等
        row.setContent(c.getContent());
        row.setVotes(c.getVotes());
        row.setPostedAt(c.getCreatedAt());
        row.setSourceUrl(c.getSourceUrl());
        try {
            movieCommentMapper.insert(row);
            inserted++;
        } catch (org.springframework.dao.DuplicateKeyException ignored) {
            // 极少：JSON 里同一 commentId 出现两次 — 跳过
        }
    }
    return inserted;
}
```

> 不要用 `delete + insert` 模式 —— `source=1` 站内用户短评不能误删。

---

## 12. `replaceMovieRatingDist / replaceMovieGenreRank`

简单，先删后插。

```java
private void replaceMovieRatingDist(Long movieId, List<SeedRatingBreakdownDTO> dist) {
    movieRatingDistMapper.delete(new QueryWrapper<MovieRatingDist>().eq("movie_id", movieId));
    if (dist == null) return;
    for (SeedRatingBreakdownDTO d : dist) {
        if (d == null || d.getStar() == null) continue;
        MovieRatingDist row = new MovieRatingDist();
        row.setMovieId(movieId);
        row.setStar(d.getStar());
        row.setLabel(d.getText());          // "力荐" / "推荐" / ...（注意：JSON 里是 text，label 字段是 "5星"）
        row.setPercentage(d.getPercentage() == null ? BigDecimal.ZERO : d.getPercentage());
        movieRatingDistMapper.insert(row);
    }
}

private void replaceMovieGenreRank(Long movieId, List<SeedRatingBetterThanDTO> ranks) {
    movieGenreRankMapper.delete(new QueryWrapper<MovieGenreRank>().eq("movie_id", movieId));
    if (ranks == null) return;
    for (SeedRatingBetterThanDTO r : ranks) {
        if (r == null || !StringUtils.hasText(r.getGenre())) continue;
        MovieGenreRank row = new MovieGenreRank();
        row.setMovieId(movieId);
        row.setGenreName(r.getGenre());
        row.setPercentile(r.getPercentage() == null ? BigDecimal.ZERO : r.getPercentage());
        row.setRankUrl(r.getUrl());
        movieGenreRankMapper.insert(row);
    }
}
```

---

## 13. `upsertTop250(movieId, rankNo, listTitle, quote)`

```java
private void upsertTop250(Long movieId, Short rankNo, String listTitle, String quote) {
    if (rankNo == null) return;       // 这部不在 Top250
    MovieTop250 existing = movieTop250Mapper.selectById(movieId);
    MovieTop250 row = existing == null ? new MovieTop250() : existing;
    row.setMovieId(movieId);
    row.setRankNo(rankNo);
    row.setListTitle(listTitle);
    row.setQuote(quote);
    row.setSnapshotAt(LocalDateTime.now());
    if (existing == null) movieTop250Mapper.insert(row);
    else movieTop250Mapper.updateById(row);
}
```

> `rank_no` UNIQUE。如果导入顺序不一致或冲突，`existing` 命中后 update 即可。罕见地遇到"两部都自称 rank=1"会冲突，直接抛异常让 importOne 失败 → 这部进 errors。

---

## 14. `aggregatePersonStats()` — 可选

全量结束后聚合 persons 数据。两条 UPDATE：

```java
private void aggregatePersonStats() {
    // 用 @Update 注解直接给 PersonMapper 加方法，比这里写 jdbc 干净。
    // 或者直接走 MyBatis-Plus 的 update + Wrapper。

    // 草稿 SQL（建议放到 PersonMapper 里）:
    //
    // UPDATE persons p
    // LEFT JOIN (
    //   SELECT person_id, COUNT(DISTINCT movie_id) cnt FROM (
    //     SELECT person_id, movie_id FROM movie_director
    //     UNION ALL
    //     SELECT person_id, movie_id FROM movie_writer
    //     UNION ALL
    //     SELECT person_id, movie_id FROM movie_actor
    //   ) u GROUP BY person_id
    // ) s ON s.person_id = p.id
    // SET p.movie_count = COALESCE(s.cnt, 0);
    //
    // UPDATE persons p
    // LEFT JOIN (
    //   SELECT person_id, AVG(douban_rating) avg_r FROM (
    //     SELECT t.person_id, m.douban_rating FROM movie_actor t JOIN movies m ON m.id=t.movie_id
    //     UNION ALL
    //     SELECT t.person_id, m.douban_rating FROM movie_director t JOIN movies m ON m.id=t.movie_id
    //   ) u WHERE douban_rating IS NOT NULL GROUP BY person_id
    // ) s ON s.person_id = p.id
    // SET p.avg_movie_rating = COALESCE(s.avg_r, 0);
}
```

实现思路：在 `PersonMapper` 加两个 `@Update` 方法。

> 没做 Phase B 也能跑完，Phase C 的 `MovieFeatureBuilder` 不依赖这些字段。

---

## 15. 工具方法（建议新建 `SeedTextUtils.java`）

```java
public final class SeedTextUtils {
    private SeedTextUtils() {}

    public static boolean containsLatin(String s) {
        if (s == null) return false;
        return s.codePoints().anyMatch(cp -> cp <= 0x7F && Character.isLetter(cp));
    }

    public static String safe(String s) { return s == null ? "" : s; }

    public static String truncate(String s, int max) {
        if (s == null) return null;
        // 注意 codePoint 而非 char 长度（中文 1 codePoint = 1 视觉字符 = 1 char in BMP）
        return s.length() <= max ? s : s.substring(0, max);
    }
}
```

---

## 16. 验收 SQL（实现完跑一次）

```sql
SELECT
  (SELECT COUNT(*) FROM movies)                       AS movies,        -- 250
  (SELECT COUNT(*) FROM movie_top250)                 AS top250,        -- 250
  (SELECT COUNT(*) FROM persons)                      AS persons,       -- ≥1500
  (SELECT COUNT(*) FROM movie_actor)                  AS actors,        -- ≥2500
  (SELECT COUNT(*) FROM movie_director)               AS directors,     -- ≥250
  (SELECT COUNT(*) FROM movie_writer)                 AS writers,       -- ≥300
  (SELECT COUNT(*) FROM movie_genre)                  AS m_genre,       -- ≥600
  (SELECT COUNT(*) FROM movie_country)                AS m_country,     -- ≥300
  (SELECT COUNT(*) FROM movie_language)               AS m_language,    -- ≥350
  (SELECT COUNT(*) FROM movie_aka)                    AS akas,          -- ≥800
  (SELECT COUNT(*) FROM movie_release_date)           AS rdates,        -- ≥1000
  (SELECT COUNT(*) FROM movie_award)                  AS awards,        -- ≥400
  (SELECT COUNT(*) FROM movie_related)                AS relateds,      -- ≥2000
  (SELECT COUNT(*) FROM movie_comment WHERE source=0) AS comments,      -- ≥7000
  (SELECT COUNT(*) FROM movie_rating_dist)            AS rating_dist,   -- 1250 (250×5)
  (SELECT COUNT(*) FROM movie_genre_rank)             AS gr;            -- ≥500
```

抽样核对（肖申克）：

```sql
SELECT m.title, t.rank_no, t.quote, m.douban_rating, m.wish_count, m.collect_count
FROM movies m JOIN movie_top250 t ON m.id=t.movie_id
WHERE m.douban_id='1292052';
-- 期望: rank_no=1, quote='希望让人自由。', douban_rating>=9.7
```

---

## 17. 调试技巧

### 17.1 只导前 N 部
临时让 `SeedAutoRunner` 跑前 5 部加快迭代：把 `importAll()` 里的 `for (SeedMovieDTO dto : movies)` 改成 `for (SeedMovieDTO dto : movies.subList(0, Math.min(5, movies.size())))`。调通后改回。

### 17.2 看 import_logs
```sql
SELECT * FROM import_logs ORDER BY id DESC LIMIT 5;
```
`errors` 是 JSON 数组字符串，复制出来格式化看具体哪部失败。

### 17.3 单部重试
debug 时改某个 stub 后，可以直接 SQL 删该部数据再重启：
```sql
DELETE FROM movies WHERE douban_id='1292052';
-- 关联表会通过 ON DELETE CASCADE 自动清理
```
然后 `auto-import=true` 再启动 → 因 `count<50` 触发，但只重新导这 1 部。

### 17.4 SQL 日志
`application.yml` 里已经开了 `mybatis-plus.configuration.log-impl: StdOutImpl`，控制台会打每条 SQL。

---

## 18. 实现 checklist（按完成顺序）

- [ ] §1 `upsertMovie`
- [ ] §2 `upsertAllPersons`
- [ ] §4 `ensureGenreIds / ensureCountryIds / ensureLanguageIds`
- [ ] §5 `replaceMovieGenre / Country / Language / Tag`
- [ ] §6 `replaceMovieDirectors / Writers / Actors`
- [ ] §7 `replaceMovieAka`
- [ ] §8 `replaceMovieReleaseDates`
- [ ] §13 `upsertTop250`（先把 Top250 跑通）
- [ ] §9 `replaceMovieAwards`
- [ ] §10 `replaceMovieRelated`
- [ ] §11 `replaceMovieComments`
- [ ] §12 `replaceMovieRatingDist / GenreRank`
- [ ] §14 `aggregatePersonStats`（可选）
- [ ] §16 验收 SQL 全绿

完成后 → 进 [12 - Phase C 推荐特征计算实现指南](./12-Phase-C-推荐特征计算实现指南.md)。
