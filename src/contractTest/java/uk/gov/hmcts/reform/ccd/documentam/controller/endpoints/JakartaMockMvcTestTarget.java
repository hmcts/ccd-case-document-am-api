package uk.gov.hmcts.reform.ccd.documentam.controller.endpoints;

import au.com.dius.pact.core.model.ContentType;
import au.com.dius.pact.core.model.IRequest;
import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.OptionalBody;
import au.com.dius.pact.core.model.Pact;
import au.com.dius.pact.core.model.PactSource;
import au.com.dius.pact.core.model.SynchronousRequestResponse;
import au.com.dius.pact.core.model.generators.GeneratorTestMode;
import au.com.dius.pact.provider.IProviderVerifier;
import au.com.dius.pact.provider.ProviderInfo;
import au.com.dius.pact.provider.ProviderResponse;
import au.com.dius.pact.provider.junit5.TestTarget;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.ContentDisposition;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.ParseException;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.servlet.http.Cookie;
import kotlin.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.anything;

public class JakartaMockMvcTestTarget implements TestTarget {
    private MockMvc mockMvc;
    private List<Object> controllers = new ArrayList<>();
    private List<Object> controllerAdvices = new ArrayList<>();
    private List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
    private boolean printRequestResponse;

    @Override
    public Map<String, Object> getUserConfig() {
        return Collections.emptyMap();
    }

    @Override
    public ProviderInfo getProviderInfo(final String serviceName, final PactSource pactSource) {
        return new ProviderInfo(serviceName);
    }

    @Override
    public Pair<Object, Object> prepareRequest(final Pact pact, final Interaction interaction,
                                               final Map<String, Object> context) {
        if (interaction instanceof SynchronousRequestResponse requestResponse) {
            IRequest request = requestResponse.getRequest().generatedRequest(context, GeneratorTestMode.Provider);
            return new Pair<>(toMockRequestBuilder(request), buildMockMvc());
        }

        throw new UnsupportedOperationException("Only request/response interactions can be used with MockMvc");
    }

    public void setControllers(final Object... controllers) {
        this.controllers = List.of(controllers);
    }

    public void setControllerAdvices(final Object... controllerAdvices) {
        this.controllerAdvices = List.of(controllerAdvices);
    }

    public void setMessageConverters(final HttpMessageConverter<?>... messageConverters) {
        this.messageConverters = List.of(messageConverters);
    }

    public void setPrintRequestResponse(final boolean printRequestResponse) {
        this.printRequestResponse = printRequestResponse;
    }

    private MockMvc buildMockMvc() {
        if (mockMvc != null) {
            return mockMvc;
        }

        return MockMvcBuilders.standaloneSetup(controllers.toArray())
            .setControllerAdvice(controllerAdvices.toArray())
            .setMessageConverters(messageConverters.toArray(new HttpMessageConverter<?>[0]))
            .defaultRequest(buildDefaultRequest())
            .build();
    }

    private MockHttpServletRequestBuilder buildDefaultRequest() {
        return MockMvcRequestBuilders.get("/");
    }

    private MockHttpServletRequestBuilder toMockRequestBuilder(final IRequest request) {
        OptionalBody body = request.getBody();
        Cookie[] cookies = cookies(request);
        MockHttpServletRequestBuilder requestBuilder;

        if (body.isPresent()) {
            requestBuilder = request.isMultipartFileUpload()
                ? multipartRequestBuilder(request)
                : requestBuilderWithBody(request);
        } else {
            requestBuilder = MockMvcRequestBuilders
                .request(HttpMethod.valueOf(request.getMethod()), requestUri(request))
                .headers(mapHeaders(request, false));
        }

        if (cookies.length > 0) {
            requestBuilder.cookie(cookies);
        }

        return requestBuilder;
    }

    private MockHttpServletRequestBuilder requestBuilderWithBody(final IRequest request) {
        return MockMvcRequestBuilders
            .request(HttpMethod.valueOf(request.getMethod()), requestUri(request))
            .headers(mapHeaders(request, true))
            .content(request.getBody().getValue());
    }

    private MockHttpServletRequestBuilder multipartRequestBuilder(final IRequest request) {
        try {
            MimeMultipart multipart = new MimeMultipart(
                new ByteArrayDataSource(request.getBody().unwrap(), request.asHttpPart().contentTypeHeader()));
            MockMultipartHttpServletRequestBuilder multipartRequest = MockMvcRequestBuilders.multipart(
                HttpMethod.valueOf(request.getMethod()), requestUri(request));

            for (int i = 0; i < multipart.getCount(); i++) {
                addMultipartPart(multipartRequest, multipart.getBodyPart(i));
            }

            return multipartRequest.headers(mapHeaders(request, true));
        } catch (IOException | MessagingException exception) {
            throw new IllegalStateException("Failed to build Pact multipart request", exception);
        }
    }

    private void addMultipartPart(final MockMultipartHttpServletRequestBuilder multipartRequest,
                                  final BodyPart bodyPart)
        throws IOException, MessagingException {

        ContentDisposition disposition = contentDisposition(bodyPart);
        String name = valueOrDefault(disposition.getParameter("name"), "file");
        String filename = disposition.getParameter("filename");
        byte[] content = FileCopyUtils.copyToByteArray(bodyPart.getInputStream());

        if (filename == null || filename.isBlank()) {
            multipartRequest.param(name, new String(content, charset(bodyPart)));
        } else {
            multipartRequest.file(new MockMultipartFile(name, filename, bodyPart.getContentType(), content));
        }
    }

    private ContentDisposition contentDisposition(final BodyPart bodyPart) throws MessagingException {
        String[] headers = bodyPart.getHeader("Content-Disposition");
        if (headers == null || headers.length == 0) {
            return new ContentDisposition();
        }

        try {
            return new ContentDisposition(headers[0]);
        } catch (ParseException exception) {
            throw new MessagingException("Invalid multipart Content-Disposition header", exception);
        }
    }

    private Charset charset(final BodyPart bodyPart) throws MessagingException {
        String contentType = bodyPart.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return StandardCharsets.UTF_8;
        }

        Charset charset = org.springframework.http.MediaType.parseMediaType(contentType).getCharset();
        return charset == null ? StandardCharsets.UTF_8 : charset;
    }

    private Cookie[] cookies(final IRequest request) {
        return request.cookies().stream()
            .map(cookie -> cookie.split("=", 2))
            .map(values -> new Cookie(values[0], values.length > 1 ? values[1] : ""))
            .toArray(Cookie[]::new);
    }

    private URI requestUri(final IRequest request) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath(request.getPath());

        request.getQuery().forEach((key, values) -> values.forEach(value -> {
            if (value == null) {
                uriBuilder.queryParam(key);
            } else {
                uriBuilder.queryParam(key, value);
            }
        }));

        return URI.create(uriBuilder.toUriString());
    }

    private HttpHeaders mapHeaders(final IRequest request, final boolean hasBody) {
        HttpHeaders headers = new HttpHeaders();

        request.getHeaders().forEach((key, values) -> headers.add(key, String.join(", ", values)));

        if (hasBody && !headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
            headers.add(HttpHeaders.CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
        }

        return headers;
    }

    private String valueOrDefault(final String value, final String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    @Override
    public boolean isHttpTarget() {
        return true;
    }

    @Override
    public ProviderResponse executeInteraction(final Object client, final Object request) {
        try {
            MockMvc mockMvcClient = (MockMvc) client;
            RequestBuilder requestBuilder = (MockHttpServletRequestBuilder) request;
            MvcResult mvcResult = performRequest(mockMvcClient, requestBuilder).andReturn();

            return handleResponse(mvcResult.getResponse());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to execute Pact interaction with MockMvc", exception);
        }
    }

    private ResultActions performRequest(final MockMvc mockMvc, final RequestBuilder requestBuilder) throws Exception {
        ResultActions resultActions = mockMvc.perform(requestBuilder);
        if (printRequestResponse) {
            resultActions.andDo(MockMvcResultHandlers.print());
        }

        if (resultActions.andReturn().getRequest().isAsyncStarted()) {
            return mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(resultActions
                .andExpect(MockMvcResultMatchers.request().asyncResult(anything()))
                .andReturn()));
        }

        return resultActions;
    }

    private ProviderResponse handleResponse(final MockHttpServletResponse httpResponse) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        httpResponse.getHeaderNames()
            .forEach(headerName -> headers.put(headerName, new ArrayList<>(httpResponse.getHeaders(headerName))));

        ContentType contentType = httpResponse.getContentType() == null || httpResponse.getContentType().isBlank()
            ? ContentType.getJSON()
            : ContentType.fromString(httpResponse.getContentType());

        return new ProviderResponse(
            httpResponse.getStatus(),
            headers,
            contentType,
            OptionalBody.body(httpResponse.getContentAsByteArray(), contentType));
    }

    @Override
    public void prepareVerifier(final IProviderVerifier verifier, final Object testInstance, final Pact pact) {
    }

    @Override
    public boolean supportsInteraction(final Interaction interaction) {
        return interaction instanceof SynchronousRequestResponse;
    }
}
