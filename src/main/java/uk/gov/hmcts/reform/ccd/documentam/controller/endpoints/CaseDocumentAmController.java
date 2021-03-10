package uk.gov.hmcts.reform.ccd.documentam.controller.endpoints;

import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.APPLICATION_JSON;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.BAD_REQUEST;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_DOCUMENT_HASH_TOKEN_INVALID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_DOCUMENT_ID_INVALID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_DOCUMENT_NOT_FOUND;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_ID_NOT_VALID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_TYPE_ID_INVALID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CLASSIFICATION_ID_INVALID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.HASHTOKEN;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.INPUT_STRING_PATTERN;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.JURISDICTION_ID_INVALID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.SERVICE_PERMISSION_ERROR;
import static uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils.SERVICE_AUTHORIZATION;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.documentam.model.StoredDocumentHalResourceCollection;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentCommand;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.ccd.documentam.service.DocumentManagementService;
import uk.gov.hmcts.reform.ccd.documentam.service.ValidationUtils;

@Api(value = "cases")
@RestController
@Slf4j
public class CaseDocumentAmController {

    private final DocumentManagementService documentManagementService;
    private final ValidationUtils validationUtils;
    private final SecurityUtils securityUtils;

    @Autowired
    public CaseDocumentAmController(DocumentManagementService documentManagementService,
                                    ValidationUtils validationUtils, SecurityUtils securityUtils) {
        this.documentManagementService = documentManagementService;
        this.validationUtils = validationUtils;
        this.securityUtils = securityUtils;
    }

    /*********** Document MetaData API. ***********/
    @GetMapping(
        path = "/cases/documents/{documentId}",
        produces = {APPLICATION_JSON
        })
    @ApiOperation(value = "Retrieves JSON representation of a Stored Document.", tags = "get")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = StoredDocumentHalResource.class
        ),
        @ApiResponse(
            code = 400,
            message = CASE_DOCUMENT_ID_INVALID
        ),
        @ApiResponse(
            code = 404,
            message = CASE_DOCUMENT_NOT_FOUND
        )
    })
    public ResponseEntity<Object> getDocumentbyDocumentId(
        @PathVariable("documentId") UUID documentId,
        @ApiParam(value = "S2S JWT token for an approved micro-service", required = true)
        @RequestHeader(SERVICE_AUTHORIZATION) String s2sToken
    ) {
        validationUtils.validateDocumentId(documentId.toString());
        ResponseEntity<?> responseEntity = documentManagementService.getDocumentMetadata(documentId);
        if (documentManagementService.checkServicePermission(responseEntity,
                                                             getServiceNameFromS2SToken(s2sToken),
                                                             Permission.READ)) {
            if (documentManagementService.checkUserPermission(responseEntity, documentId, Permission.READ)) {
                return ResponseEntity.status(HttpStatus.OK).body(responseEntity.getBody());
            }
            log.error("User doesn't have read permission on requested document {}", HttpStatus.FORBIDDEN);
            throw new ForbiddenException(documentId.toString());
        }
        log.error(SERVICE_PERMISSION_ERROR, HttpStatus.FORBIDDEN);
        throw new ForbiddenException(documentId.toString());
    }

    /*********** Binary content API. ***********/
    @GetMapping(
        path = "/cases/documents/{documentId}/binary",
        produces = {APPLICATION_JSON
        })
    @ApiOperation(value = "Streams contents of the most recent Document associated with the Case Document.", tags =
        "get")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "OK",
            response = Object.class
        ),
        @ApiResponse(
            code = 400,
            message = CASE_DOCUMENT_ID_INVALID
        ),
        @ApiResponse(
            code = 404,
            message = CASE_DOCUMENT_NOT_FOUND
        )
    })
    public ResponseEntity<Object> getDocumentBinaryContentbyDocumentId(
        @PathVariable("documentId") UUID documentId,
        @ApiParam(value = "S2S JWT token for an approved micro-service", required = true)
        @RequestHeader(SERVICE_AUTHORIZATION) String s2sToken
    ) {
        validationUtils.validateDocumentId(documentId.toString());
        ResponseEntity<?> documentMetadata = documentManagementService.getDocumentMetadata(documentId);
        if (documentManagementService.checkServicePermission(documentMetadata, getServiceNameFromS2SToken(s2sToken),
                                                              Permission.READ)) {
            if (documentManagementService.checkUserPermission(documentMetadata, documentId, Permission.READ)) {
                return documentManagementService.getDocumentBinaryContent(documentId);
            }
            log.error("User doesn't have read permission on requested document {}", HttpStatus.FORBIDDEN);
            throw new ForbiddenException(documentId.toString());
        }
        log.error(SERVICE_PERMISSION_ERROR, HttpStatus.FORBIDDEN);
        throw new ForbiddenException(documentId.toString());
    }

    /*********** Upload Documents API. ***********/
    @PostMapping(
        path = "/cases/documents",
        produces = {APPLICATION_JSON},
        consumes = {"multipart/form-data"}
    )
    @ApiOperation(value = "creates a list of stored document by uploading a list of binary/text file", tags = "upload")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Created",
            response = StoredDocumentHalResourceCollection.class
        ),
        @ApiResponse(
            code = 400,
            message = CASE_TYPE_ID_INVALID
        ),
        @ApiResponse(
            code = 400,
            message = JURISDICTION_ID_INVALID
        ),
        @ApiResponse(
            code = 400,
            message = CLASSIFICATION_ID_INVALID
        )
    })
    public ResponseEntity<Object> uploadDocuments(

        @ApiParam(value = "List of file to be uploaded", required = true)
        @NotNull(message = "Provide some file to be uploaded.")
        @Size(min = 1, message = "Please provide atleast one file to be uploaded.")
        @RequestParam(value = "files") List<MultipartFile> files,

        @ApiParam(value = "Security classification for the file", required = true)
        @Valid
        @NotNull(message = "Please provide classification")
        @RequestParam(value = "classification") String classification,

        @ApiParam(value = "CaseType identifier for the case document.", required = true)
        @NotNull(message = "Provide the Case Type ID ")
        @RequestParam(value = "caseTypeId") String caseTypeId,

        @ApiParam(value = "Jurisdiction identifier for the case document.", required = true)
        @NotNull(message = "Provide the Jurisdiction ID ")
        @RequestParam(value = "jurisdictionId") String jurisdictionId,
        @ApiParam(value = "S2S JWT token for an approved micro-service", required = true)
        @RequestHeader(SERVICE_AUTHORIZATION) String s2sToken
    ) {
        validationUtils.validateInputParams(INPUT_STRING_PATTERN, caseTypeId, jurisdictionId, classification);
        validationUtils.isValidSecurityClassification(classification);
        validationUtils.validateLists(files);
        if (documentManagementService.checkServicePermissionsForUpload(caseTypeId, jurisdictionId,
                                                                       getServiceNameFromS2SToken(s2sToken),
                                                                       Permission.CREATE)) {
            return documentManagementService.uploadDocuments(files, classification, caseTypeId, jurisdictionId);
        }
        log.error(SERVICE_PERMISSION_ERROR, HttpStatus.FORBIDDEN);
        throw new ForbiddenException(caseTypeId + " " + jurisdictionId);
    }

    /*********** Patch Document by DocumentId API. ***********/
    @PatchMapping(
        path = "/cases/documents/{documentId}",
        produces = {APPLICATION_JSON},
        consumes = {APPLICATION_JSON}
    )
    @ApiOperation(value = "Updates ttl on document ", tags = "patch")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = StoredDocumentHalResource.class
        ),
        @ApiResponse(
            code = 400,
            message = CASE_DOCUMENT_ID_INVALID
        ),
        @ApiResponse(
            code = 404,
            message = CASE_DOCUMENT_NOT_FOUND
        )
    })
    public ResponseEntity<Object> patchDocumentbyDocumentId(
        @ApiParam(required = true)
        @Valid @RequestBody UpdateDocumentCommand body,
        @PathVariable("documentId") UUID documentId,
        @ApiParam(value = "S2S JWT token for an approved micro-service", required = true)
        @RequestHeader(SERVICE_AUTHORIZATION) String s2sToken
    ) {
        validationUtils.validateDocumentId(documentId.toString());
        ResponseEntity<?> responseEntity = documentManagementService.getDocumentMetadata(documentId);
        if (documentManagementService.checkServicePermission(responseEntity, getServiceNameFromS2SToken(s2sToken),
                                                              Permission.UPDATE)) {
            ResponseEntity<?> response = documentManagementService.patchDocument(documentId, body);
            return ResponseEntity.status(HttpStatus.OK).body(response.getBody());
        }
        log.error(SERVICE_PERMISSION_ERROR, HttpStatus.FORBIDDEN);
        throw new ForbiddenException(documentId.toString());
    }

    /*********** Patch Meta Data on Documents API. ***********/
    @PatchMapping(
        path = "/cases/documents/attachToCase",
        produces = {APPLICATION_JSON},
        consumes = {APPLICATION_JSON}
    )
    @ApiOperation(value = "Updates a list of case document with provided metadata", tags = "patch")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = StoredDocumentHalResource.class
        ),
        @ApiResponse(
            code = 400,
            message = CASE_DOCUMENT_ID_INVALID
        ),
        @ApiResponse(
            code = 400,
            message = CASE_ID_NOT_VALID
        ),
        @ApiResponse(
            code = 400,
            message = CASE_TYPE_ID_INVALID
        ),
        @ApiResponse(
            code = 400,
            message = JURISDICTION_ID_INVALID
        ),
        @ApiResponse(
            code = 400,
            message = CASE_DOCUMENT_HASH_TOKEN_INVALID
        ),
        @ApiResponse(
            code = 404,
            message = CASE_DOCUMENT_NOT_FOUND
        )
    })
    public ResponseEntity<Object> patchMetaDataOnDocuments(
        @ApiParam(required = true)
        @Valid @RequestBody CaseDocumentsMetadata caseDocumentsMetadata,
        @ApiParam(value = "S2S JWT token for an approved micro-service", required = true)
        @RequestHeader(SERVICE_AUTHORIZATION) String s2sToken
    ) {
        if (!validationUtils.validate(caseDocumentsMetadata.getCaseId())) {
            throw new BadRequestException(CASE_ID_NOT_VALID);
        }
        if (caseDocumentsMetadata.getDocumentHashTokens() != null) {
            caseDocumentsMetadata.getDocumentHashTokens()
                .forEach(document -> validationUtils.validateDocumentId(document.getId()));

            //validate the service authorization for first document in payload
            ResponseEntity<?> documentMetadata = documentManagementService.getDocumentMetadata(UUID.fromString(
                caseDocumentsMetadata.getDocumentHashTokens().get(0).getId()));

            if (documentManagementService.checkServicePermission(documentMetadata, getServiceNameFromS2SToken(s2sToken),
                                                                 Permission.ATTACH)) {
                documentManagementService.patchDocumentMetadata(caseDocumentsMetadata);
                HashMap<String, String> responseBody = new HashMap<>();
                responseBody.put("Result", "Success");
                return ResponseEntity
                    .status(HttpStatus.OK).body(responseBody);
            } else {
                log.error(SERVICE_PERMISSION_ERROR, HttpStatus.FORBIDDEN);
                throw new ForbiddenException(caseDocumentsMetadata.getCaseTypeId() + " "
                    + caseDocumentsMetadata.getJurisdictionId());
            }
        } else {
            throw new BadRequestException(BAD_REQUEST);
        }
    }

    /*********** Delete API. ***********/
    @DeleteMapping(
        path = "/cases/documents/{documentId}",
        produces = {APPLICATION_JSON}
    )
    @ApiOperation(value = "Deletes a case document with service authorization.", tags = "delete")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "OK"
        ),
        @ApiResponse(
            code = 400,
            message = CASE_DOCUMENT_ID_INVALID
        ),
        @ApiResponse(
            code = 404,
            message = CASE_DOCUMENT_NOT_FOUND
        )
    })
    public ResponseEntity<Object> deleteDocumentbyDocumentId(
        @PathVariable("documentId") UUID documentId,
        @Valid @RequestParam(value = "permanent", required = false, defaultValue = "false") Boolean permanent,
        @ApiParam(value = "S2S JWT token for an approved micro-service", required = true)
        @RequestHeader(SERVICE_AUTHORIZATION) String s2sToken
    ) {
        validationUtils.validateDocumentId(documentId.toString());
        ResponseEntity<?> responseEntity = documentManagementService.getDocumentMetadata(documentId);
        if (documentManagementService.checkServicePermission(responseEntity, getServiceNameFromS2SToken(s2sToken),
                                                             Permission.UPDATE)) {
            return documentManagementService.deleteDocument(documentId, permanent);
        }
        log.error(SERVICE_PERMISSION_ERROR, HttpStatus.FORBIDDEN);
        throw new ForbiddenException(documentId.toString());
    }

    /*********** Generate Hash Token API. ***********/
    @GetMapping(
        path = "/cases/documents/{documentId}/token",
        produces = {APPLICATION_JSON}
    )
    @ApiOperation(value = "Retrieves the hashcode for document Id", tags = "get")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = StoredDocumentHalResource.class
        ),
        @ApiResponse(
            code = 400,
            message = CASE_DOCUMENT_ID_INVALID
        ),
        @ApiResponse(
            code = 404,
            message = CASE_DOCUMENT_NOT_FOUND
        )
    })
    public ResponseEntity<Object> generateHashCode(@PathVariable("documentId") UUID documentId,
        @ApiParam(value = "S2S JWT token for an approved micro-service", required = true)
        @RequestHeader(SERVICE_AUTHORIZATION) String s2sToken
    ) {
        validationUtils.validateDocumentId(documentId.toString());
        ResponseEntity<?> responseEntity = documentManagementService.getDocumentMetadata(documentId);
        if (documentManagementService.checkServicePermission(responseEntity, getServiceNameFromS2SToken(s2sToken),
                                                             Permission.HASHTOKEN)) {
            HashMap<String, String> responseBody = new HashMap<>();
            responseBody.put(HASHTOKEN, documentManagementService.generateHashToken(documentId));
            return new ResponseEntity<>(responseBody, HttpStatus.OK);
        }
        log.error(SERVICE_PERMISSION_ERROR, HttpStatus.FORBIDDEN);
        throw new ForbiddenException(documentId.toString());
    }

    private String getServiceNameFromS2SToken(String s2sToken) {
        return securityUtils.getServiceNameFromS2SToken(s2sToken);
    }
}
