package uk.gov.hmcts.reform.ccd.documentam.configuration;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.ccd.documentam.BaseTest;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedServices;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServicesJsonPropertySourceFactoryTest extends BaseTest {

    private static final String SERVICE_ID = "xui_webapp";
    private static final String CASE_TYPE_ID_ALL_WILDCARD = "*";
    private static final String JURISDICTION_ID = "*";

    @Inject
    private AuthorisedServices authorisedServices;

    @Test
    public void successfullyLoadServiceConfigFile() {

        assertThat(authorisedServices).isNotNull();
        assertThat(authorisedServices.getAuthServices())
            .first()
            .satisfies(service -> {
                assertThat(service.getId()).isEqualTo(SERVICE_ID);
                assertThat(service.getCaseTypeId().size()).isEqualTo(1);
                assertThat(service.getJurisdictionId()).isEqualTo(JURISDICTION_ID);
                assertThat(service.getCaseTypeId()).contains(CASE_TYPE_ID_ALL_WILDCARD);
                assertThat(service.getPermissions()).contains(Permission.CREATE);
            });
    }

}
