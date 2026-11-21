package com.omar.postify.util;

import com.omar.postify.dto.RequestIdentity;
import com.omar.postify.entities.AnonymousUser;
import com.omar.postify.entities.CustomUserDetails;
import com.omar.postify.service.AnonymousUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestIdentityResolver {

    public static final String REQUEST_ATTRIBUTE = "requestIdentity";

    private final AnonymousUserService anonymousUserService;

    public RequestIdentity resolve(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Object existingIdentity = request.getAttribute(REQUEST_ATTRIBUTE);
        if (existingIdentity instanceof RequestIdentity identity) {
            return identity;
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {

            RequestIdentity identity = RequestIdentity.authenticated(userDetails.getId());
            request.setAttribute(REQUEST_ATTRIBUTE, identity);
            return identity;
        }

        AnonymousUser anonymousUser =
                anonymousUserService.getOrCreate(request, response);

        RequestIdentity identity = RequestIdentity.anonymous(anonymousUser.getId());
        request.setAttribute(REQUEST_ATTRIBUTE, identity);
        return identity;
    }
}
