package uk.gov.hmcts.reform.ccd.documentam.controller.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.documentam.auditlog.AuditOperationType;
import uk.gov.hmcts.reform.ccd.documentam.auditlog.LogAudit;
import uk.gov.hmcts.reform.ccd.documentam.dto.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.documentam.dto.UpdateTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.dto.UploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedService;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.GeneratedHashCodeResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentMetaDataResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.ccd.documentam.service.DocumentManagementService;

import javax.validation.Valid;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.APPLICATION_JSON;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_DOCUMENT_HASH_TOKEN_INVALID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_DOCUMENT_ID_INVALID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_DOCUMENT_NOT_FOUND;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_ID_NOT_VALID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_TYPE_ID_INVALID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CLASSIFICATION_ID_INVALID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.JURISDICTION_ID_INVALID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.SERVICE_PERMISSION_ERROR;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.USER_PERMISSION_ERROR;
import static uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils.SERVICE_AUTHORIZATION;

@Api(value = "cases")
@RestController
@Slf4j
@ConditionalOnProperty(value = "case.document.am.api.enabled", havingValue = "true")
public class CaseDocumentAmController {

    private final DocumentManagementService documentManagementService;
    private final SecurityUtils securityUtils;
    private static final String TTL_FORBIDDEN_MESSAGE = "Document %s can not be downloaded as TTL has expired";

    @Autowired
    public CaseDocumentAmController(final DocumentManagementService documentManagementService,
                                    final SecurityUtils securityUtils) {
        this.documentManagementService = documentManagementService;
        this.securityUtils = securityUtils;
    }

    @GetMapping(
        path = "/cases/documents/{documentId}",
        produces = {APPLICATION_JSON
        })
    @ApiOperation(value = "Retrieves JSON representation of a Stored Document.", tags = "get")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = Document.class
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

    @LogAudit(
        operationType = AuditOperationType.DOWNLOAD_DOCUMENT_BY_ID,
        documentId = "#documentId"
    )
    public ResponseEntity<Document> getDocumentByDocumentId(
        @PathVariable("documentId") final UUID documentId,
        @ApiParam(value = "S2S JWT token for an approved micro-service", required = true)
        @RequestHeader(SERVICE_AUTHORIZATION) final String s2sToken
    ) {
        final Document document = documentManagementService.getDocumentMetadata(documentId);

        documentManagementService.checkServicePermission(
            document.getCaseTypeId(),
            document.getJurisdictionId(),
            getServiceNameFromS2SToken(s2sToken),
            Permission.READ,
            SERVICE_PERMISSION_ERROR,
            documentId.toString()
        );

        if (document.getCaseId() != null) {
            documentManagementService.checkUserPermission(
                    document.getCaseId(),
                    documentId,
                    Permission.READ,
                    USER_PERMISSION_ERROR,
                    documentId.toString());

            return ResponseEntity.ok(document);
        }

        if (ttlIsFutureDate(document.getTtl())) {
            return ResponseEntity.ok(document);
        } else {
            String errorMessage = String.format(TTL_FORBIDDEN_MESSAGE, documentId);
            log.error(errorMessage);
            throw new ForbiddenException(errorMessage);
        }
    }

    private boolean ttlIsFutureDate(Date ttl) {
        return ttl != null && ttl.after(Date.from(Instant.now()));
    }

    @GetMapping(
        path = "/_cases/documents/{documentId}/binary",
        produces = {APPLICATION_JSON
        })
    @ApiOperation(value = "Returns contents of the most recent Document associated with the Case Document.", tags =
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

    @LogAudit(
        operationType = AuditOperationType.DOWNLOAD_DOCUMENT_BINARY_CONTENT_BY_ID,
        documentId = "#documentId"
    )
    public ResponseEntity<ByteArrayResource> getDocumentBinaryContentByDocumentId(
        @PathVariable("documentId") final UUID documentId,
        @ApiParam(value = "S2S JWT token for an approved micro-service", required = true)
        @RequestHeader(SERVICE_AUTHORIZATION) final String s2sToken
    ) {
        final Document document = documentManagementService.getDocumentMetadata(documentId);

        documentManagementService.checkServicePermission(document.getCaseTypeId(),
                                                         document.getJurisdictionId(),
                                                         getServiceNameFromS2SToken(s2sToken),
                                                         Permission.READ,
                                                         SERVICE_PERMISSION_ERROR,
                                                         documentId.toString());

        if (document.getCaseId() != null) {
            documentManagementService.checkUserPermission(document.getCaseId(),
                    documentId,
                    Permission.READ,
                    USER_PERMISSION_ERROR,
                    documentId.toString());

            return documentManagementService.getDocumentBinaryContent(documentId);
        }

        if (ttlIsFutureDate(document.getTtl())) {
            return documentManagementService.getDocumentBinaryContent(documentId);
        } else {
            String errorMessage = String.format(TTL_FORBIDDEN_MESSAGE, documentId);
            log.error(errorMessage);
            throw new ForbiddenException(errorMessage);
        }
    }

    @GetMapping(
        path = "/cases/documents/{documentId}/binary",
        produces = {APPLICATION_JSON})
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

    @LogAudit(
        operationType = AuditOperationType.DOWNLOAD_STREAMED_DOCUMENT_BINARY_CONTENT_BY_ID,
        documentId = "#documentId"
    )
    public ResponseEntity<InputStreamResource> streamDocumentBinaryContentByDocumentId(
        @PathVariable("documentId") final UUID documentId,
        @ApiParam(value = "S2S JWT token for an approved micro-service", required = true)
        @RequestHeader(SERVICE_AUTHORIZATION) final String s2sToken) {
        final Document document = documentManagementService.getDocumentMetadata(documentId);

        documentManagementService.checkServicePermission(
                        document.getCaseTypeId(),
                        document.getJurisdictionId(),
                        getServiceNameFromS2SToken(s2sToken),
                        Permission.READ,
                        SERVICE_PERMISSION_ERROR,
                        documentId.toString());

        if (document.getCaseId() != null) {
            documentManagementService.checkUserPermission(
                        document.getCaseId(),
                        documentId,
                        Permission.READ,
                        USER_PERMISSION_ERROR,
                        documentId.toString());

            return documentManagementService.streamDocumentBinaryContent(documentId);
        }

        if (ttlIsFutureDate(document.getTtl())) {
            return documentManagementService.streamDocumentBinaryContent(documentId);
        } else {
            String errorMessage = String.format(TTL_FORBIDDEN_MESSAGE, documentId);
            log.error(errorMessage);
            throw new ForbiddenException(errorMessage);
        }
    }

    @PostMapping(
        path = "/cases/documents",
        produces = {APPLICATION_JSON},
        consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}
    )
    @ApiOperation(value = "creates a list of stored document by uploading a list of binary/text file", tags = "upload")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Created",
            response = UploadResponse.class
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

    @LogAudit(
        operationType = AuditOperationType.UPLOAD_DOCUMENTS,
        caseType = "#documentUploadRequest.caseTypeId",
        jurisdiction = "#documentUploadRequest.jurisdictionId"
    )
    public UploadResponse uploadDocuments(
        @ApiParam(value = "List of documents to be uploaded and their metadata", required = true)
        @Valid final DocumentUploadRequest documentUploadRequest,

        final BindingResult bindingResult,

        @ApiParam(value = "S2S JWT token for an approved micro-service", required = true)
        @RequestHeader(SERVICE_AUTHORIZATION) final String s2sToken) {

        handleErrors(bindingResult);

        final String permissionFailureMessage = documentUploadRequest.getCaseTypeId() + " "
            + documentUploadRequest.getJurisdictionId();

        documentManagementService.checkServicePermission(documentUploadRequest.getCaseTypeId(),
                                                         documentUploadRequest.getJurisdictionId(),
                                                         getServiceNameFromS2SToken(s2sToken),
                                                         Permission.CREATE,
                                                         SERVICE_PERMISSION_ERROR,
                                                         permissionFailureMessage);

        return documentManagementService.uploadDocuments(documentUploadRequest);
    }

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
            response = PatchDocumentResponse.class
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

    @LogAudit(
        operationType = AuditOperationType.PATCH_DOCUMENT_BY_DOCUMENT_ID,
        documentId = "#documentId"
    )
    public ResponseEntity<PatchDocumentResponse> patchDocumentByDocumentId(
        @PathVariable("documentId") final UUID documentId,

        @ApiParam(value = "", required = true)
        @Valid @RequestBody final UpdateTtlRequest ttlRequest,

        @ApiParam(value = "S2S JWT token for an approved micro-service", required = true)
        @RequestHeader(SERVICE_AUTHORIZATION) final String s2sToken) {

        final Document document = documentManagementService.getDocumentMetadata(documentId);

        documentManagementService.checkServicePermission(
            document.getCaseTypeId(),
            document.getJurisdictionId(),
            getServiceNameFromS2SToken(s2sToken),
            Permission.UPDATE,
            SERVICE_PERMISSION_ERROR,
            documentId.toString()
        );

        final PatchDocumentResponse patchDocumentResponse = documentManagementService.patchDocument(
            documentId,
            ttlRequest
        );

        return ResponseEntity.ok(patchDocumentResponse);
    }

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
            response = PatchDocumentMetaDataResponse.class
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
    @LogAudit(
        operationType = AuditOperationType.PATCH_METADATA_ON_DOCUMENTS,
        documentIds = "T(uk.gov.hmcts.reform.ccd.documentam.util.DocumentIdsExtractor)"
            + ".extractIds(#caseDocumentsMetadata.documentHashTokens)",
        caseId = "#caseDocumentsMetadata.caseId"
    )
    public ResponseEntity<PatchDocumentMetaDataResponse> patchMetaDataOnDocuments(
        @ApiParam(value = "", required = true)
        @Valid @RequestBody final CaseDocumentsMetadata caseDocumentsMetadata,

        @ApiParam(value = "S2S JWT token for an approved micro-service", required = true)
        @RequestHeader(SERVICE_AUTHORIZATION) final String s2sToken) {

        documentManagementService.checkServicePermission(
            caseDocumentsMetadata.getCaseTypeId(),
            caseDocumentsMetadata.getJurisdictionId(),
            getServiceNameFromS2SToken(s2sToken),
            Permission.ATTACH,
            SERVICE_PERMISSION_ERROR,
            caseDocumentsMetadata.getCaseTypeId() + " " + caseDocumentsMetadata.getJurisdictionId());

        documentManagementService.patchDocumentMetadata(caseDocumentsMetadata);

        return ResponseEntity.ok(new PatchDocumentMetaDataResponse("Success"));
    }

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
    @LogAudit(
        operationType = AuditOperationType.DELETE_DOCUMENT_BY_DOCUMENT_ID,
        documentId = "#documentId"
    )
    public ResponseEntity<Void> deleteDocumentByDocumentId(
        @PathVariable("documentId") final UUID documentId,
        @Valid @RequestParam(value = "permanent", required = false, defaultValue = "false") final Boolean permanent,
        @ApiParam(value = "S2S JWT token for an approved micro-service", required = true)
        @RequestHeader(SERVICE_AUTHORIZATION) final String s2sToken
    ) {
        final Document document = documentManagementService.getDocumentMetadata(documentId);

        documentManagementService.checkServicePermission(document.getCaseTypeId(),
                                                         document.getJurisdictionId(),
                                                         getServiceNameFromS2SToken(s2sToken),
                                                         Permission.UPDATE,
                                                         SERVICE_PERMISSION_ERROR,
                                                         documentId.toString());

        documentManagementService.deleteDocument(documentId, permanent);

        return ResponseEntity.noContent().build();
    }

    @GetMapping(
        path = "/cases/documents/{documentId}/token",
        produces = {APPLICATION_JSON}
    )
    @ApiOperation(value = "Retrieves the hashcode for document Id", tags = "get")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = GeneratedHashCodeResponse.class
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
    @LogAudit(
        operationType = AuditOperationType.GENERATE_HASH_CODE,
        documentId = "#documentId"
    )
    public ResponseEntity<GeneratedHashCodeResponse> generateHashCode(
        @PathVariable("documentId") final UUID documentId,
        @ApiParam(value = "S2S JWT token for an approved micro-service", required = true)
        @RequestHeader(SERVICE_AUTHORIZATION) final String s2sToken
    ) {
        final Document document = documentManagementService.getDocumentMetadata(documentId);

        AuthorisedService authorisedService = documentManagementService
            .checkServicePermission(
                document.getCaseTypeId(),
                document.getJurisdictionId(),
                getServiceNameFromS2SToken(s2sToken),
                Permission.HASHTOKEN,
                SERVICE_PERMISSION_ERROR,
                documentId.toString()
            );

        return ResponseEntity.ok(GeneratedHashCodeResponse.builder()
                                     .hashToken(documentManagementService.generateHashToken(
                                         documentId,
                                         authorisedService,
                                         Permission.HASHTOKEN
                                     ))
                                     .build());
    }

    private String getServiceNameFromS2SToken(String s2sToken) {
        return securityUtils.getServiceNameFromS2SToken(s2sToken);
    }

    private void handleErrors(final BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            final String message = bindingResult.getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .findFirst()
                .orElse(null);

            throw new BadRequestException(message);
        }
    }
}
