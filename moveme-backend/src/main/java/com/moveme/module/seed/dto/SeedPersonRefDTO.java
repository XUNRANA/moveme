package com.moveme.module.seed.dto;

import lombok.Data;

/**
 * 通用人物引用：覆盖 director_details / writer_details / actor_details / celebrity_preview / award.recipients。
 *
 * 来源差异：
 *   - *_details: { id, name, url }
 *   - celebrity_preview: { id, name, title, role, url, avatar }
 *   - award.recipients: { id, name, url }
 *
 * 全部字段都标 @Nullable —— 哪个来源缺什么字段就是 null。
 * "id" 即豆瓣 personage id（字符串）。
 */
@Data
public class SeedPersonRefDTO {
    private String id;
    private String name;
    /** celebrity_preview 独有，例如 "弗兰克·德拉邦特 Frank Darabont" */
    private String title;
    /** celebrity_preview 独有，例如 "导演" */
    private String role;
    private String url;
    /** celebrity_preview 独有：人物头像 URL */
    private String avatar;
}
