package com.omar.postify.rateLimit;

import com.omar.postify.entities.enums.RateLimitType;
import com.omar.postify.exception.RateLimitException;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitPolicyResolver policyResolver;
    private final RateLimiterService rateLimiterService;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            RateLimitType type = policyResolver.resolve(request);

            if (type == null) {
                filterChain.doFilter(request, response);
                return;
            }

            String clientKey = request.getRemoteAddr();

            Bucket bucket = rateLimiterService.resolveBucket(
                    clientKey,
                    type
            );

            if (!bucket.tryConsume(1)) {
                throw new RateLimitException("Too many requests. Please try again later.");
            }

            filterChain.doFilter(request, response);
        } catch (RateLimitException ex) {
            handlerExceptionResolver.resolveException(request, response, null, ex);
        }
    }
}
