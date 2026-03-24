package uk.gov.hmcts.reform.ccd.documentam.controller.endpoints;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.ccd.documentam.ApplicationParams;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.ccd.documentam.service.DocumentManagementService;

import java.util.List;

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
        ApplicationParams applicationParams = new ApplicationParams();
        ReflectionTestUtils.setField(applicationParams, "documentURL", "http://dm-store");
        ReflectionTestUtils.setField(applicationParams, "documentTtlInDays", 1);
        ReflectionTestUtils.setField(applicationParams, "salt", "test-salt");
        ReflectionTestUtils.setField(applicationParams, "hashCheckEnabled", false);
        ReflectionTestUtils.setField(applicationParams, "movingCaseTypes", List.of());
        ReflectionTestUtils.setField(applicationParams, "clientRequestHeadersToForward", List.of());
        ReflectionTestUtils.setField(applicationParams, "isStreamDownloadEnabled", false);
        ReflectionTestUtils.setField(applicationParams, "isStreamUploadEnabled", false);
        return applicationParams;
    }

    @Bean
    @Primary
    public CaseDocumentAmController caseDocumentAmController(DocumentManagementService documentManagementService,
                                                             SecurityUtils securityUtils,
                                                             ApplicationParams applicationParams) {
        return new CaseDocumentAmController(documentManagementService, securityUtils, applicationParams);
    }

}
