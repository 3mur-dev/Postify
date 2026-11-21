package com.omar.postify.service;

import com.omar.postify.entities.AnonymousUser;
import com.omar.postify.repository.AnonymousUserRepository;
import com.omar.postify.repository.AnonymousUserTopicFeatureRepository;
import com.omar.postify.repository.InteractionEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class AnonymousUserRetentionServiceTest {

    @Mock private AnonymousUserRepository anonymousUserRepository;
    @Mock private InteractionEventRepository interactionEventRepository;
    @Mock private AnonymousUserTopicFeatureRepository topicFeatureRepository;

    @InjectMocks
    private AnonymousUserRetentionService retentionService;

    @Test
    void deletesExpiredIdentitiesAndTheirDependentData() {
        ReflectionTestUtils.setField(retentionService, "retentionDays", 30);
        AnonymousUser expiredUser = new AnonymousUser();
        expiredUser.setId(17L);
        when(anonymousUserRepository.lockExpiredUsers(any(), any()))
                .thenReturn(List.of(expiredUser));

        retentionService.purgeInactiveUsers();

        ArgumentCaptor<LocalDateTime> cutoffCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);
        verify(anonymousUserRepository).lockExpiredUsers(cutoffCaptor.capture(), any());
        assertTrue(cutoffCaptor.getValue().isBefore(LocalDateTime.now().minusDays(29)));
        verify(interactionEventRepository).deleteByAnonymousUserIds(List.of(17L));
        verify(topicFeatureRepository).deleteByAnonymousUserIds(List.of(17L));
        verify(anonymousUserRepository).deleteAllInBatch(List.of(expiredUser));
    }
}
