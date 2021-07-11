package uk.gov.hmcts.reform.ccd.documentam;

import uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentCommand;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;
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
    String USER_ID = "d5566a63-f87c-4658-a4d6-213d949f8415";
    String BEFTA_CASETYPE_2 = "BEFTA_CASETYPE_2";
    String BEFTA_JURISDICTION_2 = "BEFTA_JURISDICTION_2";
    String MATCHED_DOCUMENT_ID = "41334a2b-79ce-44eb-9168-2d49a744be9c";
    String UNMATCHED_DOCUMENT_ID = "41334a2b-79ce-44eb-9168-2d49a744be9d";
    String CASE_ID_TEST_VALUE = "1582550122096256";

    static String generateRandomString() {
        final byte[] array = new byte[10];
        new Random().nextBytes(array);
        return new String(array, StandardCharsets.UTF_8);
    }

    static DateTimeFormatter getDateTimeFormatter() {
        final String timestampPattern = "yyyy-MM-dd'T'HH:mm:ssZ";
        return DateTimeFormatter.ofPattern(timestampPattern, Locale.ENGLISH)
            .withZone(ZoneOffset.UTC);
    }

    static Date makeTtlInMinutes(final long minutes) {
        final Instant instant = Instant.now(Clock.systemUTC())
            .plus(minutes, ChronoUnit.MINUTES);

        return Date.from(instant);
    }

    static UpdateDocumentCommand buildUpdateDocumentCommand() {
        final Date ttl = makeTtlInMinutes(10);

        return new UpdateDocumentCommand(ttl);
    }

}
