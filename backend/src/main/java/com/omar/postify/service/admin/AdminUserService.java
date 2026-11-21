package com.omar.postify.service.admin;

import com.omar.postify.dto.AdminContext;
import com.omar.postify.dto.admin.AdminUserDetailsDto;
import com.omar.postify.dto.admin.AdminUserDto;
import com.omar.postify.dto.enums.UserSortField;
import com.omar.postify.entities.User;
import com.omar.postify.entities.enums.UserStatus;
import com.omar.postify.exception.ResourceNotFoundException;
import com.omar.postify.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    public Page<AdminUserDto> getAllUsers(
            String search,
            int page,
            int size,
            UserSortField sortField,
            Sort.Direction direction
    ) {

        String sortProperty = switch (sortField) {
            case CREATED_AT -> "createdAt";
            case USERNAME -> "username";
            case EMAIL -> "email";
            case STATUS -> "status";
        };

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(direction, sortProperty)
        );

        if (search == null || search.isBlank()) {
            return userRepository.findAllUsers(pageable);
        }

        return userRepository.searchUsers(search.trim(), pageable);

    }

    public AdminUserDetailsDto getUserByPublicId(String publicId) {

        return userRepository.findByPublicId(publicId)
                .map(this::mapToDetailsDto)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public AdminUserDto updateUserStatus(String publicId, UserStatus status, AdminContext adminContext) {

        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() == status) {
            return mapToDto(user);
        }
        user.setStatus(status);

        return mapToDto(user);
    }

    private AdminUserDto mapToDto(User user) {
        return new AdminUserDto(
                user.getPublicId(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }

    private AdminUserDetailsDto mapToDetailsDto(User user) {

        long followersCount = userRepository.countFollowers(user.getPublicId());
        long followingCount = userRepository.countFollowing(user.getPublicId());
        long postCount = userRepository.countPosts(user.getId());

        return new AdminUserDetailsDto(
                user.getPublicId(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getBio(),
                user.getAvatarUrl(),
                followersCount,
                followingCount,
                postCount,
                user.getRole()

        );
    }
}