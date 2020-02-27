package uk.gov.hmcts.reform.ccd.document.am.controller.feign;

import feign.Headers;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
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
    @Headers({"Authorization: {authorization}", "ServiceAuthorization: {serviceAuthorization}", "user-roles: {user_roles}", "Content-Type: application/json"})
    Response getMetadataForDocument(@PathVariable("documentId") UUID documentId);
}
