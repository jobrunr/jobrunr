package org.jobrunr.utils.resilience;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.locks.ReentrantLock;

import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;

public class RateLimiter {

    public static final Duration SECOND = ofSeconds(1);

    private final ReentrantLock lock;
    private final Duration perDuration;
    private volatile Instant lastAllowed;

    private RateLimiter(Duration perDuration, Instant lastAllowed) {
        this.lock = new ReentrantLock();
        this.perDuration = perDuration;
        this.lastAllowed = lastAllowed;
    }

    public boolean isRateLimited() {
        return !isAllowed();
    }

    public boolean isAllowed() {
        if (lock.tryLock()) {
            try {
                if (lastAllowed.plus(perDuration).isBefore(now())) {
                    lastAllowed = now();
                    return true;
                }
                return false;
            } finally {
                lock.unlock();
            }
        } else {
            return false;
        }
    }

    public static class Builder {

        private int amount;

        public static Builder rateLimit() {
            return new Builder();
        }

        public Builder at1Request() {
            return atRequests(1);
        }

        public Builder at2Requests() {
            return atRequests(2);
        }

        public Builder at5Requests() {
            return atRequests(5);
        }

        public Builder at10Requests() {
            return atRequests(10);
        }

        public Builder atRequests(int amount) {
            this.amount = amount;
            return this;
        }

        public RateLimiter per(int time, ChronoUnit unit) {
            return per(Duration.of(time, unit));
        }

        public RateLimiter per(Duration duration) {
            Duration perDuration = duration.dividedBy(amount);
            Instant lastAllowed = now().minus(duration);
            return new RateLimiter(perDuration, lastAllowed);
        }

        public RateLimiter withoutLimits() {
            return new RateLimiter(Duration.ofNanos(1), now());
        }
    }
}
