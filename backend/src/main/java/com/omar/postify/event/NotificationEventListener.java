package com.omar.postify.event;

import com.omar.postify.entities.Comment;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.User;
import com.omar.postify.entities.enums.NotificationType;
import com.omar.postify.exception.ResourceNotFoundException;
import com.omar.postify.repository.CommentRepository;
import com.omar.postify.repository.PostRepository;
import com.omar.postify.repository.UserRepository;
import com.omar.postify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;

    /**
     * Runs AFTER the main transaction commits, so notification failures
     * never roll back the original like/comment/follow operation.
     * Each handler is wrapped in try-catch to prevent any exception from
     * propagating and disrupting the event publishing infrastructure.
     */

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostLiked(PostLikedEvent event) {
        try {
            User actor = userRepository.findById(event.actorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Actor not found"));

            Post post = postRepository.findById(event.postId())
                    .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

            User recipient = post.getUser();

            if (actor.getId().equals(recipient.getId())) {
                return;
            }

            notificationService.createNotification(
                    recipient,
                    actor,
                    NotificationType.LIKE,
                    post,
                    null,
                    actor.getUsername() + " liked your post"
            );
        } catch (Exception e) {
            log.error("Failed to create like notification for post={}, actor={}",
                    event.postId(), event.actorId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFollowed(UserFollowedEvent event) {
        try {
            User actor = userRepository.findById(event.followerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Actor not found"));

            User recipient = userRepository.findById(event.followingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));

            if (actor.getId().equals(recipient.getId())) {
                return;
            }

            notificationService.createNotification(
                    recipient,
                    actor,
                    NotificationType.FOLLOW,
                    null,
                    null,
                    actor.getUsername() + " started following you"
            );
        } catch (Exception e) {
            log.error("Failed to create follow notification for follower={}, following={}",
                    event.followerId(), event.followingId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentCreated(CommentCreatedEvent event) {
        try {
            User actor = userRepository.findById(event.actorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Actor not found"));

            Comment comment = commentRepository.findById(event.commentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

            User recipient = comment.getPost().getUser();

            if (actor.getId().equals(recipient.getId())) {
                return;
            }

            notificationService.createNotification(
                    recipient,
                    actor,
                    NotificationType.COMMENT,
                    comment.getPost(),
                    comment,
                    actor.getUsername() + " commented on your post"
            );
        } catch (Exception e) {
            log.error("Failed to create comment notification for comment={}, actor={}",
                    event.commentId(), event.actorId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSystemNotification(SystemNotificationEvent event) {
        try {
            User user = userRepository.findById(event.recipientId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            notificationService.createNotification(
                    user,
                    null,
                    NotificationType.SYSTEM,
                    null,
                    null,
                    event.message()
            );
        } catch (Exception e) {
            log.error("Failed to create system notification for user={}",
                    event.recipientId(), e);
        }
    }
}