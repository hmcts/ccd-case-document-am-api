
package uk.gov.hmcts.reform.ccd.document.am.controller.endpoints;

import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.S2S_API_PARAM;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.SERVICE_AUTHORIZATION;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.MetadataSearchCommand;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResourceCollection;
import uk.gov.hmcts.reform.ccd.document.am.model.UpdateDocumentCommand;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadDocumentsCommand;
import uk.gov.hmcts.reform.ccd.document.am.service.DocumentManagementService;

@Controller
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class CaseDocumentAmController implements CaseDocumentAm {

    private static final Logger LOG = LoggerFactory.getLogger(CaseDocumentAmController.class);

    private transient ObjectMapper objectMapper;
    private transient HttpServletRequest request;
    private transient DocumentManagementService  documentManagementService;
    private RestTemplate restTemplate;
    private String dmStoreURL = "http://localhost:4506";

    @Autowired
    public CaseDocumentAmController(ObjectMapper objectMapper, HttpServletRequest request, DocumentManagementService  documentManagementService) {
        this.objectMapper = objectMapper;
        this.request = request;
        this.documentManagementService = documentManagementService;
    }

    @Override
    public ResponseEntity<String> deleteDocumentbyDocumentId(
        @ApiParam(value = "Service Auth (S2S). Use it when accessing the API on App Tier level.", required = true)
        @RequestHeader(value = "ServiceAuthorization", required = true) String serviceAuthorization,

        @ApiParam("documentId")
        @PathVariable("documentId") UUID documentId,

        @ApiParam("permanent delete flag")
        @Valid @RequestParam(value = "permanent", required = false) Boolean permanent,

        @ApiParam("User-Id of the currently authenticated user. If provided will be used to populate the creator field of a document"
                          + " and will be used for authorisation.")
        @RequestHeader(value = "User-Id", required = false) String userId,

        @ApiParam("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "User-Roles", required = false) String userRoles) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<String>(objectMapper.readValue("\"\"", String.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                LOG.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<String>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Object> getDocumentBinaryContentbyDocumentId(
        @ApiParam(value = "Service Auth (S2S). Use it when accessing the API on App Tier level.", required = true)
        @RequestHeader(value = "ServiceAuthorization", required = true) String serviceAuthorization,
        @ApiParam("documentId")
        @PathVariable("documentId") UUID documentId,
        @ApiParam("User-Id of the currently authenticated user. If provided will be used to populate the creator field of a document"
                          + " and will be used for authorisation.")
        @RequestHeader(value = "User-Id", required = false) String userId,
        @ApiParam("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "User-Roles", required = false) String userRoles) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<Object>(objectMapper.readValue("{ }", Object.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                LOG.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<Object>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Object> getDocumentbyDocumentId(
        @ApiParam(value = "Service Auth (S2S). Use it when accessing the API on App Tier level.", required = true)
        @RequestHeader(value = "ServiceAuthorization", required = true) String serviceAuthorization,

        @ApiParam("documentId")
        @PathVariable("documentId") UUID documentId,

        @ApiParam("User-Id of the currently authenticated user. If provided will be used to populate the creator field of a document"
                          + " and will be used for authorisation.")
        @RequestHeader(value = "User-Id", required = false) String userId,
        @ApiParam("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "User-Roles", required = false) String userRoles) {

        ResponseEntity responseEntity = documentManagementService.getDocumentMetadata(documentId);

        return  ResponseEntity
            .status(HttpStatus.OK)
            .body(responseEntity.getBody());
    }

    @Override
    public ResponseEntity<StoredDocumentHalResource> patchDocumentbyDocumentId(
        @ApiParam(value = "", required = true)
        @Valid @RequestBody UpdateDocumentCommand body,

        @ApiParam(value = "Service Auth (S2S). Use it when accessing the API on App Tier level.", required = true)
        @RequestHeader(value = "ServiceAuthorization", required = true) String serviceAuthorization,

        @ApiParam("documentId")
        @PathVariable("documentId") UUID documentId,

        @ApiParam("User-Id of the currently authenticated user. If provided will be used to populate the creator field of a document"
                          + " and will be used for authorisation.")
        @RequestHeader(value = "User-Id", required = false) String userId,

        @ApiParam("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "User-Roles", required = false) String userRoles) {

        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<StoredDocumentHalResource>(objectMapper.readValue("",
                    StoredDocumentHalResource.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                LOG.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<StoredDocumentHalResource>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<StoredDocumentHalResource>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<StoredDocumentHalResource> patchMetaDataOnDocuments(

        @ApiParam(value = "", required = true)
        @Valid @RequestBody CaseDocumentMetadata body,

        @ApiParam(value = "Service Auth (S2S). Use it when accessing the API on App Tier level.", required = true)
        @RequestHeader(value = "ServiceAuthorization", required = true) String serviceAuthorization,

        @ApiParam("User-Id of the currently authenticated user. If provided will be used to populate the creator field of a document"
                          + " and will be used for authorisation.")
        @RequestHeader(value = "User-Id", required = false) String userId,

        @ApiParam("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "User-Roles", required = false) String userRoles) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<StoredDocumentHalResource>(objectMapper.readValue("",
                    StoredDocumentHalResource.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                LOG.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<StoredDocumentHalResource>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<StoredDocumentHalResource>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<StoredDocumentHalResourceCollection> postDocumentsSearchCommand(
        @ApiParam(value = "", required = true) @Valid @RequestBody MetadataSearchCommand body,
        @ApiParam(value = "Service Auth (S2S). Use it when accessing the API on App Tier level.", required = true)
        @RequestHeader(value = "ServiceAuthorization", required = true) String serviceAuthorization,
        @ApiParam("User-Id of the currently authenticated user. If provided will be used to populate the creator field of a document"
                          + " and will be used for authorisation.")
        @RequestHeader(
        value = "User-Id", required = false) String userId,
        @ApiParam("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "User-Roles", required = false) String userRoles,
        @ApiParam("") @Valid @RequestParam(value = "offset", required = false) Long offset,
        @ApiParam("") @Valid @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
        @ApiParam("") @Valid @RequestParam(value = "pageSize", required = false) Integer pageSize,
        @ApiParam("") @Valid @RequestParam(value = "paged", required = false) Boolean paged,
        @ApiParam("") @Valid @RequestParam(value = "sort.sorted", required = false) Boolean sortSorted,
        @ApiParam("") @Valid @RequestParam(value = "sort.unsorted", required = false) Boolean sortUnsorted,
        @ApiParam("") @Valid @RequestParam(value = "unpaged", required = false) Boolean unpaged) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<StoredDocumentHalResourceCollection>(objectMapper.readValue("",
                    StoredDocumentHalResourceCollection.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                LOG.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<StoredDocumentHalResourceCollection>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<StoredDocumentHalResourceCollection>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<StoredDocumentHalResourceCollection> uploadDocuments(
        @ApiParam(value = "", required = true) @RequestParam(value = "files", required = true) List<java.io.File> files,
        @ApiParam(value = "", required = true) @RequestParam(value = "classification", required = true) String classification,
        @ApiParam(value = "", required = true) @RequestParam(value = "ttl", required = true) Date ttl,
        @ApiParam(value = "", required = false) @RequestParam(value = "roles", required = false) List<String> roles,

        @ApiParam(value = S2S_API_PARAM, required = true)
        @RequestHeader(value = SERVICE_AUTHORIZATION, required = true) String serviceAuthorization,

        @ApiParam(value = "CaseType identifier for the case document.", required = true)
        @RequestHeader(value = "caseTypeId", required = true) String caseTypeId,

        @ApiParam(value = "Jurisdiction identifier for the case document.", required = true)
        @RequestHeader(value = "jurisdictionId", required = true) String jurisdictionId,

        @ApiParam("User-Id of the currently authenticated user. If provided will be used to populate the creator field of a document"
                  + " and will be used for authorisation.")
        @RequestHeader("user-id") String userId,

        @ApiParam(value = "Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "user-roles", required = false) String userRoles) {

        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("files", new ClassPathResource("file.png"));
        map.set("classification", "PUBLIC");
        map.set("roles", "caseworker");
        map.set("user-id", "auto.test.cnp@gmail.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("ServiceAuthorization", serviceAuthorization);

        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity
            = new HttpEntity<LinkedMultiValueMap<String, Object>>(
            map, headers);

        ResponseEntity<StoredDocumentHalResourceCollection> responseEntity1 = restTemplate.postForEntity(dmStoreURL, requestEntity, StoredDocumentHalResourceCollection.class);
        return responseEntity1;
    }

}
