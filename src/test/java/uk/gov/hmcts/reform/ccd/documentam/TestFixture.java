package uk.gov.hmcts.reform.ccd.documentam;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateTtlRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.UUID;

public interface TestFixture {
    UUID MATCHED_DOCUMENT_ID = UUID.fromString("41334a2b-79ce-44eb-9168-2d49a744be9c");
    UUID UNMATCHED_DOCUMENT_ID = UUID.fromString("41334a2b-79ce-44eb-9168-2d49a744be9d");
    UUID DOCUMENT_ID = MATCHED_DOCUMENT_ID;
    UUID DOCUMENT_ID_1 = UUID.randomUUID();
    UUID DOCUMENT_ID_2 = UUID.randomUUID();
    long RANDOM_LONG = new Random().nextLong();
    int RANDOM_INT = new Random().nextInt();
    String RANDOM_STRING = generateRandomString();
    String TEST_STRING = "test";
    String REQUEST_ID = "Test Request ID";
    String REQUEST_PATH = "/test_path";
    String CREATED_BY = "createdBy";
    String JURISDICTION = "Test-Jurisdiction";
    String CASE_TYPE = "Test-Case-Type";
    String USER_ID = "d5566a63-f87c-4658-a4d6-213d949f8415";
    String CASE_TYPE_ID_VALUE = "BEFTA_CASETYPE_2";
    String JURISDICTION_ID_VALUE = "BEFTA_JURISDICTION_2";
    String CASE_ID_VALUE = "1582550122096256";
    String XUI_WEBAPP = "xui_webapp";

    static String generateRandomString() {
        final byte[] array = new byte[10];
        new Random().nextBytes(array);
        return new String(array, StandardCharsets.UTF_8);
    }

    static LocalDateTime makeTtlInMinutes(final long minutes) {
        return LocalDateTime.now(Clock.systemUTC())
            .plus(minutes, ChronoUnit.MINUTES);
    }

    static UpdateTtlRequest buildUpdateDocumentCommand() {
        final LocalDateTime ttl = makeTtlInMinutes(10);

        return new UpdateTtlRequest(ttl);
    }

    static ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        return objectMapper;
    }

    static String objectToJsonString(final Object object) throws IOException {
        final ObjectMapper objectMapper = objectMapper();

        return objectMapper.writeValueAsString(object);
    }

}
