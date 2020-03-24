package uk.gov.hmcts.reform.ccd.document.am.controller.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "case-document-am-delete-api", url = "${case_document_am.url}")
public interface CaseDocumentDeleteApi {

    @RequestMapping(method = RequestMethod.DELETE, value = "cases/documents/{documentId}")
    ResponseEntity  deleteDocument(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
        @RequestHeader("ServiceAuthorization") String serviceAuth,
        @RequestHeader("user-roles") String userRoles,
        @PathVariable("documentId") UUID documentId,
        @RequestParam("permanent") boolean permanent
    );
}
