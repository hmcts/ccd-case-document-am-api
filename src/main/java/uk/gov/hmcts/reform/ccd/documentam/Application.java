package uk.gov.hmcts.reform.ccd.documentam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class Application {
    public static void main(final String[] args) {
        SpringApplication.run(Application.class);
    }
}
