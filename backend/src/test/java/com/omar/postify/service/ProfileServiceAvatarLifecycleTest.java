package com.omar.postify.service;

import com.omar.postify.entities.User;
import com.omar.postify.repository.FollowRepository;
import com.omar.postify.repository.PostMediaRepository;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.UserRepository;
import com.omar.postify.service.storage.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceAvatarLifecycleTest {

    @Mock private UserRepository userRepository;
    @Mock private PostRepository postRepository;
    @Mock private FollowRepository followRepository;
    @Mock private com.omar.postify.mapper.UserMapper userMapper;
    @Mock private com.omar.postify.mapper.FollowMapper followMapper;
    @Mock private FileStorageService fileStorageService;
    @Mock private com.omar.postify.repository.PostMediaRepository postMediaRepository;
    @Mock private com.omar.postify.mapper.PostMediaMapper postMediaMapper;

    @InjectMocks
    private com.omar.postify.service.ProfileService profileService;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void updatesAvatarAndDeletesOldOneAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();

        User user = User.builder().id(1L).username("member").avatarUrl("/old-avatar.jpg").build();
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(fileStorageService.storeAvatar(any()))
                .thenReturn("https://cloudinary.com/new-avatar.jpg");

        MockMultipartFile avatar = new MockMultipartFile(
                "avatar", "new-avatar.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        profileService.updateProfile(user, "new bio", avatar);

        verify(userRepository).save(user);
        assertEquals("https://cloudinary.com/new-avatar.jpg", user.getAvatarUrl());
        assertEquals("new bio", user.getBio());

        // Trigger afterCommit to verify old avatar deletion
        TransactionSynchronizationUtils.triggerAfterCompletion(
                TransactionSynchronization.STATUS_COMMITTED
        );
        verify(fileStorageService).deleteAvatarIfExists("/old-avatar.jpg");
    }

    @Test
    void deletesNewAvatarOnRollback() {
        TransactionSynchronizationManager.initSynchronization();

        User user = User.builder().id(1L).username("member").avatarUrl(null).build();
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(fileStorageService.storeAvatar(any()))
                .thenReturn("https://cloudinary.com/new-avatar.jpg");

        MockMultipartFile avatar = new MockMultipartFile(
                "avatar", "new-avatar.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        profileService.updateProfile(user, "new bio", avatar);

        TransactionSynchronizationUtils.triggerAfterCompletion(
                TransactionSynchronization.STATUS_ROLLED_BACK
        );

        verify(fileStorageService).deleteAvatarIfExists("https://cloudinary.com/new-avatar.jpg");
    }
}
