package com.moveme.module.recommend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveme.module.recommend.config.LlmProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MimoService {

    private final RestTemplate restTemplate;
    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private LlmProperties.MimoProperties getMimo() {
        LlmProperties.MimoProperties mimo = llmProperties.getMimo();
        if (mimo == null || mimo.getApiKey() == null || mimo.getApiKey().isBlank()) {
            throw new RuntimeException("MiMO API Key 未配置，请设置环境变量 MIMO_API_KEY");
        }
        return mimo;
    }

    /**
     * 非流式调用 MiMO API，返回完整响应文本
     */
    public String chat(List<Map<String, String>> messages) {
        LlmProperties.MimoProperties mimo = getMimo();
        String url = mimo.getBaseUrl() + "/chat/completions";

        Map<String, Object> body = Map.of(
                "model", mimo.getModel(),
                "messages", messages,
                "temperature", 1.0,
                "top_p", 0.95,
                "stream", false,
                "thinking", Map.of("type", "disabled")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(mimo.getApiKey());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            String responseBody = response.getBody();
            log.info("MiMO API 响应: {}", responseBody);
            JsonNode root = objectMapper.readTree(responseBody);

            // 检查 API 返回的错误
            if (root.has("error")) {
                String errorMsg = root.path("error").path("message").asText("未知错误");
                log.error("MiMO API 返回错误: {}", errorMsg);
                throw new RuntimeException("MiMO API 错误: " + errorMsg);
            }

            return root.path("choices").path(0).path("message").path("content").asText();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("MiMO API 调用失败: {}", e.getMessage());
            throw new RuntimeException("MiMO API 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 流式调用 MiMO API，使用 Java HttpClient 处理 SSE
     */
    public void chatStream(List<Map<String, String>> messages, StreamCallback callback) {
        LlmProperties.MimoProperties mimo = getMimo();
        String url = mimo.getBaseUrl() + "/chat/completions";

        Map<String, Object> body = Map.of(
                "model", mimo.getModel(),
                "messages", messages,
                "temperature", 1.0,
                "top_p", 0.95,
                "stream", true,
                "thinking", Map.of("type", "disabled")
        );

        try {
            byte[] bodyBytes = objectMapper.writeValueAsBytes(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + mimo.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                log.error("MiMO API 返回 {}: {}", response.statusCode(), errorBody);
                callback.onError(new RuntimeException("MiMO API 返回 " + response.statusCode()));
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) {
                            callback.onComplete();
                            return;
                        }
                        try {
                            JsonNode chunk = objectMapper.readTree(data);
                            String content = chunk.path("choices").path(0)
                                    .path("delta").path("content").asText("");
                            if (!content.isEmpty()) {
                                callback.onToken(content);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
                callback.onComplete();
            }
        } catch (Exception e) {
            log.error("MiMO 流式调用失败: {}", e.getMessage());
            callback.onError(e);
        }
    }

    public interface StreamCallback {
        void onToken(String token);
        void onComplete();
        void onError(Exception e);
    }
}
