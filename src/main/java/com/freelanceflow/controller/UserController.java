package com.freelanceflow.controller;

import com.freelanceflow.dto.UpdateProfileRequest;
import com.freelanceflow.dto.UserSummaryResponse;
import com.freelanceflow.security.UserPrincipal;
import com.freelanceflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profiles, freelancer portfolio, and client information")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @Operation(summary = "Get logged in user profile")
    public ResponseEntity<UserSummaryResponse> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getProfile(principal.getId()));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update logged in user profile")
    public ResponseEntity<UserSummaryResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(principal.getId(), request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user public profile by ID")
    public ResponseEntity<UserSummaryResponse> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getProfile(id));
    }
}
