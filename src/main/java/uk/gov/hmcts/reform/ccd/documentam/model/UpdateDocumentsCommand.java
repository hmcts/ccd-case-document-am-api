package uk.gov.hmcts.reform.ccd.documentam.model;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.Date;
import java.util.List;

@Data
public class UpdateDocumentsCommand {

    public final Date ttl;
    public final @NotEmpty List<DocumentUpdate> documents;
}
