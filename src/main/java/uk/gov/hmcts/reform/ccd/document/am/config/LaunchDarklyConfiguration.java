package uk.gov.hmcts.reform.ccd.document.am.config;

import com.launchdarkly.client.LDClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.security.Security;

@Configuration
public class LaunchDarklyConfiguration {

    private LDClient client;

    @Value("${launchdarkly.sdk.key}")
    private String sdkKey;

    @PostConstruct
    void initialise() {
        client = new LDClient(sdkKey);

        // LaunchDarkly servers use dynamic IP addresses behind a load balancer, which could lead
        // to connection issues if the DNS cache is not updated frequently
        Security.setProperty("networkaddress.cache.ttl" , "60");
    }

    @Bean
    public LDClient ldClient() {
        return client;
    }

    @PreDestroy
    void close() throws IOException {
        client.flush();
        client.close();
    }
}
