package uk.gov.hmcts.reform.ccd.document.am;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement(proxyTargetClass = true)
@EnableCircuitBreaker
@EnableFeignClients
public class Application {
    public static void main(final String[] args) {
        SpringApplication.run(Application.class);
    }

}
