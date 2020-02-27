package uk.gov.hmcts.reform.ccd.document.am.controller.feign;

import feign.Headers;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import uk.gov.hmcts.reform.ccd.document.am.configuration.FeignClientConfiguration;

import java.util.UUID;

@FeignClient(name = "CaseDataStoreClient",
             url = "${caseDataStoreUrl}",
             configuration = FeignClientConfiguration.class
             )
public interface CaseDataStoreFeignClient {

    @RequestMapping(method = RequestMethod.GET, value = "/{caseid}/{documentId}")
    @Headers({"Authorization: {Authorization}", "ServiceAuthorization: {ServiceAuthorization}", "Content-Type: application/json"})
    Response getCaseDocumentMetadata(@PathVariable("caseID") String caseID,@PathVariable("documentId") UUID documentId);



}
