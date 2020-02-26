
package uk.gov.hmcts.reform.ccd.document.am.controller.endpoints;

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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.MetadataSearchCommand;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResourceCollection;
import uk.gov.hmcts.reform.ccd.document.am.model.UpdateDocumentCommand;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.document.am.service.CaseDataStoreService;
import uk.gov.hmcts.reform.ccd.document.am.service.DocumentManagementService;

@Controller
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class CaseDocumentAmController implements CaseDocumentAm {

    private static final Logger LOG = LoggerFactory.getLogger(CaseDocumentAmController.class);

    private transient ObjectMapper objectMapper;
    private transient HttpServletRequest request;
    private transient DocumentManagementService  documentManagementService;
    private transient CaseDataStoreService caseDataStoreService;

    @Autowired
    public CaseDocumentAmController(ObjectMapper objectMapper, HttpServletRequest request, DocumentManagementService  documentManagementService,
                                    CaseDataStoreService caseDataStoreService) {
        this.objectMapper = objectMapper;
        this.request = request;
        this.documentManagementService = documentManagementService;
        this.caseDataStoreService = caseDataStoreService;
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



        ResponseEntity documentMetadata = documentManagementService.getDocumentMetadata(documentId);
        String caseId = documentManagementService.extractCaseIdFromMetadata(documentMetadata.getBody());
        CaseDocumentMetadata caseDocumentMetadata = caseDataStoreService.getCaseDocumentMetadata(caseId,documentId);
        for (Document document : caseDocumentMetadata.getDocuments()) {
            if (document.getPermissions().contains(Permission.READ) && document.getId().equals(documentId.toString())) {
                ResponseEntity<Resource> response = documentManagementService.getDocumentBinaryContent(documentId);
                HttpHeaders headers = new HttpHeaders();
                headers.add("OriginalFileName",response.getHeaders().get("OriginalFileName").get(0));
                headers.add("Content-Disposition",response.getHeaders().get("Content-Disposition").get(0));
                headers.add("data-source",response.getHeaders().get("data-source").get(0));
                if (HttpStatus.OK.equals(response.getStatusCode())) {
                    return ResponseEntity.ok().headers(headers).contentLength(Integer.parseInt(response.getHeaders().get("Content-Length").get(0)))
                        .contentType(MediaType.parseMediaType(response.getHeaders().get("Content-Type").get(0))).body((ByteArrayResource) response.getBody());
                } else {
                    return ResponseEntity
                        .status(response.getStatusCode())
                        .body(response.getBody());
                }
            }
        }


        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid Request");
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
    public ResponseEntity<StoredDocumentHalResourceCollection> postDocumentsWithBinaryFile(
        @ApiParam(value = "", required = true) @RequestParam(value = "classification", required = true) String classification,
        @ApiParam(value = "", required = true) @RequestParam(value = "ttl", required = true) Date ttl,
        @ApiParam(value = "", required = true) @RequestParam(value = "roles", required = true) List<String> roles,
        @ApiParam(value = "", required = true) @RequestParam(value = "files", required = true) List<java.io.File> files,
        @ApiParam(value = "Service Auth (S2S). Use it when accessing the API on App Tier level.", required = true)
        @RequestHeader(value = "ServiceAuthorization", required = true) String serviceAuthorization,
        @ApiParam(value = "CaseType identifier for the case document.", required = true)
        @RequestHeader(value = "caseTypeId", required = true) String caseTypeId,
        @ApiParam(value = "Jurisdiction identifier for the case document.", required = true)
        @RequestHeader(value = "jurisdictionId", required = true) String jurisdictionId,
        @ApiParam("User-Id of the currently authenticated user. If provided will be used to populate the creator field of a document"
                          + " and will be used for authorisation.")
        @RequestHeader(value = "User-Id", required = false) String userId,
        @ApiParam("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "User-Roles", required = false) String userRoles) {

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

}
