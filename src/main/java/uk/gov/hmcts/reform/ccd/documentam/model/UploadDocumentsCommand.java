package uk.gov.hmcts.reform.ccd.documentam.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Classification;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.List;
import java.util.Map;


@Data
public class UploadDocumentsCommand {

    @NotNull(message = "Provide some files to be uploaded.")
    @Size(min = 1, message = "Please provide at least one file to be uploaded.")
    private List<MultipartFile> files;

    @NotNull(message = "Please provide classification")
    private Classification classification;

    @Getter
    @Setter
    private List<String> roles;

    @Getter
    @Setter
    private Map<String, String> metadata;

    @Getter
    @Setter
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
    private Date ttl;

}
