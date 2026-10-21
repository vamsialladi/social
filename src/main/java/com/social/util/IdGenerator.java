package com.social.util;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates 24-hex-character ObjectId strings compatible with MongoDB ObjectId format:
 * 4-byte timestamp + 5-byte random value + 3-byte incrementing counter.
 */
public final class IdGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final AtomicInteger COUNTER = new AtomicInteger(SECURE_RANDOM.nextInt(0x00ffffff));
    private static final byte[] RANDOM_BYTES = new byte[5];

    static {
        SECURE_RANDOM.nextBytes(RANDOM_BYTES);
    }

    private IdGenerator() {
    }

    public static String nextObjectId() {
        int timestamp = (int) Instant.now().getEpochSecond();
        int counter = COUNTER.incrementAndGet() & 0x00ffffff;

        StringBuilder sb = new StringBuilder(24);
        // 4 bytes timestamp (8 chars)
        sb.append(String.format("%08x", timestamp));
        // 5 bytes random (10 chars)
        for (byte b : RANDOM_BYTES) {
            sb.append(String.format("%02x", b & 0xff));
        }
        // 3 bytes counter (6 chars)
        sb.append(String.format("%06x", counter));

        return sb.toString();
    }
}
