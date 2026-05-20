package com.moveme.module.recommend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "moveme.llm")
public class LlmProperties {

    private String defaultProvider;
    private MimoProperties mimo = new MimoProperties();

    @Data
    public static class MimoProperties {
        private String apiKey;
        private String model = "mimo-v2.5-pro";
        private String baseUrl = "https://token-plan-cn.xiaomimimo.com/v1";
    }
}
