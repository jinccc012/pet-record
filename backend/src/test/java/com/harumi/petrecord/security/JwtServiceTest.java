package com.harumi.petrecord.security;

import com.harumi.petrecord.config.JwtProperties;
import com.harumi.petrecord.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private JwtProperties properties;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecret("test-secret-test-secret-test-secret-test-secret");
        properties.setExpirationMinutes(60);
        jwtService = new JwtService(properties);
    }

    @Test
    void issueAndParseRoundTrip() {
        CurrentUser user = new CurrentUser(42L, "harumi", UserRole.USER);

        String token = jwtService.issueToken(user, 7);
        JwtService.VerifiedToken parsed = jwtService.parseToken(token);

        assertThat(parsed.user()).isEqualTo(user);
        assertThat(parsed.tokenVersion()).isEqualTo(7);
    }

    @Test
    void parseRejectsTamperedToken() {
        CurrentUser user = new CurrentUser(1L, "harumi", UserRole.USER);
        String token = jwtService.issueToken(user, 0);
        String tampered = token.substring(0, token.length() - 2) + "AA";

        assertThatThrownBy(() -> jwtService.parseToken(tampered))
                .isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void parseRejectsTokenSignedWithDifferentKey() {
        CurrentUser user = new CurrentUser(1L, "harumi", UserRole.USER);
        String token = jwtService.issueToken(user, 0);

        JwtProperties otherProps = new JwtProperties();
        otherProps.setSecret("different-secret-different-secret-different-secret");
        otherProps.setExpirationMinutes(60);
        JwtService otherJwt = new JwtService(otherProps);

        assertThatThrownBy(() -> otherJwt.parseToken(token))
                .isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void parseRejectsGarbage() {
        assertThatThrownBy(() -> jwtService.parseToken("not-a-jwt"))
                .isInstanceOf(InvalidJwtException.class);
    }
}
