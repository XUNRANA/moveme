package com.moveme.module.seed.support;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * 跨电影共享的导入上下文。
 *
 * 核心：persons 缓存。一个人物（如摩根·弗里曼）会出现在多部电影里，
 * 用此缓存避免每部都查一次 DB —— key 是 personKey（豆瓣 person id 或合成 key），
 * value 是 persons.id。
 *
 * 计数器用于结果统计 + import_logs。
 */
@Getter
@Setter
public class ImportContext {

    /** key = douban_person_id 或 _synthetic_:name|nameEn，value = persons.id */
    private final Map<String, Long> personCache = new HashMap<>();

    private int personsUpserted = 0;
    private int commentsImported = 0;

    public Long getCachedPersonId(String key) {
        return personCache.get(key);
    }

    public void cachePersonId(String key, Long id) {
        personCache.put(key, id);
    }

    public void incrementPersons() {
        personsUpserted++;
    }

    public void addComments(int n) {
        commentsImported += n;
    }
}
