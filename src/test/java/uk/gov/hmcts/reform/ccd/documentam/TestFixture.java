package uk.gov.hmcts.reform.ccd.documentam;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.ccd.documentam.dto.UpdateTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ServiceException;
import uk.gov.hmcts.reform.ccd.documentam.model.DmTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

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
    String SERVICE_NAME_XUI_WEBAPP = "xui_webapp";
    Date NULL_TTL = null;
    String SALT = "AAAOA7A2AA6AAAA5";
    String ORIGINAL_DOCUMENT_NAME = "filename.txt";
    String SELF_LINK = "http://dm-store:8080/documents/" + DOCUMENT_ID;
    String BINARY_LINK = "http://dm-store:8080/documents/" + DOCUMENT_ID + "/binary";
    String MIME_TYPE = "application/octet-stream";

    static String generateRandomString() {
        final byte[] array = new byte[10];
        new Random().nextBytes(array);
        return new String(array, StandardCharsets.UTF_8);
    }

    static Instant makeTtlInMinutes(final long minutes) {
        return Instant.now(Clock.systemUTC())
            .plus(minutes, ChronoUnit.MINUTES);
    }

    static UpdateTtlRequest buildUpdateDocumentCommand() {
        final Instant instant = makeTtlInMinutes(10);

        return new UpdateTtlRequest(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
    }

    static DmTtlRequest buildTtlRequest() {
        return new DmTtlRequest(makeTtlInMinutes(10).atZone(ZoneId.systemDefault()));
    }

    static PatchDocumentResponse buildPatchDocumentResponse() {
        final Instant instant = makeTtlInMinutes(10);

        return PatchDocumentResponse.builder()
            .ttl(Date.from(instant))
            .build();
    }

    static ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        return objectMapper;
    }

    static String objectToJsonString(final Object object) {
        try {
            final ObjectMapper objectMapper = objectMapper();

            return objectMapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static Stream<Arguments> provideHttpErrorForDocumentParameters() {
        final String serviceExceptionMessagePrefix = "Exception occurred with operation on document id: ";

        return Stream.of(
            Arguments.of(HttpStatus.BAD_GATEWAY, ServiceException.class, serviceExceptionMessagePrefix),
            Arguments.of(HttpStatus.NOT_FOUND, ResourceNotFoundException.class, "Resource not found ")
        );
    }

    static Document.Links getLinks() {
        Document.Links links = new Document.Links();

        Document.Link self = new Document.Link();
        Document.Link binary = new Document.Link();
        self.href = SELF_LINK;
        binary.href = BINARY_LINK;

        links.self = self;
        links.binary = binary;

        return links;
    }

}
