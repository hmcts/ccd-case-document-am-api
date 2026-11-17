package uk.gov.hmcts.reform.ccd.documentam.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedServices;

import static org.assertj.core.api.Assertions.assertThat;

class ServicesBindingValidationTest {
    private static final String EXPECTED_MESSAGE = "jurisdictionId: \"*\" must be the only element when present";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(TestConfig.class);

    @Test
    void realConfigJsonBindsAndPassesValidation() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AuthorisedServices.class);
            assertThat(context.getBean(AuthorisedServices.class).getAuthServices()).isNotEmpty();
        });
    }

    @Test
    void startsWhenAsteriskIsTheOnlyId() {
        runner
            .withPropertyValues("authServices[0].jurisdictionId[0]=*")
            .run(context -> {
                assertThat(context).hasNotFailed();

                AuthorisedServices services = context.getBean(AuthorisedServices.class);
                assertThat(services.getAuthServices()).hasSize(1);
                assertThat(services.getAuthServices().getFirst().getJurisdictionId()).containsExactly("*");
            });
    }

    @Test
    void startsWhenIdsDoNotContainAsterisk() {
        runner
            .withPropertyValues(
                "authServices[0].jurisdictionId[0]=1",
                "authServices[0].jurisdictionId[1]=2")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(AuthorisedServices.class).getAuthServices().getFirst().getJurisdictionId())
                    .containsExactly("1", "2");
            });
    }

    @Test
    void failsStartupWhenAsteriskIsCombinedWithOtherIds() {
        runner
            .withPropertyValues(
                "authServices[0].jurisdictionId[0]=*",
                "authServices[0].jurisdictionId[1]=1")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context).getFailure()
                    .hasRootCauseInstanceOf(BindValidationException.class)
                    .rootCause()
                    .hasMessageContaining(EXPECTED_MESSAGE);
            });
    }

    @Test
    void failsStartupWhenOnlyALaterServiceIsInvalid() {
        runner
            .withPropertyValues(
                "authServices[0].jurisdictionId[0]=*",
                "authServices[1].jurisdictionId[0]=*",
                "authServices[1].jurisdictionId[1]=test")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context).getFailure()
                    .hasRootCauseInstanceOf(BindValidationException.class)
                    .rootCause()
                    .hasMessageContaining(EXPECTED_MESSAGE)
                    .hasMessageContaining("authServices[1].jurisdictionIdWildcardAlone");
            });
    }

    @Configuration
    @EnableConfigurationProperties
    @Import(AuthorisedServices.class)
    static class TestConfig {
    }

}
