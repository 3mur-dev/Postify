package com.omar.postify.service;

import com.omar.postify.entities.AnonymousUser;
import com.omar.postify.repository.AnonymousUserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnonymousUserService {

    @Value("${postify.cookie.secure}")
    private Boolean isSecure;

    private static final String ANONYMOUS_COOKIE = "POSTIFY_ANONYMOUS_ID";

    private final AnonymousUserRepository anonymousUserRepository;

    @Transactional
    public AnonymousUser getOrCreate(HttpServletRequest request,
                                     HttpServletResponse response) {

        if(request.getCookies() == null){
            return createAndSaveAnonymousUser(response);
        }

        Cookie anonymousCookie = Arrays.stream(request.getCookies())
                .filter(cookie -> ANONYMOUS_COOKIE.equals(
                        cookie.getName()
                        )
                )
                .findFirst()
                .orElse(null);

        if (anonymousCookie != null) {

            try {

                UUID anonymousId = UUID.fromString(anonymousCookie.getValue());

                Optional<AnonymousUser> result =
                        anonymousUserRepository.findByPublicId(anonymousId);

                if (result.isPresent()) {
                    AnonymousUser anonymousUser = result.get();
                    LocalDateTime lastSeenAt = LocalDateTime.now();
                    if (anonymousUserRepository.updateLastSeenAt(
                            anonymousUser.getId(),
                            lastSeenAt
                    ) == 1) {
                        anonymousUser.setLastSeenAt(lastSeenAt);
                        addCookie(response, anonymousUser.getPublicId());
                        return anonymousUser;
                    }
                }

                return createAndSaveAnonymousUser(response);

            } catch (IllegalArgumentException e) {

                return createAndSaveAnonymousUser(response);

            }
        }

        return createAndSaveAnonymousUser(response);
    }

    private AnonymousUser createAndSaveAnonymousUser(HttpServletResponse response) {

        AnonymousUser anonymousUser = AnonymousUser.builder()
                .lastSeenAt(LocalDateTime.now())
                .build();
        anonymousUserRepository.save(anonymousUser);

        addCookie(response, anonymousUser.getPublicId());
        return anonymousUser;
    }

    private void addCookie(HttpServletResponse response, UUID publicId) {
        Cookie cookie = new Cookie(
                ANONYMOUS_COOKIE,
                publicId.toString()
        );

        cookie.setHttpOnly(true);
        cookie.setSecure(isSecure);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24 * 30);

        response.addCookie(cookie);
    }
}
