package com.moveme.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** 海报本地目录（磁盘路径），由 application.yml 配置；默认指向 ./static/posters */
    @Value("${moveme.poster.dir:./static/posters}")
    private String posterDir;

    /** 海报对外 URL 前缀 */
    @Value("${moveme.poster.url-prefix:/static/posters}")
    private String posterUrlPrefix;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/static/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "HEAD", "OPTIONS")
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String diskPrefix = "file:" + posterDir.replace('\\', '/');
        if (!diskPrefix.endsWith("/")) diskPrefix += "/";

        String urlPattern = posterUrlPrefix.endsWith("/**") ? posterUrlPrefix : posterUrlPrefix + "/**";

        registry.addResourceHandler(urlPattern)
                .addResourceLocations(diskPrefix)
                .setCachePeriod(60 * 60 * 24 * 7);
    }
}
