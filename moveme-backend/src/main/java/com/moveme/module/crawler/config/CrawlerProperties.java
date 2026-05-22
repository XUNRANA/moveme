package com.moveme.module.crawler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "moveme.crawler")
public class CrawlerProperties {

    private boolean enabled = true;
    private Python python = new Python();
    private Schedule schedule = new Schedule();
    private String dataDir = "./data/crawler/";
    private int lockTimeoutMinutes = 120;

    @Data
    public static class Python {
        private String executable = "python";
        private String scriptsDir = "../crawler/";
        private String cookie = "";
        private double sleepSeconds = 1.5;
        private int requestTimeout = 15;
    }

    @Data
    public static class Schedule {
        private String chartCron = "0 0 3 ? * MON";
        private String commentsCron = "0 0 4 ? * MON";
        private String annualCron = "0 0 3 1 1 ?";
        private String enrichCron = "0 0 5 ? * MON";
    }
}
