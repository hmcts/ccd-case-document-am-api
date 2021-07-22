package uk.gov.hmcts.reform.ccd.documentam.configuration;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.ccd.documentam.BaseTest;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedService;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedServices;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServicesJsonPropertySourceFactoryTest extends BaseTest {

    private static final String SERVICE_ID = "xui_webapp";
    private static final String CASE_TYPE_ID_ALL_WILDCARD = "*";
    private static final String JURISDICTION_ID = "BEFTA_JURISDICTION_2";

    @Inject
    private AuthorisedServices authorisedServices;

    @Test
    public void successfullyLoadServiceConfigFile() {

        assertThat(authorisedServices).isNotNull()
            .satisfies(x -> {
                AuthorisedService authorisedService = x.getAuthServices().get(0);
                assertThat(authorisedService.getId()).isEqualTo(SERVICE_ID);
                assertThat(authorisedService.getCaseTypeId().size()).isEqualTo(1);
                assertThat(authorisedService.getCaseTypeId().get(0)).isEqualTo(CASE_TYPE_ID_ALL_WILDCARD);
                assertThat(authorisedService.getJurisdictionId()).isEqualTo(JURISDICTION_ID);
                assertThat(authorisedService.getPermissions().get(0)).isEqualTo(Permission.CREATE);
            });
    }

}
