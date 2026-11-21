package com.omar.postify.security;

import com.omar.postify.dto.RequestIdentity;
import com.omar.postify.util.RequestIdentityResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RequestIdentityFilter extends OncePerRequestFilter {

    private final RequestIdentityResolver identityResolver;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        RequestIdentity identity =
                identityResolver.resolve(request, response);

        request.setAttribute("requestIdentity", identity);

        filterChain.doFilter(request, response);
    }
}