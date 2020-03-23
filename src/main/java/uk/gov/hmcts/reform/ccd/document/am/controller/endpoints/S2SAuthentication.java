package uk.gov.hmcts.reform.ccd.document.am.controller.endpoints;

import feign.FeignException;
import feign.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.ErrorResponse;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.InvalidRequest;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.document.am.controller.feign.CaseDocumentBinaryDownloadClientApi;
import uk.gov.hmcts.reform.ccd.document.am.controller.feign.CaseDocumentMetadataDownloadClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.util.ResponseHelper;
import org.springframework.core.io.Resource;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CONTENT_DISPOSITION;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CONTENT_LENGTH;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CONTENT_TYPE;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.DATA_SOURCE;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.ORIGINAL_FILE_NAME;

import java.util.UUID;

import static org.springframework.http.ResponseEntity.ok;

@RestController
public class S2SAuthentication {

    private transient CaseDocumentMetadataDownloadClientApi caseDocumentMetadataDownloadClientApi;
    private transient CaseDocumentBinaryDownloadClientApi caseDocumentBinaryDownloadClientApi;

    @Autowired
    public S2SAuthentication(CaseDocumentMetadataDownloadClientApi caseDocumentMetadataDownloadClientApi,
                             CaseDocumentBinaryDownloadClientApi caseDocumentBinaryDownloadClientApi) {
        this.caseDocumentMetadataDownloadClientApi = caseDocumentMetadataDownloadClientApi;
        this.caseDocumentBinaryDownloadClientApi = caseDocumentBinaryDownloadClientApi;
    }

    @RequestMapping(value = "/testS2SAuthorization", method = RequestMethod.GET)
    public ResponseEntity<String> testS2SAuthorization() {

        return ok("S2S Authentication is successful !!");
    }

    @RequestMapping(value = "/testClientLibrary/{documentId}", method = RequestMethod.GET)
    public ResponseEntity clientLibraryForGetDocumentMetaData(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
                                                                      @RequestHeader("ServiceAuthorization") String serviceAuth,
                                                                      @PathVariable("documentId") UUID documentId) {


        try (Response response = caseDocumentMetadataDownloadClientApi.getMetadataForDocument(authorisation, serviceAuth, documentId)) {
            Class clazz = response.status() > 300 ? ErrorResponse.class : StoredDocumentHalResource.class;
            ResponseEntity responseEntity = ResponseHelper.toResponseEntityByFeignClient(response,clazz, documentId);
            if (HttpStatus.OK.equals(responseEntity.getStatusCode())) {
                return responseEntity;
            } else {
                //LOG.error("Document doesn't exist for requested document id at Document Store API Side " + responseEntity.getStatusCode());
                throw new ResourceNotFoundException(documentId.toString());
            }
        } catch (FeignException ex) {
            //log.error("Document Store api failed:: status code ::" + ex.status());
            throw new InvalidRequest("Document Store api failed!!");
        }
    }

    @RequestMapping(value = "/testClientLibrary/{documentId}/binary", method = RequestMethod.GET)
    public ResponseEntity<Object> clientLibraryForBinaryContent(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
                                                                      @RequestHeader("ServiceAuthorization") String serviceAuth,
                                                                      @PathVariable("documentId") UUID documentId) {

        try  {
            ResponseEntity<Resource> response = caseDocumentBinaryDownloadClientApi.getDocumentBinary(authorisation, serviceAuth, documentId);

            if (HttpStatus.OK.equals(response.getStatusCode())) {
                return ResponseEntity.ok().headers(getHeaders(response))
                    .body((ByteArrayResource) response.getBody());
            } else {
                return ResponseEntity
                    .status(response.getStatusCode())
                    .body(response.getBody());
            }

        } catch (FeignException ex) {
            //log.error("Requested document could not be downloaded, DM Store Response Code ::" + ex.getMessage());
            throw new ResourceNotFoundException("Cannot download document that is stored");
        }

    }

    private HttpHeaders getHeaders(ResponseEntity<Resource> response) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(ORIGINAL_FILE_NAME,response.getHeaders().get(ORIGINAL_FILE_NAME).get(0));
        headers.add(CONTENT_DISPOSITION,response.getHeaders().get(CONTENT_DISPOSITION).get(0));
        headers.add(DATA_SOURCE,response.getHeaders().get(DATA_SOURCE).get(0));
        headers.add(CONTENT_TYPE, response.getHeaders().get(CONTENT_TYPE).get(0));
        headers.add(CONTENT_LENGTH,response.getHeaders().get(CONTENT_LENGTH).get(0));
        return headers;

    }
}


