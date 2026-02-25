package com.omar.postify.service;

import com.omar.postify.entities.Role;
import com.omar.postify.entities.User;
import com.omar.postify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public long getUserCount() {
        return userRepository.count();
    }

    public long getAdminCount() {
        return userRepository.countByRole(Role.ADMIN);
    }

    public Iterable<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void promoteToAdmin(Long id) {

        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(Role.ADMIN);

        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}