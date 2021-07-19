package uk.gov.hmcts.reform.ccd.documentam.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.DM_ZONED_DATE_TIME_FORMAT;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DmTtlRequest {

    @JsonFormat(pattern = DM_ZONED_DATE_TIME_FORMAT)
    private ZonedDateTime ttl;

}
