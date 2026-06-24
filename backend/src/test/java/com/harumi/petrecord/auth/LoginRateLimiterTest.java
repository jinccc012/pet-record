package com.harumi.petrecord.auth;

import com.harumi.petrecord.common.exception.TooManyAttemptsException;
import com.harumi.petrecord.config.AuthRateLimitProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimiterTest {

    private LoginRateLimiter limiterWith(int maxAttempts, long windowMinutes) {
        AuthRateLimitProperties props = new AuthRateLimitProperties();
        props.setMaxAttempts(maxAttempts);
        props.setWindowMinutes(windowMinutes);
        return new LoginRateLimiter(props);
    }

    @Test
    void blocksAfterReachingMaxFailures() {
        LoginRateLimiter limiter = limiterWith(3, 15);

        for (int i = 0; i < 3; i++) {
            limiter.checkAllowed("1.1.1.1", "user@example.com");
            limiter.recordFailure("1.1.1.1", "user@example.com");
        }

        assertThatThrownBy(() -> limiter.checkAllowed("1.1.1.1", "user@example.com"))
                .isInstanceOf(TooManyAttemptsException.class);
    }

    @Test
    void successfulLoginResetsCounter() {
        LoginRateLimiter limiter = limiterWith(3, 15);
        limiter.recordFailure("1.1.1.1", "user@example.com");
        limiter.recordFailure("1.1.1.1", "user@example.com");

        limiter.reset("1.1.1.1", "user@example.com");

        assertThatCode(() -> limiter.checkAllowed("1.1.1.1", "user@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void blocksByEmailEvenWhenIpRotates() {
        LoginRateLimiter limiter = limiterWith(3, 15);

        limiter.recordFailure("1.1.1.1", "victim@example.com");
        limiter.recordFailure("2.2.2.2", "victim@example.com");
        limiter.recordFailure("3.3.3.3", "victim@example.com");

        assertThatThrownBy(() -> limiter.checkAllowed("4.4.4.4", "victim@example.com"))
                .isInstanceOf(TooManyAttemptsException.class);
    }

    @Test
    void expiredWindowAllowsAgain() {
        LoginRateLimiter limiter = limiterWith(3, 0); // zero-minute window expires immediately

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("1.1.1.1", "user@example.com");
        }

        assertThatCode(() -> limiter.checkAllowed("1.1.1.1", "user@example.com"))
                .doesNotThrowAnyException();
    }
}
