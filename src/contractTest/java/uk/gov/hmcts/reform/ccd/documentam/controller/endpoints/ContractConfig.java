package uk.gov.hmcts.reform.ccd.documentam.controller.endpoints;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.ccd.documentam.ApplicationParams;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.ccd.documentam.service.DocumentManagementService;


@Configuration
public class ContractConfig {
    @MockitoBean
    DocumentManagementService documentManagementService;

    @MockitoBean
    SecurityUtils securityUtils;

    @MockitoBean
    ApplicationParams applicationParams;

    @Bean
    @Primary
    public CaseDocumentAmController caseDocumentAmController() {
        return new CaseDocumentAmController(documentManagementService, securityUtils, applicationParams);
    }

}
