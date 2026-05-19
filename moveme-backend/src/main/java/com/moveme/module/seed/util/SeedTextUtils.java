package com.moveme.module.seed.util;

import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Phase B 导入器复用的小工具集。
 * 没业务依赖，纯字符串处理。
 */
public final class SeedTextUtils {

    private SeedTextUtils() {}

    /** 是否含 ASCII 字母（粗略判断"包含英文"）。 */
    public static boolean containsLatin(String s) {
        if (s == null) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c <= 0x7F && Character.isLetter(c)) return true;
        }
        return false;
    }

    /** 把 "肖申克的救赎 The Shawshank Redemption" 拆成 [titleCn, titleEn]。en 段必须含拉丁字母才算。 */
    public static String[] splitTitle(String full) {
        if (!StringUtils.hasText(full)) return new String[]{full, null};
        String trimmed = full.trim();
        int sp = trimmed.indexOf(' ');
        if (sp <= 0) return new String[]{trimmed, null};
        String head = trimmed.substring(0, sp);
        String tail = trimmed.substring(sp + 1).trim();
        if (containsLatin(tail)) {
            return new String[]{head, tail};
        }
        return new String[]{trimmed, null};
    }

    /** 从 "142分钟" / "120分钟(导演剪辑版)" 提取 142。提不到返回 null。 */
    public static Integer extractMinutes(String runtime) {
        if (!StringUtils.hasText(runtime)) return null;
        Matcher m = Pattern.compile("(\\d+)").matcher(runtime);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    /** 截到 max 字符（按 char，不区分代理对，250 部规模够用）。 */
    public static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** "won"/"获奖"→won，"nominated"/"提名"→nominated，其他→unknown。 */
    public static String normalizeAwardStatus(String s) {
        if (!StringUtils.hasText(s)) return "unknown";
        String t = s.toLowerCase().trim();
        return switch (t) {
            case "won", "获奖" -> "won";
            case "nominated", "提名" -> "nominated";
            default -> "unknown";
        };
    }

    /** 释放日期解析。返回 [LocalDate(可能为null), region(可能为null)]。 */
    private static final Pattern RELEASE_RE =
            Pattern.compile("^(\\d{4}(?:-\\d{2}-\\d{2})?)(?:\\((.+)\\))?$");

    public static ReleaseParse parseRelease(String raw) {
        if (!StringUtils.hasText(raw)) return new ReleaseParse(null, null);
        Matcher m = RELEASE_RE.matcher(raw.trim());
        if (!m.matches()) return new ReleaseParse(null, null);
        String dateStr = m.group(1);
        String region = m.group(2);
        java.time.LocalDate date = null;
        try {
            date = dateStr.length() == 10
                    ? java.time.LocalDate.parse(dateStr)
                    : java.time.LocalDate.of(Integer.parseInt(dateStr), 1, 1);
        } catch (Exception ignored) { /* keep null */ }
        return new ReleaseParse(date, region);
    }

    public record ReleaseParse(java.time.LocalDate date, String region) {}
}
