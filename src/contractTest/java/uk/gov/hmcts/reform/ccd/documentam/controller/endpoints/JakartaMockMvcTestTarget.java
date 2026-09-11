package uk.gov.hmcts.reform.ccd.documentam.controller.endpoints;

import au.com.dius.pact.core.model.ContentType;
import au.com.dius.pact.core.model.IRequest;
import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.OptionalBody;
import au.com.dius.pact.core.model.Pact;
import au.com.dius.pact.core.model.PactSource;
import au.com.dius.pact.core.model.SynchronousRequestResponse;
import au.com.dius.pact.core.model.generators.GeneratorTestMode;
import au.com.dius.pact.provider.IProviderInfo;
import au.com.dius.pact.provider.IProviderVerifier;
import au.com.dius.pact.provider.ProviderInfo;
import au.com.dius.pact.provider.ProviderResponse;
import au.com.dius.pact.provider.junit5.TestTarget;
import jakarta.servlet.http.Cookie;
import kotlin.Pair;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.ContentDisposition;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pact 4.6's MockMvcTestTarget maps string multipart fields to MockPart, which is incompatible with Spring 6/Jakarta.
 * This target keeps standalone MockMvc verification but maps string multipart fields to request parameters.
 */
final class JakartaMockMvcTestTarget implements TestTarget {

    private MockMvc mockMvc;
    private List<Object> controllers = new ArrayList<>();
    private List<Object> controllerAdvices = new ArrayList<>();
    private List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
    private boolean printRequestResponse;
    private String servletPath;

    @Override
    public Map<String, Object> getUserConfig() {
        return Collections.emptyMap();
    }

    @Override
    public IProviderInfo getProviderInfo(String serviceName, PactSource pactSource) {
        return new ProviderInfo(serviceName);
    }

    @Override
    public Pair<Object, Object> prepareRequest(Pact pact, Interaction interaction, Map<String, Object> context) {
        if (interaction instanceof SynchronousRequestResponse requestResponse) {
            IRequest request = requestResponse.getRequest().generatedRequest(context, GeneratorTestMode.Provider);
            return new Pair<>(toMockRequestBuilder(request), buildMockMvc());
        }

        throw new UnsupportedOperationException("Only request/response interactions can be used with a MockMvc target");
    }

    @Override
    public boolean isHttpTarget() {
        return true;
    }

    @Override
    public ProviderResponse executeInteraction(Object client, Object request) {
        MockMvc mockMvcClient = (MockMvc) client;
        MockHttpServletRequestBuilder requestBuilder = (MockHttpServletRequestBuilder) request;

        try {
            MvcResult mvcResult = performRequest(mockMvcClient, requestBuilder);
            return handleResponse(mvcResult.getResponse());
        } catch (Exception exception) {
            throw new IllegalStateException("Request to provider failed", exception);
        }
    }

    @Override
    public void prepareVerifier(IProviderVerifier verifier, Object testInstance, Pact pact) {
        // No custom verifier setup required.
    }

    @Override
    public boolean supportsInteraction(Interaction interaction) {
        return interaction instanceof SynchronousRequestResponse;
    }

    void setControllers(Object... controllers) {
        this.controllers = List.of(controllers);
    }

    void setControllerAdvices(Object... controllerAdvices) {
        this.controllerAdvices = List.of(controllerAdvices);
    }

    void setMessageConverters(HttpMessageConverter<?>... messageConverters) {
        this.messageConverters = List.of(messageConverters);
    }

    void setPrintRequestResponse(boolean printRequestResponse) {
        this.printRequestResponse = printRequestResponse;
    }

    void setServletPath(String servletPath) {
        this.servletPath = servletPath;
    }

    private MockMvc buildMockMvc() {
        if (mockMvc != null) {
            return mockMvc;
        }

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/");
        if (StringUtils.isNotBlank(servletPath)) {
            requestBuilder.servletPath(servletPath);
        }

        StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(controllers.toArray())
            .setControllerAdvice(controllerAdvices.toArray())
            .setMessageConverters(messageConverters.toArray(new HttpMessageConverter[0]))
            .defaultRequest(requestBuilder);

        mockMvc = builder.build();
        return mockMvc;
    }

    private MockHttpServletRequestBuilder toMockRequestBuilder(IRequest request) {
        try {
            OptionalBody body = request.getBody();
            MockHttpServletRequestBuilder servletRequestBuilder;

            if (body.isPresent()) {
                if (request.isMultipartFileUpload()) {
                    servletRequestBuilder = multipartRequest(request, body);
                } else {
                    servletRequestBuilder = MockMvcRequestBuilders
                        .request(HttpMethod.valueOf(request.getMethod()), requestUriString(request))
                        .headers(mapHeaders(request, true))
                        .content(body.getValue());
                }
            } else {
                servletRequestBuilder = MockMvcRequestBuilders
                    .request(HttpMethod.valueOf(request.getMethod()), requestUriString(request))
                    .headers(mapHeaders(request, false));
            }

            Cookie[] cookies = cookies(request);
            if (cookies.length > 0) {
                servletRequestBuilder.cookie(cookies);
            }

            return servletRequestBuilder;
        } catch (IOException | MessagingException exception) {
            throw new IllegalStateException("Failed to build MockMvc request from Pact interaction", exception);
        }
    }

    private MockHttpServletRequestBuilder multipartRequest(IRequest request, OptionalBody body)
        throws MessagingException, IOException {

        MimeMultipart multipart = new MimeMultipart(new ByteArrayDataSource(
            body.unwrap(),
            request.asHttpPart().contentTypeHeader()
        ));
        MockMultipartHttpServletRequestBuilder multipartRequest =
            MockMvcRequestBuilders.multipart(requestUriString(request));

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            ContentDisposition contentDisposition =
                new ContentDisposition(bodyPart.getHeader("Content-Disposition")[0]);
            String name = Objects.toString(contentDisposition.getParameter("name"), "file");
            String filename = Objects.toString(contentDisposition.getParameter("filename"), "");
            byte[] partContent = FileCopyUtils.copyToByteArray(bodyPart.getInputStream());

            if (filename.isEmpty()) {
                multipartRequest.param(name, new String(partContent, partCharset(bodyPart)));
            } else {
                multipartRequest.file(new MockMultipartFile(
                    name,
                    filename,
                    bodyPart.getContentType(),
                    partContent
                ));
            }
        }

        return multipartRequest.headers(mapHeaders(request, true));
    }

    private Charset partCharset(BodyPart bodyPart) throws MessagingException {
        try {
            MediaType mediaType = MediaType.parseMediaType(bodyPart.getContentType());
            return mediaType.getCharset() == null ? StandardCharsets.UTF_8 : mediaType.getCharset();
        } catch (IllegalArgumentException exception) {
            return StandardCharsets.UTF_8;
        }
    }

    private Cookie[] cookies(IRequest request) {
        return request.cookies().stream()
            .map(cookie -> {
                String[] values = cookie.split("=", 2);
                return new Cookie(values[0], values.length > 1 ? values[1] : "");
            })
            .toArray(Cookie[]::new);
    }

    private URI requestUriString(IRequest request) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath(request.getPath());

        request.getQuery().forEach((key, value) -> uriBuilder.queryParam(key, value.toArray()));

        return URI.create(uriBuilder.toUriString());
    }

    private HttpHeaders mapHeaders(IRequest request, boolean hasBody) {
        HttpHeaders httpHeaders = new HttpHeaders();

        request.getHeaders().forEach((key, values) -> httpHeaders.add(key, String.join(", ", values)));

        if (hasBody && !httpHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
            httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        }

        return httpHeaders;
    }

    private MvcResult performRequest(MockMvc mockMvcClient, MockHttpServletRequestBuilder requestBuilder)
        throws Exception {

        ResultActions resultActions = mockMvcClient.perform(requestBuilder);
        if (printRequestResponse) {
            resultActions.andDo(MockMvcResultHandlers.print());
        }

        MvcResult mvcResult = resultActions.andReturn();
        if (mvcResult.getRequest().isAsyncStarted()) {
            mvcResult = mockMvcClient.perform(MockMvcRequestBuilders.asyncDispatch(mvcResult)).andReturn();
        }

        return mvcResult;
    }

    private ProviderResponse handleResponse(MockHttpServletResponse httpResponse) {
        Map<String, List<String>> headers = new java.util.LinkedHashMap<>();
        httpResponse.getHeaderNames()
            .forEach(headerName -> headers.put(headerName, new ArrayList<>(httpResponse.getHeaders(headerName))));

        ContentType contentType = StringUtils.isBlank(httpResponse.getContentType())
            ? ContentType.getJSON()
            : ContentType.fromString(httpResponse.getContentType());

        return new ProviderResponse(
            httpResponse.getStatus(),
            headers,
            contentType,
            OptionalBody.body(httpResponse.getContentAsByteArray(), contentType)
        );
    }
}
