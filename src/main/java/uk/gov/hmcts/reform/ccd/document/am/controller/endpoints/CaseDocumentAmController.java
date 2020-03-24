
package uk.gov.hmcts.reform.ccd.document.am.controller.endpoints;

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
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.UpdateDocumentCommand;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.document.am.service.DocumentManagementService;
import uk.gov.hmcts.reform.ccd.document.am.service.common.ValidationService;
import uk.gov.hmcts.reform.ccd.document.am.util.ApplicationUtils;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.HASHCODE;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.INPUT_STRING_PATTERN;

@Controller
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class CaseDocumentAmController implements CaseDocumentAm {

    private static final Logger LOG = LoggerFactory.getLogger(CaseDocumentAmController.class);

    private transient DocumentManagementService  documentManagementService;

    @Autowired
    public CaseDocumentAmController(DocumentManagementService documentManagementService) {
        this.documentManagementService = documentManagementService;
    }

    @Override
    public ResponseEntity<Object> deleteDocumentbyDocumentId(
        @ApiParam(value = "Service Auth (S2S). Use it when accessing the API on App Tier level.", required = true)
        @RequestHeader(value = "ServiceAuthorization", required = true) String serviceAuthorization,

        @ApiParam("Authorization header of the currently authenticated user")
        @RequestHeader(value = "Authorization", required = true) String authorization,

        @ApiParam("documentId")
        @PathVariable("documentId") UUID documentId,

        @ApiParam("user-id of the currently authenticated user. If provided will be used to populate the creator field of a document"
            + " and will be used for authorisation.")
        @RequestHeader(value = "user-id", required = false) String userId,

        @ApiParam("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "user-roles", required = false) String userRoles,

        @ApiParam("permanent delete flag")
        @Valid @RequestParam(value = "permanent", required = false, defaultValue = "false") Boolean permanent) {

        ResponseEntity responseEntity = documentManagementService.getDocumentMetadata(documentId);
        if (documentManagementService.checkUserPermission(responseEntity, documentId, authorization, Permission.UPDATE)) {
            return  documentManagementService.deleteDocument(documentId, userId, userRoles, permanent);

        }
        LOG.error("User doesn't have update permission on requested document " + HttpStatus.FORBIDDEN);
        throw new ForbiddenException(documentId.toString());
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
        @RequestHeader(value = "user-id", required = false) String userId,

        @ApiParam("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "user-roles", required = false) String userRoles) {

        ResponseEntity documentMetadata = documentManagementService.getDocumentMetadata(documentId);
        if (documentManagementService.checkUserPermission(documentMetadata, documentId, authorization, Permission.READ)) {
            return documentManagementService.getDocumentBinaryContent(documentId);

        }
        LOG.error("User doesn't have read permission on requested document " + HttpStatus.FORBIDDEN);
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
        if (documentManagementService.checkUserPermission(responseEntity, documentId, authorization, Permission.READ)) {
            return  ResponseEntity
                 .status(HttpStatus.OK)
                 .body(responseEntity.getBody());
        }
        LOG.error("User doesn't have read permission on requested document " + HttpStatus.FORBIDDEN);
        throw new ForbiddenException(documentId.toString());
    }

    @Override
    public ResponseEntity<Object> patchDocumentbyDocumentId(

        @ApiParam(value = "", required = true)
        @Valid UpdateDocumentCommand body,

        @ApiParam(value = "Service Auth (S2S). Use it when accessing the API on App Tier level.", required = true)
        @RequestHeader(value = "ServiceAuthorization", required = true) String serviceAuthorization,

        @ApiParam("Authorization header of the currently authenticated user")
        @RequestHeader(value = "Authorization", required = true) String authorization,

        @ApiParam("documentId")
        @PathVariable("documentId") UUID documentId,

        @ApiParam("user-id of the currently authenticated user. If provided will be used to populate the creator field of a document"
            + " and will be used for authorisation.")
        @RequestHeader(value = "user-id", required = false) String userId,

        @ApiParam("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "user-roles", required = false) String userRoles) {

        ResponseEntity responseEntity = documentManagementService.getDocumentMetadata(documentId);
        if (documentManagementService.checkUserPermission(responseEntity, documentId, authorization, Permission.UPDATE)) {
            ResponseEntity response =   documentManagementService.patchDocument(documentId, body, userId, userRoles);
            return  ResponseEntity.status(HttpStatus.OK).body(response.getBody());
        }
        LOG.error("User doesn't have update permission on requested document " + HttpStatus.FORBIDDEN);
        throw new ForbiddenException(documentId.toString());
    }

    @Override
    public ResponseEntity<Object> patchMetaDataOnDocuments(

        @ApiParam(value = "", required = true)
        @Valid @RequestBody DocumentMetadata caseDocumentMetadata,

        @ApiParam(value = "Service Auth (S2S). Use it when accessing the API on App Tier level.", required = true)
        @RequestHeader(value = "ServiceAuthorization", required = true) String serviceAuthorization,

        @ApiParam("User-Id of the currently authenticated user. If provided will be used to populate the creator field of a document"
            + " and will be used for authorisation.")
        @RequestHeader(value = "user-id", required = false) String userId,

        @ApiParam("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "user-roles", required = false) String userRoles) {

        try {
            documentManagementService.patchDocumentMetadata(caseDocumentMetadata, serviceAuthorization, userId);
        } catch (Exception e) {
            LOG.error("Exception while attaching the documents to a case :" + e);
            throw e;
        }
        return new ResponseEntity<Object>(HttpStatus.OK);
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
            LOG.error("Exception while uploading the documents :" + e);
            throw new BadRequestException("Exception while uploading the documents :" + e);
        } catch (Exception e) {
            LOG.error("Exception while uploading the documents :" + e);
            throw new ResponseFormatException("Exception while uploading the documents :" + e);
        }
    }

    @Override
    public ResponseEntity<Object> generateHashCode(
        @ApiParam(value = Constants.S2S_API_PARAM, required = true)
        @RequestHeader(value = Constants.SERVICE_AUTHORIZATION, required = true) String serviceAuthorization,

        @ApiParam("Authorization header of the currently authenticated user")
        @RequestHeader(value = "Authorization", required = true) String authorization,

        @ApiParam("documentId")
        @PathVariable("documentId") UUID documentId,

        @ApiParam(value = "CaseType identifier for the case document.", required = true)
        @NotNull(message = "Provide the Case Type ID ")
        @RequestHeader(value = "caseTypeId", required = true) String caseTypeId,

        @ApiParam(value = "Jurisdiction identifier for the case document.", required = true)
        @NotNull(message = "Provide the Jurisdiction ID ")
        @RequestHeader(value = "jurisdictionId", required = true) String jurisdictionId) {

        try {
            ValidationService.validateInputParams(INPUT_STRING_PATTERN, documentId.toString(), caseTypeId, jurisdictionId);

            HashMap<String, String> responseBody = new HashMap<>();

            String hashedToken = ApplicationUtils.generateHashCode(documentId.toString().concat(jurisdictionId).concat(caseTypeId));
            responseBody.put(HASHCODE, hashedToken);

            return new ResponseEntity<>(responseBody, HttpStatus.OK);

        } catch (BadRequestException | IllegalArgumentException e) {
            LOG.error("Illegal argument exception: " + e);
            throw new BadRequestException("Illegal argument exception:" + e);
        } catch (Exception e) {
            LOG.error("Exception :" + e);
            throw new ResponseFormatException("Exception :" + e);
        }
    }
}
