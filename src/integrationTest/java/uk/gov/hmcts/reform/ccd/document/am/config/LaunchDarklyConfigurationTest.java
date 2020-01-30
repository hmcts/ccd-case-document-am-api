package uk.gov.hmcts.reform.ccd.document.am.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class LaunchDarklyConfigurationTest {

    @Autowired
    private LaunchDarklyConfiguration launchDarklyConfiguration;

    @Test
    void launchDarklyClientShouldBeInitialised() {
        boolean isLdClientInitialised = launchDarklyConfiguration.ldClient().initialized();
        assertTrue(isLdClientInitialised, "LaunchDarkly client should be initialised");
    }
}
