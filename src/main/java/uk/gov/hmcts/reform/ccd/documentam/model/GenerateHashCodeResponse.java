package uk.gov.hmcts.reform.ccd.documentam.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class GenerateHashCodeResponse {
    private String hashToken;
}
