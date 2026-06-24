package com.harumi.petrecord.auth;

import com.harumi.petrecord.auth.dto.AuthResponse;
import com.harumi.petrecord.auth.dto.LoginRequest;
import com.harumi.petrecord.auth.dto.RegisterRequest;
import com.harumi.petrecord.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(AuthService authService, LoginRateLimiter loginRateLimiter) {
        this.authService = authService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        String ip = clientIp(httpRequest);
        loginRateLimiter.checkAllowed(ip, request.email());
        try {
            AuthResponse response = authService.login(request);
            loginRateLimiter.reset(ip, request.email());
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException ex) {
            loginRateLimiter.recordFailure(ip, request.email());
            throw ex;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CurrentUser currentUser) {
        // Revokes all of this user's tokens (every device/session) by bumping their token version.
        authService.logout(currentUser.id());
        return ResponseEntity.noContent().build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            // X-Forwarded-For may be a comma-separated list; the first entry is the originating client.
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
