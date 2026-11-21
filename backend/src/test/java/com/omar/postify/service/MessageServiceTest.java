package com.omar.postify.service;

import com.omar.postify.dto.SendMessageRequest;
import com.omar.postify.entities.Conversation;
import com.omar.postify.entities.Message;
import com.omar.postify.entities.User;
import com.omar.postify.exception.ConflictException;
import com.omar.postify.exception.ForbiddenException;
import com.omar.postify.repository.ConversationRepository;
import com.omar.postify.repository.MessageRepository;
import com.omar.postify.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    private static final String CONVERSATION_PUBLIC_ID = "conversation-public-id";
    private static final String CLIENT_MESSAGE_ID = "c8e4fd6a-8269-4f39-9258-2fca3ba9ff34";

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MessageService messageService;

    private Conversation conversation;
    private User sender;
    private User recipient;

    @BeforeEach
    void setUp() {
        sender = user(1L, "sender", "sender-public-id");
        recipient = user(2L, "recipient", "recipient-public-id");
        conversation = new Conversation();
        conversation.setId(10L);
        conversation.setPublicId(CONVERSATION_PUBLIC_ID);
        conversation.setUserOne(sender);
        conversation.setUserTwo(recipient);

        lenient().when(conversationRepository.findByPublicId(CONVERSATION_PUBLIC_ID))
                .thenReturn(Optional.of(conversation));
        lenient().when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        lenient().when(messageRepository.findByConversation_IdAndSender_IdAndClientMessageId(
                10L,
                1L,
                CLIENT_MESSAGE_ID
        )).thenReturn(Optional.empty());
    }

    @Test
    void persistsMessageWithCorrelationAndConversationIdentity() {
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message saved = invocation.getArgument(0);
            saved.setId(42L);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        var result = messageService.sendMessage(
                CONVERSATION_PUBLIC_ID,
                new SendMessageRequest("Hello"),
                "sender",
                CLIENT_MESSAGE_ID
        );

        assertEquals(42L, result.id());
        assertEquals(CONVERSATION_PUBLIC_ID, result.conversationPublicId());
        assertEquals("sender-public-id", result.senderPublicId());
        assertEquals(CLIENT_MESSAGE_ID, result.clientMessageId());
        assertFalse(result.isRead());
        assertNull(result.readAt());
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void includesReadReceiptInMessageHistoryResponse() {
        LocalDateTime readAt = LocalDateTime.now();
        Message readMessage = message("Hello");
        readMessage.setRead(true);
        readMessage.setReadAt(readAt);

        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(recipient));
        when(messageRepository.findAllByConversationIdOrderByCreatedAtDesc(
                10L,
                PageRequest.of(0, 30)
        )).thenReturn(new PageImpl<>(List.of(readMessage)));

        var result = messageService.getMessages(CONVERSATION_PUBLIC_ID, PageRequest.of(0, 30));

        assertTrue(result.getContent().get(0).isRead());
        assertEquals(readAt, result.getContent().get(0).readAt());
    }

    @Test
    void returnsExistingMessageForSafeRetry() {
        Message existing = message("Hello");
        when(messageRepository.findByConversation_IdAndSender_IdAndClientMessageId(
                10L,
                1L,
                CLIENT_MESSAGE_ID
        )).thenReturn(Optional.of(existing));

        var result = messageService.sendMessage(
                CONVERSATION_PUBLIC_ID,
                new SendMessageRequest("Hello"),
                "sender",
                CLIENT_MESSAGE_ID
        );

        assertEquals(existing.getId(), result.id());
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void rejectsReusingCorrelationIdWithDifferentContent() {
        when(messageRepository.findByConversation_IdAndSender_IdAndClientMessageId(
                10L,
                1L,
                CLIENT_MESSAGE_ID
        )).thenReturn(Optional.of(message("Original")));

        assertThrows(ConflictException.class, () -> messageService.sendMessage(
                CONVERSATION_PUBLIC_ID,
                new SendMessageRequest("Changed"),
                "sender",
                CLIENT_MESSAGE_ID
        ));
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void rejectsNonParticipantBeforeSaving() {
        User stranger = user(3L, "stranger", "stranger-public-id");
        when(userRepository.findByUsername("stranger")).thenReturn(Optional.of(stranger));

        assertThrows(ForbiddenException.class, () -> messageService.sendMessage(
                CONVERSATION_PUBLIC_ID,
                new SendMessageRequest("Hello"),
                "stranger",
                CLIENT_MESSAGE_ID
        ));
        verify(messageRepository, never()).save(any(Message.class));
    }

    private Message message(String content) {
        Message message = new Message();
        message.setId(42L);
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content);
        message.setClientMessageId(CLIENT_MESSAGE_ID);
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }

    private User user(Long id, String username, String publicId) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPublicId(publicId);
        user.setAvatarUrl(null);
        return user;
    }
}
