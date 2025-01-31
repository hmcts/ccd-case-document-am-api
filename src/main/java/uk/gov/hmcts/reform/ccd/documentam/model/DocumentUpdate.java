package uk.gov.hmcts.reform.ccd.documentam.model;

import java.util.Map;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUpdate {
    private @NotBlank UUID documentId;
    private @NotBlank Map<String, String> metadata;
}
