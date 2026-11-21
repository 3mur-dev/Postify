package com.omar.postify.service;

import com.omar.postify.entities.AnonymousUser;
import com.omar.postify.repository.AnonymousUserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnonymousUserServiceTest {

    @Test
    void refreshesExistingIdentityExpiryAndLastSeenTime() {
        AnonymousUserRepository repository = mock(AnonymousUserRepository.class);
        AnonymousUserService service = new AnonymousUserService(repository);
        ReflectionTestUtils.setField(service, "isSecure", false);
        UUID publicId = UUID.randomUUID();
        AnonymousUser user = new AnonymousUser();
        user.setId(17L);
        user.setPublicId(publicId);
        when(repository.findByPublicId(publicId)).thenReturn(Optional.of(user));
        when(repository.updateLastSeenAt(any(), any())).thenReturn(1);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("POSTIFY_ANONYMOUS_ID", publicId.toString()));
        MockHttpServletResponse response = new MockHttpServletResponse();

        AnonymousUser result = service.getOrCreate(request, response);

        assertSame(user, result);
        assertEquals(2592000, response.getCookie("POSTIFY_ANONYMOUS_ID").getMaxAge());
        verify(repository).updateLastSeenAt(org.mockito.ArgumentMatchers.eq(17L), any());
    }
}
