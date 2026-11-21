package com.omar.postify.util;

import com.omar.postify.dto.AdminContext;
import com.omar.postify.entities.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminContextProvider {

    private final HttpServletRequest request;

    public AdminContext getCurrentContext() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        CustomUserDetails admin =
                (CustomUserDetails) authentication.getPrincipal();

        return new AdminContext(
                admin.getId(),
                admin.getUsername(),
                getClientIp(request),
                request.getHeader("User-Agent")
        );
    }

    private String getClientIp(HttpServletRequest httpServletRequest) {
        if (httpServletRequest == null) {
            return "unknown";
        }

        String xfHeader = httpServletRequest.getHeader("X-Forwarded-For");

        if (xfHeader == null || xfHeader.isBlank()) {
            return httpServletRequest.getRemoteAddr();
        }

        return xfHeader.split(",")[0];
    }
}