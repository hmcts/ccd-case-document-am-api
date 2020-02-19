package uk.gov.hmcts.reform.ccd.document.am.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

@Data
@EqualsAndHashCode()
public class UploadedEvidence {
    private final Resource content;
    private final String name;
    private final String contentType;

    public static UploadedEvidence pdf(byte[] content, String name) {
        return new UploadedEvidence(new ByteArrayResource(content), name, "application/pdf");
    }

    public UploadedEvidence(Resource content, String name, String contentType) {
        this.content = content;
        this.name = name;
        this.contentType = contentType;
    }

    @Override
    public String toString() {
        return "UploadedEvidence{"
            +
                "content=" + content
            +
                ", name='" + name + '\''
            +
                ", contentType='" + contentType + '\''
            +
                '}';
    }
}
