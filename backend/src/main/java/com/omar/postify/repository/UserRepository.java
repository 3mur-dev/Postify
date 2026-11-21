package com.omar.postify.repository;

import com.omar.postify.dto.admin.AdminUserDto;
import com.omar.postify.entities.enums.Role;
import com.omar.postify.entities.User;
import com.omar.postify.entities.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByPublicId(String publicId);

    List<User> findByUsernameContainingIgnoreCase(String query);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    long countByStatus(UserStatus status);

    @Query("""
SELECT u.createdAt, COUNT(u)
FROM User u
WHERE u.createdAt BETWEEN :from AND :to
GROUP BY u.createdAt
ORDER BY u.createdAt
""")
    List<Object[]> countUsersGroupedByDay(LocalDateTime from, LocalDateTime to);

    @Query("""
    SELECT new com.omar.postify.dto.admin.AdminUserDto(
        u.publicId,
        u.username,
        u.email,
        u.status,
        u.createdAt
    )
    FROM User u
""")
    Page<AdminUserDto> findAllUsers(Pageable pageable);

    @Query("""
    SELECT new com.omar.postify.dto.admin.AdminUserDto(
        u.publicId,
        u.username,
        u.email,
        u.status,
        u.createdAt
    )
    FROM User u
    WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
       OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<AdminUserDto> searchUsers(
            @Param("search") String search,
            Pageable pageable
    );


    @Query("""
    SELECT COUNT(f)
    FROM Follow f
    WHERE f.following.publicId = :publicId
""")
    long countFollowers(String publicId);

    @Query("""
    SELECT COUNT(f)
    FROM Follow f
    WHERE f.follower.publicId = :publicId
""")
    long countFollowing(String publicId);

    @Query("""
SELECT COUNT(p)
FROM Post p
WHERE p.user.id = :userId
""")
    long countPosts(@Param("userId") Long userId);

    Optional<User> findByEmail(String email);
}
