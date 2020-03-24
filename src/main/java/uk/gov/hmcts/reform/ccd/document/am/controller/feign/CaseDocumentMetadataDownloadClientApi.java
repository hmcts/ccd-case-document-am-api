package uk.gov.hmcts.reform.ccd.document.am.controller.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import uk.gov.hmcts.reform.ccd.document.am.model.Documents;

import java.util.UUID;

@FeignClient(name = "case-document-am-metadata-api", url = "${case_document_am.url}",
    configuration = CaseDocumentMetadataDownloadClientApi.DownloadConfiguration.class)
public interface CaseDocumentMetadataDownloadClientApi {

    @RequestMapping(method = RequestMethod.GET, value = "/cases/documents/{documentId}")
    Documents getMetadataForDocument(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
                                     @RequestHeader("ServiceAuthorization") String serviceAuth,
                                     @PathVariable("documentId") UUID documentId);





    class DownloadConfiguration {
        @Bean
        @Primary
        Decoder feignDecoder(ObjectMapper objectMapper) {
            return new JacksonDecoder(objectMapper);
        }
    }
}
