package uk.gov.hmcts.reform.ccd.documentam;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.UUID;

public interface TestConstants {
    UUID RANDOM_UUID = UUID.randomUUID();
    long RANDOM_LONG = new Random().nextLong();
    int RANDOM_INT = new Random().nextInt();
    String RANDOM_STRING = generateRandomString();
    String TEST_STRING = "test";

    static String generateRandomString() {
        final byte[] array = new byte[10];
        new Random().nextBytes(array);
        return new String(array, StandardCharsets.UTF_8);
    }
}
