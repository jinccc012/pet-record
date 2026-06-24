package com.harumi.petrecord.auth;

import com.harumi.petrecord.auth.dto.AuthResponse;
import com.harumi.petrecord.auth.dto.LoginRequest;
import com.harumi.petrecord.auth.dto.RegisterRequest;
import com.harumi.petrecord.auth.dto.UserSummary;
import com.harumi.petrecord.common.exception.DuplicateResourceException;
import com.harumi.petrecord.security.CurrentUser;
import com.harumi.petrecord.security.JwtService;
import com.harumi.petrecord.user.User;
import com.harumi.petrecord.user.UserRepository;
import com.harumi.petrecord.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * A throwaway hash matched against when the email is unknown, so that a failed login takes the
     * same time whether or not the account exists. Prevents timing-based account enumeration.
     */
    private final String dummyHash;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already in use");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already in use");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.USER)
                .build();
        User saved = userRepository.save(user);
        log.info("Registered user id={}", saved.getId());
        return buildAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null) {
            // Run a matching pass against a dummy hash so the response time does not reveal
            // whether the email is registered, then fail with the same generic error.
            passwordEncoder.matches(request.password(), dummyHash);
            throw new BadCredentialsException("Invalid credentials");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        log.info("Login success user id={}", user.getId());
        return buildAuthResponse(user);
    }

    /**
     * Revokes every token currently held by the user by bumping their token version. Used for
     * server-side logout; the same mechanism applies to password changes or account disabling.
     */
    @Transactional
    public void logout(Long userId) {
        userRepository.incrementTokenVersion(userId);
        log.info("Revoked tokens for user id={}", userId);
    }

    private AuthResponse buildAuthResponse(User user) {
        CurrentUser principal = new CurrentUser(user.getId(), user.getUsername(), user.getRole());
        String token = jwtService.issueToken(principal, user.getTokenVersion());
        return AuthResponse.bearer(token, jwtService.getExpirationSeconds(), UserSummary.from(user));
    }
}
