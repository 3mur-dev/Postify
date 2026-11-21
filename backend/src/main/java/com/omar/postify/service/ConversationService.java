package com.omar.postify.service;

import com.omar.postify.entities.Conversation;
import com.omar.postify.entities.Message;
import com.omar.postify.entities.User;
import com.omar.postify.dto.ConversationSummaryResponse;
import com.omar.postify.exception.ConflictException;
import com.omar.postify.exception.ForbiddenException;
import com.omar.postify.exception.ResourceNotFoundException;
import com.omar.postify.repository.ConversationRepository;
import com.omar.postify.repository.MessageRepository;
import com.omar.postify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public Conversation getOrCreateConversation(String targetUserPublicId) {

        User user = currentUserService.getCurrentUser()
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        User targetUser = userRepository.findByPublicId(targetUserPublicId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Target user is not found")
                );

        if (user.getId().equals(targetUser.getId())) {
            throw new ConflictException("Cannot start a conversation with yourself");
        }

        return conversationRepository
                .findBetweenUsers(user.getId(), targetUser.getId())
                .orElseGet(() -> {
                    Conversation newConversation = new Conversation();
                    newConversation.setUserOne(user);
                    newConversation.setUserTwo(targetUser);

                    Conversation savedConversation =
                            conversationRepository.save(newConversation);

                    return savedConversation;
                });
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getConversations() {
        User currentUser = currentUserService.getCurrentUser()
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        List<Conversation> conversations =
                conversationRepository.findConversationsByUser(currentUser.getId());
        Map<Long, Message> latestMessages = conversations.isEmpty()
                ? Map.of()
                : messageRepository.findLatestByConversationIds(
                        conversations.stream().map(Conversation::getId).toList()
                ).stream().collect(Collectors.toMap(
                        message -> message.getConversation().getId(),
                        Function.identity()
                ));

        return conversations
                .stream()
                .map(conversation -> {
                    User participant = conversation.getUserOne().getId().equals(currentUser.getId())
                            ? conversation.getUserTwo()
                            : conversation.getUserOne();
                    Message latest = latestMessages.get(conversation.getId());
                    ConversationSummaryResponse.MessagePreview preview = latest == null
                            ? null
                            : new ConversationSummaryResponse.MessagePreview(
                                    latest.getContent(),
                                    latest.getSender().getUsername(),
                                    latest.getCreatedAt()
                            );

                    return new ConversationSummaryResponse(
                            conversation.getPublicId(),
                            new ConversationSummaryResponse.Participant(
                                    participant.getPublicId(),
                                    participant.getUsername(),
                                    participant.getAvatarUrl()
                            ),
                            preview,
                            conversation.getUpdatedAt()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public String getOtherParticipantUsername(String publicId, String currentUsername) {
        Conversation conversation = conversationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (conversation.getUserOne().getUsername().equals(currentUsername)) {
            return conversation.getUserTwo().getUsername();
        }

        if (conversation.getUserTwo().getUsername().equals(currentUsername)) {
            return conversation.getUserOne().getUsername();
        }

        throw new ForbiddenException("Current user is not part of the conversation");
    }

    @Transactional(readOnly = true)
    public String getOtherParticipantPublicId(String publicId, String currentUsername) {
        Conversation conversation = conversationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (conversation.getUserOne().getUsername().equals(currentUsername)) {
            return conversation.getUserTwo().getPublicId();
        }

        if (conversation.getUserTwo().getUsername().equals(currentUsername)) {
            return conversation.getUserOne().getPublicId();
        }

        throw new ForbiddenException("Current user is not part of the conversation");
    }

    @Transactional
    public void markConversationAsRead(String publicId) {

        User currentUser = currentUserService.getCurrentUser()
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Conversation conversation = conversationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.getUserOne().getId().equals(currentUser.getId()) &&
                !conversation.getUserTwo().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Current user is not part of the conversation");
        }

        int updatedMessages =
                messageRepository.markMessagesAsReadWhereNotReadAndReadAtIsNullAndCurrentUserNotEqualToSender(
                        conversation.getId(),
                        currentUser.getId(),
                        LocalDateTime.now()
                );

        log.info("Marked {} messages as read in conversation {}", updatedMessages, publicId);
    }
}
