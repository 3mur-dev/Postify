package com.omar.postify.service;

import com.omar.postify.entities.Conversation;
import com.omar.postify.entities.Message;
import com.omar.postify.entities.User;
import com.omar.postify.repository.ConversationRepository;
import com.omar.postify.repository.MessageRepository;
import com.omar.postify.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private ConversationService conversationService;

    @Test
    void marksMessagesAsReadUsingApplicationLocalTime() {
        User currentUser = user(1L, "current", "current-public-id");
        User participant = user(2L, "participant", "participant-public-id");
        Conversation conversation = new Conversation();
        conversation.setId(40L);
        conversation.setPublicId("conversation-public-id");
        conversation.setUserOne(currentUser);
        conversation.setUserTwo(participant);

        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(currentUser));
        when(conversationRepository.findByPublicId("conversation-public-id"))
                .thenReturn(Optional.of(conversation));

        LocalDateTime beforeMarkingRead = LocalDateTime.now();
        conversationService.markConversationAsRead("conversation-public-id");
        LocalDateTime afterMarkingRead = LocalDateTime.now();

        ArgumentCaptor<LocalDateTime> readAtCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);
        verify(messageRepository).markMessagesAsReadWhereNotReadAndReadAtIsNullAndCurrentUserNotEqualToSender(
                eq(40L),
                eq(1L),
                readAtCaptor.capture()
        );
        assertFalse(readAtCaptor.getValue().isBefore(beforeMarkingRead));
        assertFalse(readAtCaptor.getValue().isAfter(afterMarkingRead));
    }

    @Test
    void returnsConversationAndLatestPreviewWithoutInternalUserIds() {
        User currentUser = user(1L, "current", "current-public-id");
        User participant = user(2L, "participant", "participant-public-id");
        Conversation conversation = new Conversation();
        conversation.setId(40L);
        conversation.setPublicId("conversation-public-id");
        conversation.setUserOne(currentUser);
        conversation.setUserTwo(participant);
        conversation.setUpdatedAt(LocalDateTime.parse("2026-10-09T12:00:00"));

        Message latest = new Message();
        latest.setId(80L);
        latest.setConversation(conversation);
        latest.setSender(participant);
        latest.setContent("Latest message");
        latest.setCreatedAt(LocalDateTime.parse("2026-10-09T12:00:00"));

        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(currentUser));
        when(conversationRepository.findConversationsByUser(1L))
                .thenReturn(List.of(conversation));
        when(messageRepository.findLatestByConversationIds(List.of(40L)))
                .thenReturn(List.of(latest));

        var result = conversationService.getConversations();

        assertEquals(1, result.size());
        assertEquals("conversation-public-id", result.get(0).publicId());
        assertEquals("participant-public-id", result.get(0).participant().publicId());
        assertEquals("participant", result.get(0).participant().username());
        assertEquals("Latest message", result.get(0).latestMessage().content());
        assertEquals("participant", result.get(0).latestMessage().senderUsername());
        verify(messageRepository).findLatestByConversationIds(List.of(40L));
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
