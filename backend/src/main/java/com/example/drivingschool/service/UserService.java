package com.example.drivingschool.service;

import com.example.drivingschool.dto.LoginRequest;
import com.example.drivingschool.dto.RegisterRequest;
import com.example.drivingschool.dto.UserResponseDto;
import com.example.drivingschool.exception.ResourceNotFoundException;
import com.example.drivingschool.model.Role;
import com.example.drivingschool.model.User;
import com.example.drivingschool.repository.UserRepository;
import com.example.drivingschool.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // ✅ Register a new user
    public String registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role: " + request.getRole());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .contact(request.getContact())
                .licenseNumber(request.getLicenseNumber())
                .role(role)
                .access(role == Role.ADMIN) // auto-approve admins
                .deleted(false)
                .build();

        userRepository.save(user);
        return "Registration successful" + (role == Role.USER ? ". Wait for admin approval." : "");
    }

    // ✅ Login
    public String loginUser(LoginRequest request) {
        User user = userRepository.findByUsernameAndDeletedFalse(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid username"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        if (!user.isAccess()) {
            throw new RuntimeException("User is not approved by admin yet");
        }

        return jwtUtil.generateToken(user);
    }

    // ✅ Approve user
    public String approveUserByUsername(String username) {
        User user = userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setAccess(true);
        userRepository.save(user);
        return "User approved successfully";
    }

    // ✅ Soft delete user
    public String softDeleteUser(String username) {
        User user = userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setDeleted(true);
        user.setAccess(false);
        userRepository.save(user);
        return "User soft deleted successfully";
    }

    // ✅ Hard delete user
    public String hardDeleteUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userRepository.delete(user);
        return "User permanently deleted successfully";
    }

    // ✅ Restore deleted user
    public String restoreUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.isDeleted()) {
            return "User is not deleted";
        }
        user.setDeleted(false);
        userRepository.save(user);
        return "User restored successfully";
    }

    // ✅ Update user info
    public String updateUser(String username, RegisterRequest request) {
        User user = userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        if (request.getContact() != null && !request.getContact().isBlank()) {
            user.setContact(request.getContact());
        }
        if (request.getLicenseNumber() != null && !request.getLicenseNumber().isBlank()) {
            user.setLicenseNumber(request.getLicenseNumber());
        }

        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                user.setRole(Role.valueOf(request.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid role: " + request.getRole());
            }
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        userRepository.save(user);
        return "User details updated successfully";
    }

    // ✅ Get all users (for Admin)
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserResponseDto(
                        user.getId(),
                        user.getName(),
                        user.getUsername(),
                        user.getContact(),
                        user.getRole().name(),
                        user.getLicenseNumber(),
                        user.isAccess(),
                        user.isDeleted()
                ))
                .collect(Collectors.toList());
    }
}
