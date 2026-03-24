package uk.gov.hmcts.reform.ccd.documentam.controller.endpoints;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.reform.ccd.documentam.ApplicationParams;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.ccd.documentam.service.DocumentManagementService;

import static org.mockito.Mockito.mock;


@Configuration
public class ContractConfig {
    @Bean
    @Primary
    public DocumentManagementService documentManagementService() {
        return mock(DocumentManagementService.class);
    }

    @Bean
    @Primary
    public SecurityUtils securityUtils() {
        return mock(SecurityUtils.class);
    }

    @Bean
    @Primary
    public ApplicationParams applicationParams() {
        return mock(ApplicationParams.class);
    }

    @Bean
    @Primary
    public CaseDocumentAmController caseDocumentAmController(DocumentManagementService documentManagementService,
                                                             SecurityUtils securityUtils,
                                                             ApplicationParams applicationParams) {
        return new CaseDocumentAmController(documentManagementService, securityUtils, applicationParams);
    }

}
