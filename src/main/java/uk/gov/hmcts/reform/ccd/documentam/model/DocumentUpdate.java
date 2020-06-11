package uk.gov.hmcts.reform.ccd.documentam.model;

import java.util.Map;
import java.util.UUID;
import javax.validation.constraints.NotBlank;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DocumentUpdate {
    private @NotBlank UUID documentId;
    private @NotBlank Map<String, String> metadata;
}
