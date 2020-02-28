package uk.gov.hmcts.reform.ccd.document.am.controller.feign;

import feign.Headers;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import uk.gov.hmcts.reform.ccd.document.am.configuration.FeignClientConfiguration;

import java.util.UUID;

@FeignClient(name = "DocumentStoreClient",
             url = "${documentStoreUrl}",
             configuration = FeignClientConfiguration.class
             )
public interface DocumentStoreFeignClient {

    @RequestMapping(method = RequestMethod.GET, value = "/{documentId}")
    @Headers({ "ServiceAuthorization: {serviceAuthorization}", "user-roles: {user-roles}", "user-id: {user-id}", "Content-Type: application/json"})
    Response getMetadataForDocument(@PathVariable("documentId") UUID documentId);

    @RequestMapping(method = RequestMethod.GET, value = "/{documentId}/binary")
    @Headers({"ServiceAuthorization: {serviceAuthorization}", "user-roles: {user-roles}", "user-id: {user-id}", "Content-Type: application/json"})
    ResponseEntity<Resource> getDocumentBinary(@PathVariable("documentId") UUID documentId);


}
