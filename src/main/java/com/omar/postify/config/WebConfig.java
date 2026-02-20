package com.omar.postify.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.springframework.http.CacheControl;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Path AVATAR_UPLOAD_DIR =
            Paths.get(System.getProperty("user.dir"), "uploads", "avatars");

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = AVATAR_UPLOAD_DIR.toUri().toString();
        registry.addResourceHandler("/images/avatars/**")
                .addResourceLocations(uploadPath)
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic());
    }
}
