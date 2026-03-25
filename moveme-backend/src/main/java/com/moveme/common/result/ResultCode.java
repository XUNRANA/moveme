package com.moveme.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 业务错误码 1xxx
    USER_ALREADY_EXISTS(1001, "用户名已存在"),
    EMAIL_ALREADY_EXISTS(1002, "邮箱已被注册"),
    WRONG_CREDENTIALS(1003, "用户名或密码错误"),
    USER_DISABLED(1004, "用户已被禁用"),

    // 爬虫错误码 2xxx
    CRAWLER_ALREADY_RUNNING(2001, "爬虫任务正在运行中"),
    CRAWLER_BLOCKED(2002, "爬虫已被目标网站限制"),

    // LLM 错误码 3xxx
    LLM_PROVIDER_UNAVAILABLE(3001, "LLM服务不可用"),
    LLM_QUOTA_EXCEEDED(3002, "LLM调用额度已用完");

    private final int code;
    private final String message;
}
