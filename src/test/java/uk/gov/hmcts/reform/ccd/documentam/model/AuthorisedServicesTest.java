package uk.gov.hmcts.reform.ccd.documentam.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorisedServicesTest {

    @Test
    void shouldRejectSystemReadRoleOnWildcardJurisdiction() {
        AuthorisedServices services = services(service("*", "caseworker-system"));

        assertThrows(IllegalStateException.class, services::validate);
    }

    @Test
    void shouldAllowSystemReadRoleWithinOwnJurisdiction() {
        AuthorisedServices services = services(service("SSCS", "caseworker-sscs-systemupdate"),
                                               service("*", null));

        assertDoesNotThrow(services::validate);
    }

    private static AuthorisedServices services(AuthorisedService... services) {
        AuthorisedServices authorisedServices = new AuthorisedServices();
        authorisedServices.setAuthServices(List.of(services));
        return authorisedServices;
    }

    private static AuthorisedService service(String jurisdictionId, String systemReadRole) {
        return AuthorisedService.builder()
            .id("service")
            .jurisdictionId(jurisdictionId)
            .caseTypeId(List.of("*"))
            .systemReadRole(systemReadRole)
            .build();
    }
}
