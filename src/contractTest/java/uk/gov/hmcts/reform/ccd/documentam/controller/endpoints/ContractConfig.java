package uk.gov.hmcts.reform.ccd.documentam.controller.endpoints;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.ccd.documentam.service.DocumentManagementService;


@Configuration
public class ContractConfig {
    @MockBean
    DocumentManagementService documentManagementService;

    @MockBean
    SecurityUtils securityUtils;

    @Bean
    @Primary
    public CaseDocumentAmController caseDocumentAmController() {
        return new CaseDocumentAmController(documentManagementService, securityUtils);
    }

}
