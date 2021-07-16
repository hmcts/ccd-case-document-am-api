package uk.gov.hmcts.reform.ccd.documentam.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DmTtlRequest {

    public static final String UTIL_DATE_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    @JsonFormat(pattern = UTIL_DATE_TIMESTAMP_FORMAT)
    private LocalDateTime ttl;
}
