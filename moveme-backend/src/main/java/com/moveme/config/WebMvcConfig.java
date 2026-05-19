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

    /** 头像本地目录 */
    @Value("${moveme.avatar.dir:./data/avatars}")
    private String avatarDir;

    /** 头像对外 URL 前缀 */
    @Value("${moveme.avatar.url-prefix:/static/avatars}")
    private String avatarUrlPrefix;

    /** 用户头像本地目录 */
    @Value("${moveme.avatar.users-dir:./data/avatars/users}")
    private String avatarUsersDir;

    /** 用户头像对外 URL 前缀 */
    @Value("${moveme.avatar.users-url-prefix:/static/avatars/users}")
    private String avatarUsersUrlPrefix;

    /** 影人头像本地目录 */
    @Value("${moveme.avatar.persons-dir:./data/avatars/persons}")
    private String avatarPersonsDir;

    /** 影人头像对外 URL 前缀 */
    @Value("${moveme.avatar.persons-url-prefix:/static/avatars/persons}")
    private String avatarPersonsUrlPrefix;

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
        // 海报
        String posterDisk = "file:" + posterDir.replace('\\', '/');
        if (!posterDisk.endsWith("/")) posterDisk += "/";
        String posterPattern = posterUrlPrefix.endsWith("/**") ? posterUrlPrefix : posterUrlPrefix + "/**";
        registry.addResourceHandler(posterPattern)
                .addResourceLocations(posterDisk)
                .setCachePeriod(60 * 60 * 24 * 7);

        // 头像 - 用户
        String avatarUsersDisk = "file:" + avatarUsersDir.replace('\\', '/');
        if (!avatarUsersDisk.endsWith("/")) avatarUsersDisk += "/";
        String avatarUsersPattern = avatarUsersUrlPrefix.endsWith("/**") ? avatarUsersUrlPrefix : avatarUsersUrlPrefix + "/**";
        registry.addResourceHandler(avatarUsersPattern)
                .addResourceLocations(avatarUsersDisk)
                .setCachePeriod(60 * 60 * 24 * 7);

        // 头像 - 影人
        String avatarPersonsDisk = "file:" + avatarPersonsDir.replace('\\', '/');
        if (!avatarPersonsDisk.endsWith("/")) avatarPersonsDisk += "/";
        String avatarPersonsPattern = avatarPersonsUrlPrefix.endsWith("/**") ? avatarPersonsUrlPrefix : avatarPersonsUrlPrefix + "/**";
        registry.addResourceHandler(avatarPersonsPattern)
                .addResourceLocations(avatarPersonsDisk)
                .setCachePeriod(60 * 60 * 24 * 7);
    }
}
