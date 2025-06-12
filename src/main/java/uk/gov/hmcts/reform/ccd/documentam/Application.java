package uk.gov.hmcts.reform.ccd.documentam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableFeignClients(basePackages = {"uk.gov.hmcts.reform.idam"})
@EnableRetry
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class Application {
    public static void main(final String[] args) {
        SpringApplication.run(Application.class);
    }
}
