package uk.gov.hmcts.reform.ccd.document.am.controller.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.ErrorMap;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResourceCollection;
import uk.gov.hmcts.reform.ccd.document.am.model.UpdateDocumentCommand;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.APPLICATION_JSON;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.AUTHORIZATION;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.AUTHORIZATION_DESCRIPTION;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.BAD_REQUEST;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.FORBIDDEN;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.RESOURCE_NOT_FOUND;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.S2S_API_PARAM;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.TAG;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.UNAUTHORIZED;

@Api(value = "cases")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface CaseDocumentAm {

    @ApiOperation(value = "Deletes a Case Document.", nickname = "deleteDocumentbyDocumentId",
                  notes = "This API will be the single point of reference for deleting any case related documents from doc-store.",
                  response = String.class, tags = {TAG})
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "No Content"),
        @ApiResponse(code = 400, message = BAD_REQUEST, response = ErrorMap.class),
        @ApiResponse(code = 401, message = UNAUTHORIZED, response = ErrorMap.class),
        @ApiResponse(code = 403, message = FORBIDDEN, response = ErrorMap.class),
        @ApiResponse(code = 404, message = RESOURCE_NOT_FOUND, response = ErrorMap.class)})
    @DeleteMapping(value = "/cases/documents/{documentId}",
                    produces = {APPLICATION_JSON})
    ResponseEntity<Object> deleteDocumentbyDocumentId(
        @ApiParam(value = S2S_API_PARAM, required = true)
        @RequestHeader(value = SERVICE_AUTHORIZATION, required = true) String serviceAuthorization,

        @ApiParam(value = AUTHORIZATION_DESCRIPTION, required = true)
        @RequestHeader(value = AUTHORIZATION, required = true) String authorization,

        @ApiParam(value = "documentId", required = true)
        @PathVariable("documentId") UUID documentId,

        @ApiParam("user-id of the currently authenticated user. If provided will be used to populate the creator field of a document"
            + " and will be used for authorisation.")
        @RequestHeader(value = "user-id", required = false) String userId,

        @ApiParam("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "user-roles", required = false) String userRoles,

        @ApiParam("permanent delete flag")
        @Valid @RequestParam("permanent") Boolean permanent);


    @ApiOperation(value = "Streams contents of the most recent Document Content Version associated with the Case Document.",
                  nickname = "getDocumentBinaryConetentbyDocumentId",
                  notes = "This API will be the single point of reference for downloading any case related documents from doc-store. It will retrieve the "
                          + "binary content of the requested document.",
                  response = Object.class, tags = {TAG})
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Returns contents of a file", response = Object.class),
        @ApiResponse(code = 400, message = BAD_REQUEST, response = ErrorMap.class),
        @ApiResponse(code = 401, message = UNAUTHORIZED, response = ErrorMap.class),
        @ApiResponse(code = 403, message = FORBIDDEN, response = ErrorMap.class),
        @ApiResponse(code = 404, message = RESOURCE_NOT_FOUND, response = ErrorMap.class)})
    @GetMapping(value = "/cases/documents/{documentId}/binary", produces = {APPLICATION_JSON})
    ResponseEntity<Object> getDocumentBinaryContentbyDocumentId(

        @ApiParam(value = S2S_API_PARAM, required = true)
        @RequestHeader(value = SERVICE_AUTHORIZATION, required = true) String serviceAuthorization,
        @ApiParam(value = "documentId", required = true)
        @PathVariable("documentId") UUID documentId,
        @ApiParam("User-Id of the currently authenticated user. If provided will be used to populate the creator field of a document"
                          + " and will be used for authorisation.")
        @RequestHeader(value = "User-Id", required = false) String userId,
        @ApiParam("Authorization header of the currently authenticated user")
        @RequestHeader(value = "Authorization", required = true) String authorization,

        @ApiParam("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "User-Roles", required = false) String userRoles);


    @ApiOperation(value = "Retrieves JSON representation of a Case Document.", nickname = "getDocumentbyDocumentId",
                  notes = "This API will be the single point of reference for downloading any case related documents from doc-store. It will retrieve json "
                          + "representation of the document.",
                  response = StoredDocumentHalResource.class, tags = {TAG})
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Success", response = StoredDocumentHalResource.class),
        @ApiResponse(code = 401, message = UNAUTHORIZED, response = String.class),
        @ApiResponse(code = 403, message = FORBIDDEN, response = ErrorMap.class, responseContainer = "List"),
        @ApiResponse(code = 404, message = "Not Found", response = String.class)})
    @GetMapping(value = "/cases/documents/{documentId}", produces = {APPLICATION_JSON})
    ResponseEntity<Object> getDocumentbyDocumentId(

        @ApiParam(value = S2S_API_PARAM, required = true)
        @RequestHeader(value = SERVICE_AUTHORIZATION, required = true) String serviceAuthorization,

        @ApiParam(value = "documentId", required = true) @PathVariable("documentId") UUID documentId,
        @ApiParam("User-Id of the currently authenticated user. If provided will be used to populate the creator field of a document"
                          + " and will be used for authorisation.")
        @RequestHeader(value = "user-id", required = false) String userId,
        @ApiParam("Authorization header of the currently authenticated user")
        @RequestHeader(value = "Authorization", required = true) String authorization,

        @ApiParam("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "user-roles", required = false) String userRoles);


    @ApiOperation(value = "Updates document instance (ex. ttl).", nickname = "patchDocumentbyDocumentId",
                  notes = "This API will be the single point of reference for patching any case related documents from doc-store. for example removing the "
                          + "ttl from document.",
                  response = StoredDocumentHalResource.class, tags = {"case-document-controller"})
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Success", response = StoredDocumentHalResource.class),
        @ApiResponse(code = 401, message = UNAUTHORIZED, response = String.class),
        @ApiResponse(code = 403, message = FORBIDDEN, response = ErrorMap.class, responseContainer = "List"),
        @ApiResponse(code = 404, message = "Not Found", response = String.class)})
    @PatchMapping(value = "/cases/documents/{documentId}",
                    produces = {APPLICATION_JSON},
                    consumes = {APPLICATION_JSON})
    ResponseEntity<Object> patchDocumentbyDocumentId(
        @ApiParam(value = "", required = true)
        @Valid @RequestBody UpdateDocumentCommand body,

        @ApiParam("Authorization header of the currently authenticated user")
        @RequestHeader(value = "Authorization", required = true) String authorization,

        @ApiParam(value = S2S_API_PARAM, required = true)
        @RequestHeader(value = SERVICE_AUTHORIZATION, required = true) String serviceAuthorization,

        @ApiParam(value = "documentId", required = true)
        @PathVariable("documentId") UUID documentId,

        @ApiParam("user-id of the currently authenticated user. If provided will be used to populate the creator field of a document"
            + " and will be used for authorisation.")
        @RequestHeader(value = "user-id", required = false) String userId,

        @ApiParam("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "user-roles", required = false) String userRoles);


    @ApiOperation(value = "Updates a list of Case Documents with provided Metadata", nickname = "patchMetaDataOnDocuments",
                  notes = "This API will perform bulk operation of 'Update metadata and TTL removal' on list of Case Documents. It will return positive "
                          + "response if both bulk operations are executed sucessfully else an appropriate negative response will be returned.",
                  response = StoredDocumentHalResource.class, tags = {TAG})
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Created", response = StoredDocumentHalResource.class),
        @ApiResponse(code = 204, message = "No Content"),
        @ApiResponse(code = 400, message = BAD_REQUEST, response = ErrorMap.class),
        @ApiResponse(code = 401, message = UNAUTHORIZED, response = ErrorMap.class),
        @ApiResponse(code = 403, message = FORBIDDEN, response = ErrorMap.class),
        @ApiResponse(code = 404, message = RESOURCE_NOT_FOUND, response = ErrorMap.class)})
    @PatchMapping(value = "/cases/documents/attachToCase",
                    produces = {APPLICATION_JSON},
                    consumes = {APPLICATION_JSON})
    ResponseEntity<Object> patchMetaDataOnDocuments(
        @ApiParam(value = "", required = true)
        @Valid @RequestBody DocumentMetadata body,

        @ApiParam(value = S2S_API_PARAM, required = true)
        @RequestHeader(value = SERVICE_AUTHORIZATION, required = true) String serviceAuthorization,

        @ApiParam("User-Id of the currently authenticated user. If provided will be used to populate the creator field of a document"
                          + " and will be used for authorisation.")
        @RequestHeader(value = "user-ud", required = false) String userId,

        @ApiParam("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "user-roles", required = false) String userRoles);


    @ApiOperation(value = "Creates a list of Stored Documents by uploading a list of binary/text files", nickname = "uploadDocuments",
                  notes = "This API will be the single point of reference for uploading any case related documents to doc-store. It will return the document"
                          + " URL along with generated hashed-token. The hashed-token would be valid only for case creation purpose and discarded once "
                          + "document is attached with its case.",
                  response = StoredDocumentHalResourceCollection.class, tags = {TAG})
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Created", response = StoredDocumentHalResourceCollection.class),
        @ApiResponse(code = 204, message = "No Content"),
        @ApiResponse(code = 400, message = BAD_REQUEST, response = ErrorMap.class),
        @ApiResponse(code = 401, message = UNAUTHORIZED, response = ErrorMap.class),
        @ApiResponse(code = 403, message = FORBIDDEN, response = ErrorMap.class),
        @ApiResponse(code = 404, message = RESOURCE_NOT_FOUND, response = ErrorMap.class)})
    @PostMapping(value = "/cases/documents",
                    produces = {APPLICATION_JSON},
                    consumes = {"multipart/form-data"})
    ResponseEntity<Object> uploadDocuments(
        @ApiParam(value = "", required = true) @RequestParam(value = "files", required = true) List<MultipartFile> files,
        @ApiParam(value = "", required = true) @RequestParam(value = "classification", required = true) String classification,
        @ApiParam(value = "", required = false) @RequestParam(value = "roles", required = false) List<String> roles,

        @ApiParam(value = S2S_API_PARAM, required = true)
        @RequestHeader(value = SERVICE_AUTHORIZATION, required = true) String serviceAuthorization,

        @ApiParam(value = "CaseType identifier for the case document.", required = true)
        @RequestHeader(value = "caseTypeId", required = true) String caseTypeId,

        @ApiParam(value = "Jurisdiction identifier for the case document.", required = true)
        @RequestHeader(value = "jurisdictionId", required = true) String jurisdictionId,

        @ApiParam("User-Id of the currently authenticated user. If provided will be used to populate the creator field of a document"
                          + " and will be used for authorisation.")
        @RequestHeader(value = "user-id", required = true) String userId,

        @ApiParam(value = "Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "user-roles", required = false) String userRoles);


    @ApiOperation(value = "Retrieves the hashcode for document Id", nickname = "generateHashCode",
        notes = "This API will return the hashed token required for document upload functionality.",
        response = String.class, tags = {TAG})
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Success", response = StoredDocumentHalResource.class),
        @ApiResponse(code = 401, message = UNAUTHORIZED, response = String.class),
        @ApiResponse(code = 403, message = FORBIDDEN, response = ErrorMap.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Bad Request", response = String.class)})
    @GetMapping(value = "/cases/documents/{documentId}/token", produces = {APPLICATION_JSON})
    ResponseEntity<Object> generateHashCode(

        @ApiParam(value = S2S_API_PARAM, required = true)
        @RequestHeader(value = SERVICE_AUTHORIZATION, required = true) String serviceAuthorization,

        @ApiParam("Authorization header of the currently authenticated user")
        @RequestHeader(value = "Authorization", required = true) String authorization,

        @ApiParam(value = "documentId", required = true) @PathVariable("documentId") UUID documentId,

        @ApiParam(value = "CaseType identifier for the case document.", required = true)
        @RequestHeader(value = "caseTypeId", required = true) String caseTypeId,

        @ApiParam(value = "Jurisdiction identifier for the case document.", required = true)
        @RequestHeader(value = "jurisdictionId", required = true) String jurisdictionId);


}
