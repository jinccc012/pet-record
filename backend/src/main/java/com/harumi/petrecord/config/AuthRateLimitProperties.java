package com.harumi.petrecord.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Brute-force protection settings for the login endpoint. A client is blocked once it accumulates
 * {@code maxAttempts} failed logins within {@code windowMinutes}, counted per source IP and per
 * email. A successful login clears both counters.
 */
@ConfigurationProperties(prefix = "app.auth.rate-limit")
public class AuthRateLimitProperties {

    private int maxAttempts = 10;
    private long windowMinutes = 15;

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getWindowMinutes() {
        return windowMinutes;
    }

    public void setWindowMinutes(long windowMinutes) {
        this.windowMinutes = windowMinutes;
    }
}
