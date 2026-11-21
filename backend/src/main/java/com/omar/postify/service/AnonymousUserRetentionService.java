package com.omar.postify.service;

import com.omar.postify.entities.AnonymousUser;
import com.omar.postify.repository.AnonymousUserRepository;
import com.omar.postify.repository.AnonymousUserTopicFeatureRepository;
import com.omar.postify.repository.InteractionEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnonymousUserRetentionService {

    private static final int BATCH_SIZE = 1000;

    private final AnonymousUserRepository anonymousUserRepository;
    private final InteractionEventRepository interactionEventRepository;
    private final AnonymousUserTopicFeatureRepository topicFeatureRepository;

    @Value("${postify.anonymous.retention-days:30}")
    private int retentionDays;

    @Scheduled(cron = "${postify.anonymous.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void purgeInactiveUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        List<AnonymousUser> expiredUsers = anonymousUserRepository.lockExpiredUsers(
                cutoff,
                PageRequest.of(0, BATCH_SIZE)
        );
        if (expiredUsers.isEmpty()) {
            return;
        }

        List<Long> expiredIds = expiredUsers.stream()
                .map(AnonymousUser::getId)
                .toList();

        interactionEventRepository.deleteByAnonymousUserIds(expiredIds);
        topicFeatureRepository.deleteByAnonymousUserIds(expiredIds);
        anonymousUserRepository.deleteAllInBatch(expiredUsers);

        log.info("Purged {} inactive anonymous identities and their data", expiredIds.size());
    }
}
