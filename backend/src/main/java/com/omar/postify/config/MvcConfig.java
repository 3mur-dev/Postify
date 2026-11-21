package com.omar.postify.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Value("${app.storage.base-path}")
    private String storageBasePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        Path avatarUploadDir = Paths
                .get(storageBasePath, "avatars")
                .toAbsolutePath()
                .normalize();

        Path postUploadDir = Paths
                .get(storageBasePath, "posts")
                .toAbsolutePath()
                .normalize();

        registry.addResourceHandler("/images/avatars/**")
                .addResourceLocations(avatarUploadDir.toUri().toString())
                .setCacheControl(
                        CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic()
                );

        registry.addResourceHandler("/images/posts/**")
                .addResourceLocations(postUploadDir.toUri().toString())
                .setCacheControl(
                        CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic()
                );
    }
}
