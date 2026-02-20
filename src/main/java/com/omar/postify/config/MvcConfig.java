package com.omar.postify.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    private static final Path AVATAR_UPLOAD_DIR =
            Paths.get(System.getProperty("user.dir"), "uploads", "avatars");
    private static final Path POST_UPLOAD_DIR =
            Paths.get(System.getProperty("user.dir"), "uploads", "posts");

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String resourceLocation = AVATAR_UPLOAD_DIR.toUri().toString();
        registry
                .addResourceHandler("/images/avatars/**")
                .addResourceLocations(resourceLocation);

        String postResourceLocation = POST_UPLOAD_DIR.toUri().toString();
        registry
                .addResourceHandler("/images/posts/**")
                .addResourceLocations(postResourceLocation);
    }
}
