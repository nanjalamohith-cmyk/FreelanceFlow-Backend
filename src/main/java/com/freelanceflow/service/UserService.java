package com.freelanceflow.service;

import com.freelanceflow.dto.UpdateProfileRequest;
import com.freelanceflow.dto.UserSummaryResponse;
import com.freelanceflow.exception.ResourceNotFoundException;
import com.freelanceflow.model.User;
import com.freelanceflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserSummaryResponse getProfile(String userId) {
        User user = getUser(userId);
        return UserSummaryResponse.fromUser(user);
    }

    public UserSummaryResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = getUser(userId);

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getSkills() != null) {
            user.setSkills(request.getSkills());
        }
        if (request.getPortfolioLinks() != null) {
            user.setPortfolioLinks(request.getPortfolioLinks());
        }
        if (request.getCompanyName() != null) {
            user.setCompanyName(request.getCompanyName());
        }

        User updated = userRepository.save(user);
        return UserSummaryResponse.fromUser(updated);
    }

    public User getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    public List<UserSummaryResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserSummaryResponse::fromUser)
                .toList();
    }
}
