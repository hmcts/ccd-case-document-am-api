package uk.gov.hmcts.reform.ccd.documentam.configuration;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.documentam.client.RestTemplateHeaderModifierInterceptor;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class ApplicationConfiguration {

    @Value("${http.client.connection.timeout}")
    private int connectionTimeout;

    @Value("${http.client.read.timeout}")
    private int readTimeout;

    @Bean
    public RestTemplate restTemplate(final RestTemplateHeaderModifierInterceptor headerModifierInterceptor) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(getHttpClient()));

        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
        if (CollectionUtils.isEmpty(interceptors)) {
            interceptors = new ArrayList<>();
        }
        interceptors.add(headerModifierInterceptor);
        restTemplate.setInterceptors(interceptors);

        return restTemplate;
    }

    @Bean
    public RestTemplate dataStoreRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(getHttpClient()));
        return restTemplate;
    }

    private CloseableHttpClient getHttpClient() {
        RequestConfig config = RequestConfig.custom()
                                            .setConnectionRequestTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
                                            .setResponseTimeout(readTimeout, TimeUnit.MILLISECONDS)
                                            .build();

        return HttpClientBuilder
            .create()
            .useSystemProperties()
            .setDefaultRequestConfig(config)
            .build();
    }

    @Bean
    public Clock utcClock() {
        return Clock.systemUTC();
    }

}
