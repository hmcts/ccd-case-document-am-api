package uk.gov.hmcts.reform.ccd.documentam.configuration;

import lombok.extern.slf4j.Slf4j;
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

import javax.annotation.PreDestroy;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class ApplicationConfiguration {

    @Value("${http.client.connection.timeout}")
    private int connectionTimeout;

    @Value("${http.client.read.timeout}")
    private int readTimeout;

    @Value("${http.client.max.total}")
    private int maxTotalHttpClient;

    @Value("${http.client.seconds.idle.connection}")
    private int maxSecondsIdleConnection;

    @Value("${http.client.max.client_per_route}")
    private int maxClientPerRoute;

    @Value("${http.client.validate.after.inactivity}")
    private int validateAfterInactivity;

    private PoolingHttpClientConnectionManager cm;

    @PreDestroy
    void close() {
        log.info("PreDestroy called");
        if (null != cm) {
            log.info("closing connection manager");
            cm.close();
        }
    }

    @Bean
    public CloseableHttpClient httpClient() {
        return getPoolingHttpClient();
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(getHttpClient()));

        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
        if (CollectionUtils.isEmpty(interceptors)) {
            interceptors = new ArrayList<>();
        }

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

    private CloseableHttpClient getPoolingHttpClient() {
        cm = new PoolingHttpClientConnectionManager();

        log.info("""
                HttpClient Configuration:
                maxTotalHttpClient: {},
                maxSecondsIdleConnection: {},
                maxClientPerRoute: {},
                validateAfterInactivity: {},
                connectionReadTimeout: {}
                connectionTimeout: {}""", maxTotalHttpClient, maxSecondsIdleConnection, maxClientPerRoute,
                 validateAfterInactivity, readTimeout, connectionTimeout);

        cm.setMaxTotal(maxTotalHttpClient);
        cm.closeIdleConnections(maxSecondsIdleConnection, TimeUnit.SECONDS);
        cm.setDefaultMaxPerRoute(maxClientPerRoute);
        cm.setValidateAfterInactivity(validateAfterInactivity);
        final RequestConfig
            config =
            RequestConfig.custom()
                .setConnectTimeout(connectionTimeout)
                .setConnectionRequestTimeout(connectionTimeout)
                .setSocketTimeout(readTimeout)
                .build();

        return HttpClientBuilder.create()
            .useSystemProperties()
            .setDefaultRequestConfig(config)
            .setConnectionManager(cm)
            .build();
    }

    @Bean
    public Clock utcClock() {
        return Clock.systemUTC();
    }

}
