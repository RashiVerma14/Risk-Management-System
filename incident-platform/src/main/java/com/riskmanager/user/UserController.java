package com.riskmanager.user;

import com.riskmanager.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for managing user accounts, roles and status")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users (Admin only)")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.ok("Users retrieved successfully", userService.getAllUsers()));
    }

    @GetMapping("/engineers")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'VIEWER')")
    @Operation(summary = "List all engineers available for incident assignment")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getEngineers() {
        return ResponseEntity.ok(ApiResponse.ok("Engineers retrieved successfully", userService.getEngineers()));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserResponseDto>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved successfully", userService.getUserByEmail(userDetails.getUsername())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user details by ID (Admin only)")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("User retrieved successfully", userService.getUserById(id)));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user role (Admin only)")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUserRole(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("User role updated successfully", userService.updateUserRole(id, request.role())));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate or deactivate a user account (Admin only)")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUserStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("User status updated successfully", userService.updateUserStatus(id, request.active())));
    }
}
