package com.example.drivingschool.controller;

import com.example.drivingschool.dto.*;
import com.example.drivingschool.model.User;
import com.example.drivingschool.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // ✅ Register new user (USER or ADMIN)
    @PostMapping("/register")
    public ApiResponse<String> register(@RequestBody RegisterRequest request) {
        String message = userService.registerUser(request);
        return new ApiResponse<>(true, message, null);
    }

    // ✅ Login and get JWT token
    @PostMapping("/login")
    public ApiResponse<String> login(@RequestBody LoginRequest request) {
        String token = userService.loginUser(request);
        return new ApiResponse<>(true, "Login successful", token);
    }

    // ✅ Approve user (ADMIN only)
    @PostMapping("/approve/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> approveUser(@PathVariable String username) {
        String message = userService.approveUserByUsername(username);
        return new ApiResponse<>(true, message, null);
    }

    // ✅ Soft delete (ADMIN only)
    @DeleteMapping("/soft-delete/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> softDelete(@PathVariable String username) {
        String message = userService.softDeleteUser(username);
        return new ApiResponse<>(true, message, null);
    }

    // ✅ Hard delete (ADMIN only)
    @DeleteMapping("/hard-delete/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> hardDelete(@PathVariable String username) {
        String message = userService.hardDeleteUser(username);
        return new ApiResponse<>(true, message, null);
    }

    // ✅ Restore deleted user (ADMIN only)
    @PostMapping("/restore/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> restoreUser(@PathVariable String username) {
        String message = userService.restoreUser(username);
        return new ApiResponse<>(true, message, null);
    }

    // ✅ Fetch all users (ADMIN only)
    @GetMapping("/users")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER', 'USER')")
    public ApiResponse<List<UserResponseDto>> getAllUsers() {
        List<UserResponseDto> users = userService.getAllUsers();
        return new ApiResponse<>(true, "All users fetched successfully", users);
    }

    // ✅ Edit user details (ADMIN only)
    @PutMapping("/edit/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> editUser(@PathVariable String username, @RequestBody RegisterRequest request) {
        String message = userService.updateUser(username, request);
        return new ApiResponse<>(true, message, null);
    }
}
