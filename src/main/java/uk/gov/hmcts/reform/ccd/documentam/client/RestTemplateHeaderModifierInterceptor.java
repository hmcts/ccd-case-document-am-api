package uk.gov.hmcts.reform.ccd.documentam.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;

import java.io.IOException;

@Component
public class RestTemplateHeaderModifierInterceptor implements ClientHttpRequestInterceptor {
    private final SecurityUtils securityUtils;

    @Autowired
    public RestTemplateHeaderModifierInterceptor(final SecurityUtils securityUtils) {
        this.securityUtils = securityUtils;
    }

    @Override
    public ClientHttpResponse intercept(final HttpRequest request,
                                        final byte[] body,
                                        final ClientHttpRequestExecution execution) throws IOException {
        final HttpHeaders requestHeaders = getRequestHeaders();
        requestHeaders.forEach((key, value) -> request.getHeaders().addAll(key, value));

        return execution.execute(request, body);
    }

    private HttpHeaders getRequestHeaders() {
        HttpHeaders headers = securityUtils.serviceAuthorizationHeaders();
        headers.set(Constants.USERID, securityUtils.getUserInfo().getUid());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
