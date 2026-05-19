package com.moveme.module.seed.support;

import java.util.List;

/**
 * 导入批次结果摘要。给调用方 / 日志 / import_logs 表使用。
 */
public record ImportResult(
        int total,
        int success,
        int fail,
        int personsUpserted,
        int commentsImported,
        long elapsedMs,
        List<String> errors
) {
}
