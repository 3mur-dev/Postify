package com.omar.postify.rateLimit;

import com.omar.postify.entities.enums.RateLimitType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class RateLimitPolicyResolver {

    public RateLimitType resolve(HttpServletRequest request) {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if (path.equals("/api/auth/login") && method.equals("POST")) {
            return RateLimitType.LOGIN;
        }
        if (path.equals("/api/auth/register") && method.equals("POST")) {
            return RateLimitType.REGISTER;
        }
        return null;
    }
}