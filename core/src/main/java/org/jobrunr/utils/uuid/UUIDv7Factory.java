package org.jobrunr.utils.uuid;

/*
 * MIT License
 *
 * Copyright (c) 2018-2022 Fabio Lima
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.time.Clock;
import java.util.Random;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Concrete factory for creating Unix epoch time-ordered unique identifiers
 * (UUIDv7).
 * <p>
 * UUIDv7 is a new UUID version proposed by Peabody and Davis. It is similar to
 * Prefix COMB GUID and ULID.
 * <p>
 * This factory creates 3 types:
 * <p>
 * <ul>
 * <li><b>Type 1 (default)</b>: this type is divided into 3 components, namely
 * time, counter and random. The counter component is incremented by 1 when the
 * time repeats. The random component is always randomized.
 * <li><b>Type 2 (plus 1)</b>: this type is divided into 2 components, namely time
 * and monotonic random. The monotonic random component is incremented by 1 when
 * the time repeats. This type of UUID is like a Monotonic ULID. It can be much
 * faster than the other types.
 * <li><b>Type 3 (plus n)</b>: this type is also divided in 2 components, namely
 * time and monotonic random. The monotonic random component is incremented by a
 * random positive integer between 1 and MAX when the time repeats. If the value
 * of MAX is not specified, MAX is 2^32. This type of UUID is also like a
 * Monotonic ULID.
 * </ul>
 * <p>
 * <b>Warning:</b> this can change in the future.
 *
 * @see <a href="https://github.com/f4b6a3/uuid-creator">UUID Creator</a>
 * @see <a href="https://github.com/ulid/spec">ULID Specification</a>
 * @see <a href=
 * "https://tools.ietf.org/html/draft-peabody-dispatch-new-uuid-format">New
 * UUID formats</a>
 * @see <a href="https://datatracker.ietf.org/wg/uuidrev/documents/">Revise
 * Universally Unique Identifier Definitions (uuidrev)</a>
 */
public final class UUIDv7Factory {

    private static final int INCREMENT_TYPE_DEFAULT = 0; // add 2^48 to `rand_b`
    private static final int INCREMENT_TYPE_PLUS_1 = 1; // add 1 to `rand_b`
    private static final int INCREMENT_TYPE_PLUS_N = 2; // add n to `rand_b`, where 1 <= n <= 2^32-1

    // Used to preserve monotonicity when the system clock is
    // adjusted by NTP after a small clock drift or when the
    // system clock jumps back by 1 second due to leap second.
    static final int CLOCK_DRIFT_TOLERANCE = 10_000;


    private final Clock clock;
    private final int incrementType;
    private final LongSupplier incrementSupplier;
    private final Random random;
    private final long versionMask;
    private UUID lastUuid;


    public UUIDv7Factory() {
        this(builder());
    }

    public UUIDv7Factory(Clock clock) {
        this(builder().withClock(clock));
    }

    public UUIDv7Factory(Builder builder) {
        this.clock = builder.getClock();
        this.incrementType = builder.getIncrementType();
        this.incrementSupplier = builder.getIncrementSupplier();
        this.random = new Random();
        this.versionMask = 7L << 12;

        // initialize internal state
        this.lastUuid = make(clock.millis(), random.nextLong(), random.nextLong());
    }

    /**
     * Concrete builder for creating a Unix epoch time-ordered factory.
     */
    public static class Builder {

        protected static final Clock DEFAULT_CLOCK = Clock.systemUTC();
        protected Clock clock;
        protected Random random;
        private Integer incrementType = INCREMENT_TYPE_DEFAULT;
        private Long incrementMax;

        protected Clock getClock() {
            if (this.clock == null) {
                this.clock = DEFAULT_CLOCK;
            }
            return this.clock;
        }

        public Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder withIncrementPlus1() {
            this.incrementType = INCREMENT_TYPE_PLUS_1;
            this.incrementMax = null;
            return this;
        }

        public Builder withIncrementPlusN() {
            this.incrementType = INCREMENT_TYPE_PLUS_N;
            this.incrementMax = null;
            return this;
        }

        public Builder withIncrementPlusN(long incrementMax) {
            this.incrementType = INCREMENT_TYPE_PLUS_N;
            this.incrementMax = incrementMax;
            return this;
        }

        protected int getIncrementType() {
            return this.incrementType;
        }

        protected LongSupplier getIncrementSupplier() {
            switch (getIncrementType()) {
                case INCREMENT_TYPE_PLUS_1:
                    // add 1 to rand_b
                    return () -> 1L;
                case INCREMENT_TYPE_PLUS_N:
                    if (incrementMax == null) {
                        return () -> {
                            // add n to rand_b, where 1 <= n <= 2^32
                            return (this.random.nextLong() >>> 32) + 1;
                        };
                    } else {
                        final long positive = 0x7fffffffffffffffL;
                        return () -> {
                            // add n to rand_b, where 1 <= n <= incrementMax
                            return ((this.random.nextLong() & positive) % incrementMax) + 1;
                        };
                    }
                case INCREMENT_TYPE_DEFAULT:
                default:
                    // add 2^48 to rand_b
                    return () -> (1L << 48);
            }
        }

        public UUIDv7Factory build() {
            return new UUIDv7Factory(this);
        }
    }

    /**
     * Returns a builder of Unix epoch time-ordered factory.
     *
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a time-ordered unique identifier (UUIDv7).
     *
     * @return a UUIDv7
     */
    public synchronized UUID create() {

        final long time = clock.millis();
        final long lastTime = lastUuid.getMostSignificantBits() >>> 16;

        // Check if the current time is the same as the previous time or has moved
        // backwards after a small system clock adjustment or after a leap second.
        // Drift tolerance = (previous_time - 10s) < current_time <= previous_time
        if ((time > lastTime - CLOCK_DRIFT_TOLERANCE) && (time <= lastTime)) {
            this.lastUuid = increment(this.lastUuid);
        } else {
            final long long1 = this.random.nextLong();
            final long long2 = this.random.nextLong();
            this.lastUuid = make(time, long1, long2);
        }

        return copy(this.lastUuid);
    }

    private synchronized UUID increment(UUID uuid) {

        // Used to check if an overflow occurred.
        final long overflow = 0x0000000000000000L;

        // Used to propagate increments through bits.
        final long versionMask = 0x000000000000f000L;
        final long variantMask = 0xc000000000000000L;

        long msb = (uuid.getMostSignificantBits() | versionMask);
        long lsb = (uuid.getLeastSignificantBits() | variantMask) + incrementSupplier.getAsLong();

        if (INCREMENT_TYPE_DEFAULT == this.incrementType) {

            // Used to clear the random component bits.
            final long clearMask = 0xffff000000000000L;

            // If the counter's 14 bits overflow,
            if ((lsb & clearMask) == overflow) {
                msb += 1; // increment the MSB.
            }
            final byte[] bytes = new byte[6];
            this.random.nextBytes(bytes);

            // And finally, randomize the lower 48 bits of the LSB.
            lsb &= clearMask; // Clear the random before randomize.
            lsb |= ByteUtil.toNumber(bytes);

        } else {
            // If the 62 bits of the monotonic random overflow,
            if (lsb == overflow) {
                msb += 1; // increment the MSB.
            }
        }

        return toUuid(msb, lsb);
    }

    private UUID make(final long time, final long long1, final long long2) {
        return toUuid((time << 16) | (long1 & 0x000000000000ffffL), long2);
    }

    private synchronized UUID copy(UUID uuid) {
        return toUuid(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

    /**
     * Creates a UUID from a pair of numbers.
     * <p>
     * It applies the version and variant numbers to the resulting UUID.
     *
     * @param msb the most significant bits
     * @param lsb the least significant bits
     * @return a UUID
     */
    private UUID toUuid(final long msb, final long lsb) {
        final long msb0 = (msb & 0xffffffffffff0fffL) | this.versionMask; // set version
        final long lsb0 = (lsb & 0x3fffffffffffffffL) | 0x8000000000000000L; // set variant
        return new UUID(msb0, lsb0);
    }
}