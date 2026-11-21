package com.omar.postify.service;

import com.omar.postify.dto.NotificationResponse;
import com.omar.postify.entities.Comment;
import com.omar.postify.entities.Notification;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.User;
import com.omar.postify.entities.enums.NotificationType;
import com.omar.postify.exception.ResourceNotFoundException;
import com.omar.postify.exception.UnauthorizedActionException;
import com.omar.postify.mapper.NotificationMapper;
import com.omar.postify.repository.NotificationRepository;
import com.omar.postify.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import com.omar.postify.dto.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final NotificationMapper notificationMapper;
    private final CurrentUserService currentUserService;

    @jakarta.transaction.Transactional
    public void createNotification(
            User recipient,
            User actor,
            NotificationType type,
            Post post,
            Comment comment,
            String message
    ) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(type)
                .post(post)
                .comment(comment)
                .message(message)
                .build();

        Notification saved = notificationRepository.save(notification);

         simpMessagingTemplate.convertAndSendToUser(
                recipient.getUsername(),
                "/queue/notifications",
                notificationMapper.toResponse(saved)
         );
    }

    public void markAsRead(String publicId) {

        User currentUser = currentUserService.getCurrentUser()
                .orElseThrow(() ->
                        new UnauthorizedActionException("User is not authenticated"));

        Notification notification = notificationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getRecipient().getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException(
                    "You cannot modify this notification"
            );
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }
    @Transactional
    public PagedResponse<NotificationResponse> getNotifications(int page, int size) {

        User currentUser = currentUserService.getCurrentUser()
                .orElseThrow(() ->
                        new UnauthorizedActionException("User is not authenticated"));

        int maxSize = Math.min(size, 50);
        Pageable pageable = PageRequest.of(page, maxSize, Sort.by("createdAt").descending());

        Page<Notification> notificationPage = notificationRepository
                .findByRecipientOrderByCreatedAtDesc(currentUser, pageable);

        return PagedResponse.from(
                notificationPage.map(notificationMapper::toResponse)
        );
    }
}
