package uk.gov.hmcts.reform.ccd.documentam;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.UUID;

public interface TestFixture {
    UUID RANDOM_UUID = UUID.randomUUID();
    String RANDOM_DOCUMENT_ID = UUID.randomUUID().toString();
    String DOCUMENT_ID_1 = UUID.randomUUID().toString();
    String DOCUMENT_ID_2 = UUID.randomUUID().toString();
    String VALID_CASE_ID = "9511425043588823";
    long RANDOM_LONG = new Random().nextLong();
    int RANDOM_INT = new Random().nextInt();
    String RANDOM_STRING = generateRandomString();
    String TEST_STRING = "test";
    String REQUEST_ID = "Test Request ID";
    String REQUEST_PATH = "/test_path";
    String CREATED_BY = "createdBy";
    String JURISDICTION = "Test-Jurisdiction";
    String CASE_TYPE = "Test-Case-Type";

    static String generateRandomString() {
        final byte[] array = new byte[10];
        new Random().nextBytes(array);
        return new String(array, StandardCharsets.UTF_8);
    }
}
