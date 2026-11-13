package uk.gov.hmcts.reform.ccd.documentam.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @ParameterizedTest
    @CsvSource({
        "400, INVALID_REQUEST", "422, INVALID_REQUEST", "401, AUTHENTICATION_REQUIRED",
        "403, ACCESS_DENIED", "404, RESOURCE_NOT_FOUND", "405, METHOD_NOT_ALLOWED",
        "413, REQUEST_TOO_LARGE", "415, UNSUPPORTED_MEDIA_TYPE", "429, TOO_MANY_REQUESTS",
        "500, INTERNAL_ERROR", "502, INTERNAL_ERROR", "503, INTERNAL_ERROR", "599, INTERNAL_ERROR",
        "409, REQUEST_FAILED", "418, REQUEST_FAILED", "499, REQUEST_FAILED", "600, REQUEST_FAILED"
    })
    void serializesTheSharedContractCategoryAsAString(int status, ErrorCode expected) throws Exception {
        assertThat(new ObjectMapper().writeValueAsString(ErrorCode.fromStatus(status)))
            .isEqualTo("\"" + expected.name() + "\"");
    }
}
