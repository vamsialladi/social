package com.social.util;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates MongoDB-compatible 24-character hexadecimal ObjectId strings:
 * - 4 bytes: Unix epoch seconds
 * - 5 bytes: Random process/machine identifier
 * - 3 bytes: Monotonically increasing counter
 */
public final class IdGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final byte[] RANDOM_BYTES = new byte[5];
    private static final AtomicInteger COUNTER = new AtomicInteger(SECURE_RANDOM.nextInt(0xFFFFFF));

    static {
        SECURE_RANDOM.nextBytes(RANDOM_BYTES);
    }

    private IdGenerator() {
    }

    public static String nextId() {
        int timestamp = (int) Instant.now().getEpochSecond();
        int counter = COUNTER.getAndIncrement() & 0xFFFFFF;

        StringBuilder sb = new StringBuilder(24);
        sb.append(String.format("%08x", timestamp));
        for (byte b : RANDOM_BYTES) {
            sb.append(String.format("%02x", b));
        }
        sb.append(String.format("%06x", counter));

        return sb.toString();
    }
}
