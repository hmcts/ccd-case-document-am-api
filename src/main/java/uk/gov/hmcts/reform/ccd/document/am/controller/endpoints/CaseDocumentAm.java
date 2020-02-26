package uk.gov.hmcts.reform.ccd.document.am.controller.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.ErrorMap;
import uk.gov.hmcts.reform.ccd.document.am.model.MetadataSearchCommand;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResourceCollection;
import uk.gov.hmcts.reform.ccd.document.am.model.UpdateDocumentCommand;

import javax.validation.Valid;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.APPLICATION_JSON;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.BAD_REQUEST;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.FORBIDDEN;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.RESOURCE_NOT_FOUND;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.S2S_API_PARAM;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.TAG;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.UNAUTHORIZED;

@Api(value = "cases", description = "the cases API")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface CaseDocumentAm {

    @ApiOperation(value = "Deletes a Case Document.", nickname = "deleteDocumentbyDocumentId",
                  notes = "This API will be the single point of reference for deleting any case related documents from doc-store.",
                  response = String.class, tags = {TAG})
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = String.class),
        @ApiResponse(code = 204, message = "No Content"),
        @ApiResponse(code = 400, message = BAD_REQUEST, response = ErrorMap.class),
        @ApiResponse(code = 401, message = UNAUTHORIZED, response = ErrorMap.class),
        @ApiResponse(code = 403, message = FORBIDDEN, response = ErrorMap.class),
        @ApiResponse(code = 404, message = RESOURCE_NOT_FOUND, response = ErrorMap.class)})
    @RequestMapping(value = "/cases/documents/{documentId}",
                    produces = {APPLICATION_JSON},
                    method = RequestMethod.DELETE)
    ResponseEntity<String> deleteDocumentbyDocumentId(
        @ApiParam(value = S2S_API_PARAM, required = true)
        @RequestHeader(value = SERVICE_AUTHORIZATION, required = true) String serviceAuthorization,

        @ApiParam(value = "documentId", required = true)
        @PathVariable("documentId") UUID documentId,

        @ApiParam("permanent delete flag")
        @Valid @RequestParam("permanent") Boolean permanent,

        @ApiParam("User-Id of the currently authenticated user. If provided will be used to populate the creator field of a document"
                          + " and will be used for authorisation.")
        @RequestHeader(value = "User-Id", required = false) String userId,

        @ApiParam("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "User-Roles", required = false) String userRoles);


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
    @RequestMapping(value = "/cases/documents/{documentId}/binary", produces = {APPLICATION_JSON}, method = RequestMethod.GET)
    ResponseEntity<Object> getDocumentBinaryContentbyDocumentId(

        @ApiParam(value = S2S_API_PARAM, required = true)
        @RequestHeader(value = SERVICE_AUTHORIZATION, required = true) String serviceAuthorization,
        @ApiParam(value = "documentId", required = true)
        @PathVariable("documentId") UUID documentId,
        @ApiParam("User-Id of the currently authenticated user. If provided will be used to populate the creator field of a document"
                          + " and will be used for authorisation.")
        @RequestHeader(value = "User-Id", required = false) String userId,
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
    @RequestMapping(value = "/cases/documents/{documentId}", produces = {APPLICATION_JSON}, method = RequestMethod.GET)
    ResponseEntity<Object> getDocumentbyDocumentId(

        @ApiParam(value = S2S_API_PARAM, required = true)
        @RequestHeader(value = SERVICE_AUTHORIZATION, required = true) String serviceAuthorization,

        @ApiParam(value = "documentId", required = true) @PathVariable("documentId") UUID documentId,
        @ApiParam("User-Id of the currently authenticated user. If provided will be used to populate the creator field of a document"
                          + " and will be used for authorisation.")
        @RequestHeader(value = "User-Id", required = false) String userId,

        @ApiParam("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "User-Roles", required = false) String userRoles);


    @ApiOperation(value = "Updates document instance (ex. ttl).", nickname = "patchDocumentbyDocumentId",
                  notes = "This API will be the single point of reference for patching any case related documents from doc-store. for example removing the "
                          + "ttl from document.",
                  response = StoredDocumentHalResource.class, tags = {"case-document-controller"})
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Success", response = StoredDocumentHalResource.class),
        @ApiResponse(code = 401, message = UNAUTHORIZED, response = String.class),
        @ApiResponse(code = 403, message = FORBIDDEN, response = ErrorMap.class, responseContainer = "List"),
        @ApiResponse(code = 404, message = "Not Found", response = String.class)})
    @RequestMapping(value = "/cases/documents/{documentId}",
                    produces = {APPLICATION_JSON},
                    consumes = {APPLICATION_JSON},
                    method = RequestMethod.PATCH)
    ResponseEntity<StoredDocumentHalResource> patchDocumentbyDocumentId(
        @ApiParam(value = "", required = true)
        @Valid @RequestBody UpdateDocumentCommand body,

        @ApiParam(value = S2S_API_PARAM, required = true)
        @RequestHeader(value = SERVICE_AUTHORIZATION, required = true) String serviceAuthorization,

        @ApiParam(value = "documentId", required = true)
        @PathVariable("documentId") UUID documentId,

        @ApiParam("User-Id of the currently authenticated user. If provided will be used to populate the creator field of a document"
                          + " and will be used for authorisation.")
        @RequestHeader(value = "User-Id", required = false) String userId,

        @ApiParam("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "User-Roles", required = false) String userRoles);


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
    @RequestMapping(value = "/cases/documents/attachToCase",
                    produces = {APPLICATION_JSON},
                    consumes = {APPLICATION_JSON},
                    method = RequestMethod.PATCH)
    ResponseEntity<StoredDocumentHalResource> patchMetaDataOnDocuments(
        @ApiParam(value = "", required = true)
        @Valid @RequestBody CaseDocumentMetadata body,

        @ApiParam(value = S2S_API_PARAM, required = true)
        @RequestHeader(value = SERVICE_AUTHORIZATION, required = true) String serviceAuthorization,

        @ApiParam("User-Id of the currently authenticated user. If provided will be used to populate the creator field of a document"
                          + " and will be used for authorisation.")
        @RequestHeader(value = "User-Id", required = false) String userId,

        @ApiParam("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "User-Roles", required = false) String userRoles);


    @ApiOperation(value = "Search Case Documents using metadata.", nickname = "postDocumentsSearchCommand",
                  notes = "This API will can filter case documents based upon Metadata. It will retrieve json representation of the list of document.",
                  response = StoredDocumentHalResourceCollection.class, tags = {"case-document-search-controller",})
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Success", response = StoredDocumentHalResourceCollection.class),
        @ApiResponse(code = 400, message = BAD_REQUEST, response = ErrorMap.class),
        @ApiResponse(code = 401, message = UNAUTHORIZED, response = ErrorMap.class),
        @ApiResponse(code = 403, message = FORBIDDEN, response = ErrorMap.class),
        @ApiResponse(code = 404, message = RESOURCE_NOT_FOUND, response = ErrorMap.class)})
    @RequestMapping(value = "/cases/documents/filter",
                    produces = {APPLICATION_JSON},
                    consumes = {APPLICATION_JSON},
                    method = RequestMethod.POST)
    ResponseEntity<StoredDocumentHalResourceCollection> postDocumentsSearchCommand(
        @ApiParam(value = "", required = true)
        @Valid @RequestBody MetadataSearchCommand body,
        @ApiParam(value = S2S_API_PARAM, required = true)
        @RequestHeader(value = SERVICE_AUTHORIZATION, required = true) String serviceAuthorization,
        @ApiParam("User-id of the currently authenticated user. If provided will be used to populate the creator field of a"
                          + " document and will be used for authorisation.")
        @RequestHeader(value = "User-Id", required = false) String userId,
        @ApiParam("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
        @RequestHeader(value = "User-Roles", required = false) String userRoles,
        @ApiParam("") @Valid @RequestParam(value = "offset", required = false) Long offset,
        @ApiParam("") @Valid @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
        @ApiParam("") @Valid @RequestParam(value = "pageSize", required = false) Integer pageSize,
        @ApiParam("") @Valid @RequestParam(value = "paged", required = false) Boolean paged,
        @ApiParam("") @Valid @RequestParam(value = "sort.sorted", required = false) Boolean sortSorted,
        @ApiParam("") @Valid @RequestParam(value = "sort.unsorted", required = false) Boolean sortUnsorted,
        @ApiParam("") @Valid @RequestParam(value = "unpaged", required = false) Boolean unpaged);


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
    @RequestMapping(value = "/cases/documents",
                    produces = {APPLICATION_JSON},
                    consumes = {"multipart/form-data"},
                    method = RequestMethod.POST)
    ResponseEntity<StoredDocumentHalResourceCollection> uploadDocuments(
        @ApiParam(value = "", required = true) @RequestParam(value = "files", required = true) List files,
        @ApiParam(value = "", required = true) @RequestParam(value = "classification", required = true) String classification,
        @ApiParam(value = "", required = true) @RequestParam(value = "ttl", required = false) Date ttl,
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
        @RequestHeader(value = "user-roles", required = false) String userRoles);

}
