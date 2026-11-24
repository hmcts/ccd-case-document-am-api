package uk.gov.hmcts.reform.ccd.documentam.controller.endpoints;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;

import uk.gov.hmcts.reform.ccd.documentam.ApplicationParams;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.ccd.documentam.service.DocumentManagementService;

import static org.mockito.Mockito.mock;


@Configuration
public class ContractConfig {

    @Bean
    public DocumentManagementService documentManagementService() {
        return mock(DocumentManagementService.class);
    }

    @Bean
    public SecurityUtils securityUtils() {
        return mock(SecurityUtils.class);
    }

    public ApplicationParams applicationParamMock = mock(ApplicationParams.class);

    @Bean
    public CaseDocumentAmController caseDocumentAmController(
            DocumentManagementService documentManagementService,
            SecurityUtils securityUtils) {
        return new CaseDocumentAmController(documentManagementService, securityUtils, applicationParamMock);
    }

    @Bean
    public JsonMapperBuilderCustomizer jsonMapperCustomizer() {
        return builder -> {
        };
    }

}
