
package uk.gov.hmcts.reform.ccd.document.am.controller.endpoints;

import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.INPUT_STRING_PATTERN;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ResponseFormatException;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.MetadataSearchCommand;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResourceCollection;
import uk.gov.hmcts.reform.ccd.document.am.model.UpdateDocumentCommand;
import uk.gov.hmcts.reform.ccd.document.am.service.CaseDataStoreService;
import uk.gov.hmcts.reform.ccd.document.am.service.DocumentManagementService;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import uk.gov.hmcts.reform.ccd.document.am.service.common.ValidationService;

@Controller
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class CaseDocumentAmController implements CaseDocumentAm {

    private static final Logger LOG = LoggerFactory.getLogger(CaseDocumentAmController.class);

    private transient ObjectMapper objectMapper;
    private transient HttpServletRequest request;
    private transient DocumentManagementService  documentManagementService;
    private transient CaseDataStoreService caseDataStoreService;

    @Autowired
    public CaseDocumentAmController(ObjectMapper objectMapper, HttpServletRequest request, DocumentManagementService documentManagementService,
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

        return new ResponseEntity<String>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> getDocumentBinaryContentbyDocumentId(
        @ApiParam(value = "Service Auth (S2S). Use it when accessing the API on App Tier level.", required = true)
        @RequestHeader(value = "ServiceAuthorization", required = true) String serviceAuthorization,
        @ApiParam("documentId")
        @PathVariable("documentId") UUID documentId,
        @ApiParam("Authorization header of the currently authenticated user")
        @RequestHeader(value = "Authorization", required = true) String authorization,
        @ApiParam("User-Id of the currently authenticated user. If provided will be used to populate the creator field of a document"
            + " and will be used for authorisation.")
        @RequestHeader(value = "User-Id", required = false) String userId,
        @ApiParam("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "User-Roles", required = false) String userRoles) {

        ResponseEntity documentMetadata = documentManagementService.getDocumentMetadata(documentId);
        if (documentManagementService.checkUserPermission(documentMetadata, documentId, authorization)) {
            return documentManagementService.getDocumentBinaryContent(documentId);

        }
        LOG.error("User don't have read permission on requested document " + HttpStatus.FORBIDDEN);
        throw new ForbiddenException(documentId.toString());
    }

    @Override
    public ResponseEntity<Object> getDocumentbyDocumentId(
        @ApiParam(value = "Service Auth (S2S). Use it when accessing the API on App Tier level.", required = true)
        @RequestHeader(value = "ServiceAuthorization", required = true) String serviceAuthorization,

        @ApiParam("documentId")
        @PathVariable("documentId") UUID documentId,

        @ApiParam("Authorization header of the currently authenticated user")
        @RequestHeader(value = "Authorization", required = true) String authorization,

        @ApiParam("User-Id of the currently authenticated user. If provided will be used to populate the creator field of a document"
                          + " and will be used for authorisation.")
        @RequestHeader(value = "user-id", required = false) String userId,

        @ApiParam("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "user-roles", required = false) String userRoles) {

        ResponseEntity responseEntity = documentManagementService.getDocumentMetadata(documentId);
        if (documentManagementService.checkUserPermission(responseEntity, documentId, authorization)) {
            return  ResponseEntity
                 .status(HttpStatus.OK)
                 .body(responseEntity.getBody());
        }
        LOG.error("User don't have read permission on requested document " + HttpStatus.FORBIDDEN);
        throw new ForbiddenException(documentId.toString());
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

        return new ResponseEntity<StoredDocumentHalResource>(HttpStatus.OK);
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

        return new ResponseEntity<StoredDocumentHalResource>(HttpStatus.OK);
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

        return new ResponseEntity<StoredDocumentHalResourceCollection>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> uploadDocuments(

        @ApiParam(value = "", required = true)
        @NotNull(message = "Provide some files to be uploaded.")
        @Size(min = 1, message = "Please provide at least one file to be uploaded.")
        @RequestParam(value = "files", required = true) List<MultipartFile> files,

        @ApiParam(value = "", required = true)
        @Valid
        @NotNull(message = "Please provide classification")
        @RequestParam(value = "classification", required = true) String classification,

        @ApiParam(value = "", required = false)
        @RequestParam(value = "roles", required = false) List<String> roles,

        @ApiParam(value = Constants.S2S_API_PARAM, required = true)
        @RequestHeader(value = Constants.SERVICE_AUTHORIZATION, required = true) String serviceAuthorization,

        @ApiParam(value = "CaseType identifier for the case document.", required = true)
        @NotNull(message = "Provide the Case Type ID ")
        @RequestHeader(value = "caseTypeId", required = true) String caseTypeId,

        @ApiParam(value = "Jurisdiction identifier for the case document.", required = true)
        @NotNull(message = "Provide the Jurisdiction ID ")
        @RequestHeader(value = "jurisdictionId", required = true) String jurisdictionId,

        @ApiParam(value = "User-Id of the currently authenticated user. If provided will be used to populate the creator field of a document"
                          + " and will be used for authorisation.", required = false)
        @RequestHeader(value = "user-id", required = true) String userId,

        @ApiParam(value = "Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "user-roles", required = false) String userRoles) {

        try {
            ValidationService.validateInputParams(INPUT_STRING_PATTERN, caseTypeId, jurisdictionId, classification, userRoles);
            ValidationService.isValidSecurityClassification(classification);
            ValidationService.validateLists(files, roles);

            return documentManagementService.uploadDocuments(files, classification, roles,
                                                             serviceAuthorization, caseTypeId, jurisdictionId, userId);
        } catch (BadRequestException | IllegalArgumentException e) {
            LOG.error("Exception while uploading the documents :" + e.getMessage());
            throw new BadRequestException("Exception while uploading the documents :" + e.getMessage());
        } catch (Exception e) {
            LOG.error("Exception while uploading the documents :" + e.getMessage());
            throw new ResponseFormatException("Exception while uploading the documents :" + e.getMessage());
        }
    }
}
