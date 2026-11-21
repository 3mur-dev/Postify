package com.omar.postify.repository;

import com.omar.postify.entities.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("""

            SELECT c FROM Conversation c
            WHERE (c.userOne.id = :userOneId AND c.userTwo.id = :userTwoId)
               OR (c.userOne.id = :userTwoId AND c.userTwo.id = :userOneId)
           """)
    Optional<Conversation> findBetweenUsers(
            @Param("userOneId") Long userOneId,
            @Param("userTwoId") Long userTwoId
    );

    @Query("""
            SELECT c FROM Conversation c
            WHERE c.userOne.id = :userId OR c.userTwo.id = :userId
            ORDER by c.updatedAt DESC
           """)
    List<Conversation> findConversationsByUser(
            @Param("userId") Long userId
    );

    Optional<Conversation> findByPublicId(String publicId);
}
