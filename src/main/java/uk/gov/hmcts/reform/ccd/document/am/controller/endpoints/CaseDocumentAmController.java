
package uk.gov.hmcts.reform.ccd.document.am.controller.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResourceCollection;
import uk.gov.hmcts.reform.ccd.document.am.model.UpdateDocumentCommand;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.document.am.service.DocumentManagementService;
import uk.gov.hmcts.reform.ccd.document.am.service.common.ValidationService;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.APPLICATION_JSON;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.BAD_REQUEST;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CASE_DOCUMENT_HASH_TOKEN_INVALID;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CASE_DOCUMENT_ID_INVALID;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CASE_DOCUMENT_NOT_FOUND;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CASE_ID_NOT_VALID;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CASE_TYPE_ID_INVALID;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.CLASSIFICATION_ID_INVALID;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.HASHTOKEN;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.INPUT_STRING_PATTERN;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.JURISDICTION_ID_INVALID;

@Api(value = "cases")
@RestController
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class CaseDocumentAmController  {

    private static final Logger LOG = LoggerFactory.getLogger(CaseDocumentAmController.class);

    private DocumentManagementService  documentManagementService;

    @Value("${idam.s2s-auth.totp_secret}")
    protected String salt;

    @Autowired
    public CaseDocumentAmController(DocumentManagementService documentManagementService) {
        this.documentManagementService = documentManagementService;
    }


    //**************** Document MetaData  API ***********
    @GetMapping(
        path = "/cases/documents/{documentId}",
        produces = {APPLICATION_JSON
        })
    @ApiOperation("Retrieves JSON representation of a Stored Document.")
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
        @PathVariable("documentId") UUID documentId) {

        ValidationService.validateDocumentId(documentId.toString());
        ResponseEntity responseEntity = documentManagementService.getDocumentMetadata(documentId);
        if (documentManagementService.checkUserPermission(responseEntity, documentId,  Permission.READ)) {
            return  ResponseEntity
                .status(HttpStatus.OK)
                .body(responseEntity.getBody());
        }
        LOG.error("User doesn't have read permission on requested document {}", HttpStatus.FORBIDDEN);
        throw new ForbiddenException(documentId.toString());
    }


    //**************** Binary content API ***********
    @GetMapping(
        path = "/cases/documents/{documentId}/binary",
        produces = {APPLICATION_JSON
        })
    @ApiOperation("Streams contents of the most recent Document associated with the Case Document.")
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
        @PathVariable("documentId") UUID documentId) {
        ValidationService.validateDocumentId(documentId.toString());
        ResponseEntity documentMetadata = documentManagementService.getDocumentMetadata(documentId);
        if (documentManagementService.checkUserPermission(documentMetadata, documentId, Permission.READ)) {
            return documentManagementService.getDocumentBinaryContent(documentId);

        }
        LOG.error("User doesn't have read permission on requested document {}", HttpStatus.FORBIDDEN);
        throw new ForbiddenException(documentId.toString());
    }


    //**************** Upload Documents  API ***************
    @PostMapping(
        path = "/cases/documents",
        produces = {APPLICATION_JSON},
        consumes = {"multipart/form-data"}
        )
    @ApiOperation("creates a list of stored document by uploading a list of binary/text file")
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
        @RequestParam(value = "files", required = true) List<MultipartFile> files,

        @ApiParam(value = "Security classification for the file", required = true)
        @Valid
        @NotNull(message = "Please provide classification")
        @RequestParam(value = "classification", required = true) String classification,

        @ApiParam(value = "CaseType identifier for the case document.", required = true)
        @NotNull(message = "Provide the Case Type ID ")
        @RequestParam(value = "caseTypeId", required = true) String caseTypeId,

        @ApiParam(value = "Jurisdiction identifier for the case document.", required = true)
        @NotNull(message = "Provide the Jurisdiction ID ")
        @RequestParam(value = "jurisdictionId", required = true) String jurisdictionId
    ) {

        ValidationService.validateInputParams(INPUT_STRING_PATTERN, caseTypeId, jurisdictionId, classification);
        ValidationService.isValidSecurityClassification(classification);
        ValidationService.validateLists(files);

        return documentManagementService.uploadDocuments(files, classification, caseTypeId, jurisdictionId);
    }


    //**************** Patch Document by DocumentId  API ***************
    @PatchMapping(
        path = "/cases/documents/{documentId}",
        produces = {APPLICATION_JSON},
        consumes = {APPLICATION_JSON}
        )
    @ApiOperation("Updates ttl on document ")
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
        @ApiParam(value = "", required = true)
        @Valid @RequestBody UpdateDocumentCommand body,
        @PathVariable("documentId") UUID documentId) {

        ValidationService.validateDocumentId(documentId.toString());

        ResponseEntity response =   documentManagementService.patchDocument(documentId, body);
        return  ResponseEntity.status(HttpStatus.OK).body(response.getBody());
    }


    //**************** Patch Meta Data on Documents  API ***************
    @PatchMapping(
        path = "/cases/documents/attachToCase",
        produces = {APPLICATION_JSON},
        consumes = {APPLICATION_JSON}
        )
    @ApiOperation("Updates a list of case document with provided metadata")
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
        @ApiParam(value = "", required = true)
        @Valid @RequestBody CaseDocumentsMetadata caseDocumentsMetadata) {

        if (!ValidationService.validate(caseDocumentsMetadata.getCaseId())) {
            throw new BadRequestException(CASE_ID_NOT_VALID);
        }

        if (caseDocumentsMetadata.getDocumentHashTokens() != null) {
            caseDocumentsMetadata.getDocumentHashTokens()
                .forEach(document -> {
                    ValidationService.validateDocumentId(document.getId());
                });

            documentManagementService.patchDocumentMetadata(caseDocumentsMetadata);

            return ResponseEntity
                .status(HttpStatus.OK)
                .body("Success");
        } else {
            throw new BadRequestException(BAD_REQUEST);
        }

    }


    //******************** Delete API ************
    @DeleteMapping(
        path = "/cases/documents/{documentId}",
        produces = {APPLICATION_JSON}
    )
    @ApiOperation("Deletes a case document with service authorization.")
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
        @Valid @RequestParam(value = "permanent", required = false, defaultValue = "false")
            Boolean permanent) {

        ValidationService.validateDocumentId(documentId.toString());

        return  documentManagementService.deleteDocument(documentId, permanent);
    }



    //**************** Generate Hash Token API  API ***************
    @GetMapping(
        path = "/cases/documents/{documentId}/token",
        produces = {APPLICATION_JSON}
        )
    @ApiOperation("Retrieves the hashcode for document Id")
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

    public ResponseEntity<Object> generateHashCode(
        @PathVariable("documentId") UUID documentId) {

        ValidationService.validateDocumentId(documentId.toString());

        HashMap<String, String> responseBody = new HashMap<>();

        responseBody.put(HASHTOKEN, documentManagementService.generateHashToken(documentId));

        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }
}
