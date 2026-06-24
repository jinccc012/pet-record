package com.harumi.petrecord.auth;

import com.harumi.petrecord.common.exception.TooManyAttemptsException;
import com.harumi.petrecord.config.AuthRateLimitProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory fixed-window limiter that throttles repeated failed logins. Failures are tracked per
 * source IP and per email; once either key exceeds the configured threshold within the window,
 * further attempts are rejected until the window rolls over. State is per instance, which is an
 * acceptable baseline for the current single-region deployment.
 */
@Component
public class LoginRateLimiter {

    private record Counter(long windowStartMs, int attempts) {
    }

    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final AuthRateLimitProperties properties;

    public LoginRateLimiter(AuthRateLimitProperties properties) {
        this.properties = properties;
    }

    /** Rejects the attempt with a 429-mapped exception if either key is currently blocked. */
    public void checkAllowed(String ip, String email) {
        if (isBlocked(ipKey(ip)) || isBlocked(emailKey(email))) {
            throw new TooManyAttemptsException("Too many login attempts. Please try again later.");
        }
    }

    /** Records a failed login against both keys. */
    public void recordFailure(String ip, String email) {
        increment(ipKey(ip));
        increment(emailKey(email));
    }

    /** Clears the counters for both keys after a successful login. */
    public void reset(String ip, String email) {
        counters.remove(ipKey(ip));
        counters.remove(emailKey(email));
    }

    private boolean isBlocked(String key) {
        Counter counter = counters.get(key);
        if (counter == null) {
            return false;
        }
        if (isExpired(counter)) {
            counters.remove(key, counter);
            return false;
        }
        return counter.attempts() >= properties.getMaxAttempts();
    }

    private void increment(String key) {
        long now = System.currentTimeMillis();
        counters.compute(key, (k, existing) -> {
            if (existing == null || isExpired(existing, now)) {
                return new Counter(now, 1);
            }
            return new Counter(existing.windowStartMs(), existing.attempts() + 1);
        });
    }

    private boolean isExpired(Counter counter) {
        return isExpired(counter, System.currentTimeMillis());
    }

    private boolean isExpired(Counter counter, long now) {
        return now - counter.windowStartMs() >= properties.getWindowMinutes() * 60_000L;
    }

    private String ipKey(String ip) {
        return "ip:" + (StringUtils.hasText(ip) ? ip : "unknown");
    }

    private String emailKey(String email) {
        String normalized = StringUtils.hasText(email) ? email.trim().toLowerCase(Locale.ROOT) : "unknown";
        return "email:" + normalized;
    }
}
