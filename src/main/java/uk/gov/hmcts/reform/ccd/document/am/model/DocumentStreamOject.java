package uk.gov.hmcts.reform.ccd.document.am.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

@Data
@Getter
@Setter
@EqualsAndHashCode()
public class DocumentStreamOject {
    private final Resource content;
    private final String name;
    private final String contentType;

    public static DocumentStreamOject pdf(byte[] content, String name) {
        return new DocumentStreamOject(new ByteArrayResource(content), name, "application/pdf");
    }

    public DocumentStreamOject(Resource content, String name, String contentType) {
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
