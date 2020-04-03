
package uk.gov.hmcts.reform.ccd.document.am.controller.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ResponseFormatException;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResourceCollection;
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

import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.APPLICATION_JSON;

import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.HASHCODE;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.INPUT_CASE_ID_PATTERN;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.INPUT_STRING_PATTERN;

@Api(value = "cases")
@RestController
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class CaseDocumentAmController  {

    private static final Logger LOG = LoggerFactory.getLogger(CaseDocumentAmController.class);

    private DocumentManagementService  documentManagementService;

    @Autowired
    public CaseDocumentAmController(DocumentManagementService documentManagementService) {
        this.documentManagementService = documentManagementService;
    }

    //******************** Delete API ************
    @ApiOperation(value = "Deletes a Case Document.")
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "No Content")
       })
    @DeleteMapping(value = "/cases/documents/{documentId}",
            produces = {APPLICATION_JSON})
    public ResponseEntity<Object> deleteDocumentbyDocumentId(
        @PathVariable("documentId") UUID documentId,
        @Valid @RequestParam(value = "permanent", required = false, defaultValue = "false") Boolean permanent) {
        ResponseEntity responseEntity = documentManagementService.getDocumentMetadata(documentId);
        if (documentManagementService.checkUserPermission(responseEntity, documentId, Permission.UPDATE)) {
            return  documentManagementService.deleteDocument(documentId, permanent);

        }
        LOG.error("User doesn't have update permission on requested document {}",  HttpStatus.FORBIDDEN);
        throw new ForbiddenException(documentId.toString());
    }


    //**************** Binary content API ***********
    @ApiOperation(value = "Streams contents of the most recent Document Content Version associated with the Case Document."
         )
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Returns contents of a file", response = Object.class),
       })
    @GetMapping(value = "/cases/documents/{documentId}/binary", produces = {APPLICATION_JSON})
    public ResponseEntity<Object> getDocumentBinaryContentbyDocumentId(
        @PathVariable("documentId") UUID documentId) {

        ResponseEntity documentMetadata = documentManagementService.getDocumentMetadata(documentId);
        if (documentManagementService.checkUserPermission(documentMetadata, documentId, Permission.READ)) {
            return documentManagementService.getDocumentBinaryContent(documentId);

        }
        LOG.error("User doesn't have read permission on requested document {}", HttpStatus.FORBIDDEN);
        throw new ForbiddenException(documentId.toString());
    }

    //**************** Document MetaData  API ***********

    @ApiOperation("Retrieves JSON representation of a Stored Document.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Success", response = StoredDocumentHalResource.class)
    })
    @GetMapping(value = "/cases/documents/{documentId}", produces = {APPLICATION_JSON})
    public ResponseEntity<Object> getDocumentbyDocumentId(@PathVariable("documentId") UUID documentId) {
        ValidationService.validateInputParams(INPUT_STRING_PATTERN, documentId.toString());
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

    //**************** Patch Document by DocumentId  API ***************

    @ApiOperation(value = "Updates document instance (ex. ttl).")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Success", response = StoredDocumentHalResource.class)})
    @PatchMapping(value = "/cases/documents/{documentId}",
        produces = {APPLICATION_JSON},
        consumes = {APPLICATION_JSON})
    public ResponseEntity<Object> patchDocumentbyDocumentId(
        @ApiParam(value = "", required = true)
        @Valid @RequestBody UpdateDocumentCommand body,
        @PathVariable("documentId") UUID documentId) {
        ResponseEntity responseEntity = documentManagementService.getDocumentMetadata(documentId);
        if (documentManagementService.checkUserPermission(responseEntity, documentId, Permission.UPDATE)) {
            ResponseEntity response =   documentManagementService.patchDocument(documentId, body);
            return  ResponseEntity.status(HttpStatus.OK).body(response.getBody());
        }
        LOG.error("User doesn't have update permission on requested document {}", HttpStatus.FORBIDDEN);
        throw new ForbiddenException(documentId.toString());
    }


    //**************** Patch Meta Data on Documents  API ***************

    @ApiOperation(value = "Updates a list of Case Documents with provided Metadata")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Created", response = StoredDocumentHalResource.class),
        @ApiResponse(code = 204, message = "No Content")})
    @PatchMapping(value = "/cases/documents/attachToCase",
        produces = {APPLICATION_JSON},
        consumes = {APPLICATION_JSON})
    public ResponseEntity<Object> patchMetaDataOnDocuments(
        @ApiParam(value = "", required = true)
        @Valid @RequestBody DocumentMetadata caseDocumentMetadata) {

        try {
            if (!ValidationService.validate(caseDocumentMetadata.getCaseId())) {
                throw new BadRequestException("The Case Id is invalid");
            }
            ValidationService.validateInputParams(INPUT_CASE_ID_PATTERN, caseDocumentMetadata.getCaseId());
            caseDocumentMetadata.getDocuments()
                                .forEach(document -> {
                                    ValidationService.validateInputParams(INPUT_STRING_PATTERN, document.getId());
                                    ValidationService.validateDocumentId(document.getId());
                                });

            documentManagementService.patchDocumentMetadata(caseDocumentMetadata);
        } catch (BadRequestException | IllegalArgumentException e) {
            throw new BadRequestException("Exception while attaching the documents to a case :" + e);
        } catch (Exception e) {
            LOG.error("Exception in controller for patch MetaData Documents API");
            throw e;
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    //**************** Upload Documents  API ***************

    @ApiOperation(value = "Creates a list of Stored Documents by uploading a list of binary/text files")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Created", response = StoredDocumentHalResourceCollection.class),
        @ApiResponse(code = 204, message = "No Content")})
    @PostMapping(value = "/cases/documents",
        produces = {APPLICATION_JSON},
        consumes = {"multipart/form-data"})
    public ResponseEntity<Object> uploadDocuments(

        @ApiParam(value = "", required = true)
        @NotNull(message = "Provide some files to be uploaded.")
        @Size(min = 1, message = "Please provide at least one file to be uploaded.")
        @RequestParam(value = "files", required = true) List<MultipartFile> files,

        @ApiParam(value = "", required = true)
        @Valid
        @NotNull(message = "Please provide classification")
        @RequestParam(value = "classification", required = true) String classification,

        @RequestParam(value = "roles", required = false) List<String> roles,

        @ApiParam(value = "CaseType identifier for the case document.", required = true)
        @NotNull(message = "Provide the Case Type ID ")
        @RequestHeader(value = "caseTypeId", required = true) String caseTypeId,

        @ApiParam(value = "Jurisdiction identifier for the case document.", required = true)
        @NotNull(message = "Provide the Jurisdiction ID ")
        @RequestHeader(value = "jurisdictionId", required = true) String jurisdictionId) {

        try {
            ValidationService.validateInputParams(INPUT_STRING_PATTERN, caseTypeId, jurisdictionId, classification);
            ValidationService.isValidSecurityClassification(classification);
            ValidationService.validateLists(files, roles);
            roles.forEach(role -> ValidationService.validateInputParams(INPUT_STRING_PATTERN, role));

            return documentManagementService.uploadDocuments(files, classification, roles,
                                                              caseTypeId, jurisdictionId);
        } catch (BadRequestException | IllegalArgumentException e) {
            throw new BadRequestException("Exception while uploading the documents :" + e);
        } catch (Exception e) {
            throw new ResponseFormatException("Exception while uploading the documents :" + e);
        }
    }

    //**************** Generate Hash Token API  API ***************

    @ApiOperation(value = "Retrieves the hashcode for document Id")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Success", response = StoredDocumentHalResource.class)})
    @GetMapping(value = "/cases/documents/{documentId}/token", produces = {APPLICATION_JSON})
    public ResponseEntity<Object> generateHashCode(
        @PathVariable("documentId") UUID documentId) {
        StoredDocumentHalResource resource = new StoredDocumentHalResource();
        ResponseEntity responseEntity = documentManagementService.getDocumentMetadata(documentId);
        if (responseEntity.getStatusCode().equals(HttpStatus.OK) && null != responseEntity.getBody()) {
            resource = (StoredDocumentHalResource) responseEntity.getBody();
        }

        ValidationService.validateInputParams(INPUT_STRING_PATTERN, documentId.toString(),
                                              resource.getMetadata().get("caseTypeId"), resource.getMetadata().get("jurisdictionId"));

        HashMap<String, String> responseBody = new HashMap<>();

        String hashedToken = ApplicationUtils.generateHashCode(documentId.toString().concat(
            resource.getMetadata().get("jurisdictionId")).concat(resource.getMetadata().get("caseTypeId")));
        responseBody.put(HASHCODE, hashedToken);

        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }
}
