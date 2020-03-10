package uk.gov.hmcts.reform.ccd.document.am.configuration;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import static java.lang.String.format;

@Configuration
@Slf4j
public class ApplicationConfiguration {

    private final String s2sSecret;
    private final String s2sMicroService;
    private final String s2sUrl;
    private transient GoogleAuthenticator googleAuthenticator;


    public ApplicationConfiguration(@Value("${idam.s2s-auth.totp_secret}") String s2sSecret,
                                    @Value("${idam.s2s-auth.microservice}") String s2sMicroService,
                                    @Value("${idam.s2s-auth.url}") String s2sUrl) {
        this.s2sSecret = s2sSecret;
        this.s2sMicroService = s2sMicroService;
        this.s2sUrl = s2sUrl;
        this.googleAuthenticator = new GoogleAuthenticator();
    }

    public String getS2sSecret() {
        return s2sSecret;
    }

    public String getS2sMicroService() {
        return s2sMicroService;
    }

    public String getS2sUrl() {
        return s2sUrl;
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(getHttpClient()));
        return restTemplate;
    }

    private CloseableHttpClient getHttpClient() {
        int timeout = 10000;
        RequestConfig config = RequestConfig.custom()
                                            .setConnectTimeout(timeout)
                                            .setConnectionRequestTimeout(timeout)
                                            .setSocketTimeout(timeout)
                                            .build();

        return HttpClientBuilder
            .create()
            .useSystemProperties()
            .setDefaultRequestConfig(config)
            .build();

    }


    public String generate() {
        final String oneTimePassword = format("%06d", googleAuthenticator.getTotpPassword(s2sSecret));
        System.out.println("s2sSecret key :: " + s2sSecret);
        System.out.print("One Time Password  " + oneTimePassword);
        return oneTimePassword;


    }
}
