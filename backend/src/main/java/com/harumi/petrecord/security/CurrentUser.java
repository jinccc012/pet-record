package com.harumi.petrecord.security;

import com.harumi.petrecord.user.UserRole;

public record CurrentUser(Long id, String username, UserRole role) {
}
