package com.moveme.module.recommend.controller;

import com.moveme.common.result.Result;
import com.moveme.module.recommend.dto.ChatRequest;
import com.moveme.module.recommend.service.MimoService;
import com.moveme.module.recommend.service.RecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Tag(name = "AI 推荐", description = "LLM 电影推荐接口")
@RestController
@RequestMapping("/api/v1/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    @Operation(summary = "聊天推荐（流式SSE）")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request,
                                  Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        SseEmitter emitter = new SseEmitter(120_000L);

        sseExecutor.execute(() -> {
            try {
                recommendService.chatStream(userId, request.getMessages(),
                        new MimoService.StreamCallback() {
                            @Override
                            public void onToken(String token) {
                                try {
                                    emitter.send(SseEmitter.event()
                                            .data(token, MediaType.TEXT_PLAIN));
                                } catch (IOException e) {
                                    emitter.completeWithError(e);
                                }
                            }

                            @Override
                            public void onComplete() {
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("done")
                                            .data(""));
                                } catch (IOException ignored) {
                                }
                                emitter.complete();
                            }

                            @Override
                            public void onError(Exception e) {
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("error")
                                            .data(e.getMessage()));
                                } catch (IOException ignored) {
                                }
                                emitter.completeWithError(e);
                            }
                        });
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> emitter.complete());
        return emitter;
    }

    @Operation(summary = "聊天推荐（非流式）")
    @PostMapping("/chat")
    public Result<String> chat(@Valid @RequestBody ChatRequest request,
                               Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String reply = recommendService.chat(userId, request.getMessages());
        return Result.success(reply);
    }

    @Operation(summary = "一键推荐（非流式）")
    @PostMapping("/quick")
    public Result<String> quickRecommend(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String reply = recommendService.quickRecommend(userId);
        return Result.success(reply);
    }
}
