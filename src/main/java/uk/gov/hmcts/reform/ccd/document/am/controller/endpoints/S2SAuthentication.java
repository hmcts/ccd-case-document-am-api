package uk.gov.hmcts.reform.ccd.document.am.controller.endpoints;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentBinaryDownloadClientApi;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentMetadataDownloadClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;

import java.util.UUID;

import static org.springframework.http.ResponseEntity.ok;

@RestController
public class S2SAuthentication {

    private transient CaseDocumentMetadataDownloadClientApi caseDocumentMetadataDownloadClientApi;
    private transient CaseDocumentBinaryDownloadClientApi caseDocumentBinaryDownloadClientApi;

    @Autowired
    public S2SAuthentication(CaseDocumentMetadataDownloadClientApi caseDocumentMetadataDownloadClientApi, CaseDocumentBinaryDownloadClientApi caseDocumentBinaryDownloadClientApi) {
        this.caseDocumentMetadataDownloadClientApi = caseDocumentMetadataDownloadClientApi;
        this.caseDocumentBinaryDownloadClientApi = caseDocumentBinaryDownloadClientApi;
    }

    @RequestMapping(value = "/testS2SAuthorization", method = RequestMethod.GET)
    public ResponseEntity<String> testS2SAuthorization() {

        return ok("S2S Authentication is successful !!");
    }

    @RequestMapping(value = "/testClientLibrary/{documentId}", method = RequestMethod.GET)
    public ResponseEntity<Object> clientLibraryForGetDocumentMetaData(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
                                                                      @RequestHeader("ServiceAuthorization") String serviceAuth,
                                                                      @PathVariable("documentId") UUID documentId) {

        Document response = caseDocumentMetadataDownloadClientApi.getMetadataForDocument(authorisation, serviceAuth, documentId);

        return  ResponseEntity
            .status(HttpStatus.OK)
            .body(response);
    }

    @RequestMapping(value = "/testClientLibrary/{documentId}/binary", method = RequestMethod.GET)
    public ResponseEntity<Object> clientLibraryForBinaryContent(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
                                                                      @RequestHeader("ServiceAuthorization") String serviceAuth,
                                                                      @PathVariable("documentId") UUID documentId) {

        ResponseEntity<Object>  response = caseDocumentBinaryDownloadClientApi.getDocumentBinary(authorisation, serviceAuth, documentId);

        return response;
    }
}


