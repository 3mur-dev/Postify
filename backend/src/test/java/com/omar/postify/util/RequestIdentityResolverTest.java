package com.omar.postify.util;

import com.omar.postify.dto.RequestIdentity;
import com.omar.postify.entities.AnonymousUser;
import com.omar.postify.service.AnonymousUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestIdentityResolverTest {

    @Mock
    private AnonymousUserService anonymousUserService;

    @Test
    void reusesTheSameAnonymousIdentityForTheRequest() {
        RequestIdentityResolver resolver = new RequestIdentityResolver(anonymousUserService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AnonymousUser anonymousUser = new AnonymousUser();
        anonymousUser.setId(42L);
        when(anonymousUserService.getOrCreate(request, response)).thenReturn(anonymousUser);

        RequestIdentity first = resolver.resolve(request, response);
        RequestIdentity second = resolver.resolve(request, response);

        assertSame(first, second);
        verify(anonymousUserService, times(1)).getOrCreate(request, response);
    }
}
