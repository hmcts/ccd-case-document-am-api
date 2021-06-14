package uk.gov.hmcts.reform.ccd.documentam.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PatchDocumentResponse {
    private Date ttl;
    private Date createdOn;
    private Date modifiedOn;
    private String originalDocumentName;
    private String mimeType;
    private String lastModifiedBy;
}
