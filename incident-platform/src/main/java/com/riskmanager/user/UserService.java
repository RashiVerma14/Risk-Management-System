package com.riskmanager.user;

import com.riskmanager.exception.BadRequestException;
import com.riskmanager.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponseDto::from)
                .toList();
    }

    public UserResponseDto getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponseDto.from(user);
    }

    public UserResponseDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return UserResponseDto.from(user);
    }

    public UserResponseDto updateUserRole(String id, Role newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setRole(newRole);
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        log.info("Updated role for user {} to {}", user.getEmail(), newRole);
        return UserResponseDto.from(saved);
    }

    public UserResponseDto updateUserStatus(String id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setActive(active);
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        log.info("Updated active status for user {} to {}", user.getEmail(), active);
        return UserResponseDto.from(saved);
    }

    public List<UserResponseDto> getEngineers() {
        return userRepository.findByRole(Role.ENGINEER).stream()
                .map(UserResponseDto::from)
                .toList();
    }
}
