package com.omar.postify.security;

import com.omar.postify.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();

        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   RequestMappingHandlerMapping requestMappingHandlerMapping
    ) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            try {
                                if (requestMappingHandlerMapping.getHandler(request) == null) {
                                    writeError(
                                            response,
                                            "/problems/not-found",
                                            HttpStatus.NOT_FOUND,
                                            "Not Found",
                                            "Endpoint not found"
                                    );
                                    return;
                                }
                            } catch (Exception ex) {
                                log.error("Failed to resolve request handler", ex);
                            }

                            writeError(
                                    response,
                                    "/problems/unauthorized",
                                    HttpStatus.UNAUTHORIZED,
                                    "Unauthorized",
                                    "Authentication required"
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(response, "/problems/forbidden", HttpStatus.FORBIDDEN, "Forbidden", "You do not have permission to access this resource"))
                )
                .authorizeHttpRequests(auth -> auth

                        // Authentication
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/register"
                        ).permitAll()

                        // Following is required to view another user's follow lists.
                        .requestMatchers(HttpMethod.GET,
                                "/api/profile/*/followers",
                                "/api/profile/*/following"
                        ).authenticated()

                        // Public profile
                        .requestMatchers(HttpMethod.GET,
                                "/api/profile/{username}"

                        ).permitAll()

                        .requestMatchers(HttpMethod.GET,
                                "/api/profile/*"
                        ).permitAll()

                        // Authenticated profile
                        .requestMatchers("/api/profile/me/**")
                        .authenticated()

                        .requestMatchers(HttpMethod.GET, "/api/post-media/**")
                        .permitAll()

                        .requestMatchers(HttpMethod.PUT,
                                "/api/profile/me"
                        ).authenticated()

                        // Feed
                        .requestMatchers("/api/interactions/**")
                        .permitAll()

                        .requestMatchers("/api/feed/**")
                        .permitAll()

                        // Posts
                        .requestMatchers(HttpMethod.GET, "/api/posts/{postId}")
                        .permitAll()

                        .requestMatchers("/api/posts/{postId}/comments")
                        .permitAll()

                        .requestMatchers("/api/posts/**")
                        .authenticated()

                        // Follow
                        .requestMatchers("/api/follow/**")
                        .authenticated()

                        // Search
                        .requestMatchers("/api/search/**")
                        .permitAll()

                        // Public profile avatars only. Post media is served by an
                        // authorization-aware endpoint that checks post status.
                        .requestMatchers(
                                "/images/avatars/**"
                        ).permitAll()
                        // WebSocket
                        .requestMatchers("/ws/**")
                        .permitAll()

                        // Admin
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/actuator/health")
                        .permitAll()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeError(HttpServletResponse response, String type, HttpStatus status, String title, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {
                  "type":"%s",
                  "title":"%s",
                  "status":%d,
                  "detail":"%s"
                }
                """.formatted(
                escapeJson(type),
                escapeJson(title),
                status.value(),
                escapeJson(detail)
        ));
    }

    private String escapeJson(String value) {
        return value == null
                ? ""
                : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
