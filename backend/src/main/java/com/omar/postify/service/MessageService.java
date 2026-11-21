package com.omar.postify.service;

import com.omar.postify.dto.MessageResponse;
import com.omar.postify.dto.SendMessageRequest;
import com.omar.postify.entities.Conversation;
import com.omar.postify.entities.Message;
import com.omar.postify.entities.User;
import com.omar.postify.exception.ConflictException;
import com.omar.postify.exception.ForbiddenException;
import com.omar.postify.exception.ResourceNotFoundException;
import com.omar.postify.repository.ConversationRepository;
import com.omar.postify.repository.MessageRepository;
import com.omar.postify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final CurrentUserService currentUserService;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    @Transactional
    public MessageResponse sendMessage(
            String conversationPublicId,
            SendMessageRequest request,
            String senderUsername,
            String clientMessageId
    ) {
        Conversation conversation = conversationRepository.findByPublicId(conversationPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        User currentUser = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        Long currentUserId = currentUser.getId();

        if (!currentUserId.equals(conversation.getUserOne().getId()) && !currentUserId.equals(conversation.getUserTwo().getId())) {
            throw new ForbiddenException("Current user is not part of the conversation");
        }

        Message existing = messageRepository
                .findByConversation_IdAndSender_IdAndClientMessageId(
                        conversation.getId(),
                        currentUserId,
                        clientMessageId
                )
                .orElse(null);
        if (existing != null) {
            if (!existing.getContent().equals(request.content())) {
                throw new ConflictException("Client message ID was already used");
            }
            return toResponse(existing);
        }

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(currentUser);
        message.setContent(request.content());
        message.setClientMessageId(clientMessageId);
        Message savedMessage = messageRepository.save(message);

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        log.info("Message sent in conversation {} by user {}", conversation.getId(), currentUserId);

        return toResponse(savedMessage);
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(String conversationPublicId, Pageable pageable){

        User currentUser = currentUserService.getCurrentUser()
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Long currentUserId = currentUser.getId();

        Conversation conversation = conversationRepository.findByPublicId(conversationPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!currentUserId.equals(conversation.getUserOne().getId()) && !currentUserId.equals(conversation.getUserTwo().getId())) {
            throw new ForbiddenException("Current user is not part of the conversation");
        }

        Page<Message> messagesPage = messageRepository.findAllByConversationIdOrderByCreatedAtDesc(conversation.getId(), pageable);

        return messagesPage.map(this::toResponse);
    }

    private MessageResponse toResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getConversation().getPublicId(),
                message.getSender().getPublicId(),
                message.getSender().getUsername(),
                message.getSender().getAvatarUrl(),
                message.getContent(),
                message.getClientMessageId(),
                message.getCreatedAt(),
                message.isRead(),
                message.getReadAt()

        );
    }
}
