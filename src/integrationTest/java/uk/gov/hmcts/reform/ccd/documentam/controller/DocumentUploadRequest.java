package uk.gov.hmcts.reform.ccd.documentam.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class DocumentUploadRequest {

    private String classification;
    private String caseTypeId;
    private String jurisdictionId;
    private List<MultipartFile> files;
}
