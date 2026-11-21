package com.omar.postify.repository;

import com.omar.postify.entities.Conversation;
import com.omar.postify.entities.Message;
import com.omar.postify.entities.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.globally_quoted_identifiers=true")
class MessageRepositoryTest {

    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void findsLatestMessageForEachConversationAndKeepsHistoryNewestFirst() {
        User sender = saveUser();
        User recipient = saveUser();
        Conversation conversation = new Conversation();
        conversation.setUserOne(sender);
        conversation.setUserTwo(recipient);
        conversation = conversationRepository.saveAndFlush(conversation);

        Message first = saveMessage(conversation, sender, "First");
        Message latest = saveMessage(conversation, recipient, "Latest");
        entityManager.clear();

        List<Message> latestMessages =
                messageRepository.findLatestByConversationIds(List.of(conversation.getId()));
        var history = messageRepository.findAllByConversationIdOrderByCreatedAtDesc(
                conversation.getId(),
                org.springframework.data.domain.PageRequest.of(0, 30)
        );

        assertEquals(1, latestMessages.size());
        assertEquals(latest.getId(), latestMessages.get(0).getId());
        assertEquals(latest.getId(), history.getContent().get(0).getId());
        assertTrue(first.getId() < latest.getId());
    }

    private User saveUser() {
        String suffix = UUID.randomUUID().toString();
        User user = new User();
        user.setUsername("dm-test-" + suffix);
        user.setEmail("dm-test-" + suffix + "@example.com");
        user.setPassword("test-password");
        return userRepository.saveAndFlush(user);
    }

    private Message saveMessage(
            Conversation conversation,
            User sender,
            String content
    ) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content);
        return messageRepository.saveAndFlush(message);
    }
}
