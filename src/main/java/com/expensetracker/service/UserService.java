package com.expensetracker.service;

import com.expensetracker.model.User;
import com.expensetracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createOrUpdateUser(User user) {
        if (user.getCreatedTmstp() == null) {
            user.setCreatedTmstp(LocalDateTime.now());
        }
        return userRepository.save(user);
    }

    public Optional<User> findById(String userId) {
        return userRepository.findById(userId);
    }

    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User updatePasswordByUserId(String userId, String newPassword) {
        Optional<User> opt = userRepository.findById(userId);
        if (opt.isEmpty()) throw new IllegalArgumentException("user not found");
        User u = opt.get();
        u.setPassword(newPassword);
        return userRepository.save(u);
    }

    public User updatePasswordByUsername(String username, String newPassword) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) throw new IllegalArgumentException("user not found");
        User u = opt.get();
        u.setPassword(newPassword);
        return userRepository.save(u);
    }

    public User updatePasswordByEmail(String email, String newPassword) {
        Optional<User> opt = userRepository.findByEmail(email);
        if (opt.isEmpty()) throw new IllegalArgumentException("user not found");
        User u = opt.get();
        u.setPassword(newPassword);
        return userRepository.save(u);
    }
}
