package com.moveme.module.recommend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {

    @NotEmpty(message = "消息列表不能为空")
    @Valid
    private List<ChatMessage> messages;

    @Data
    public static class ChatMessage {
        @NotEmpty(message = "角色不能为空")
        private String role;

        @NotEmpty(message = "消息内容不能为空")
        private String content;
    }
}
