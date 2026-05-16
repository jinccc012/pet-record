package com.harumi.petrecord.auth.dto;

import com.harumi.petrecord.user.User;
import com.harumi.petrecord.user.UserRole;

public record UserSummary(Long id, String username, String email, UserRole role) {

    public static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }
}
