package uk.gov.hmcts.reform.ccd.documentam.fixtures;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentPermissions;
import uk.gov.hmcts.reform.ccd.documentam.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.ExcessiveImports"})
public class WiremockFixtures {

    public static final String MAIN_URL = "/cases/documents";
    public static final String DOCUMENTS_URL = "/documents/";

    public static final UUID DOCUMENT_ID = UUID.randomUUID();
    public static final String CASE_ID = "1584722156538291";

    public static final String BEARER = "Bearer ";
    public static final String TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE1ODM0NDUyOTd9"
        + ".WWRzROlKxLQCJw5h0h0dHb9hHfbBhF2Idwv1z4L4FnqSw3VZ38ZRLuDmwr3tj-8oOv6EfLAxV0dJAPtUT203Iw";
    public static final String SERVICE_AUTHORISATION_VALUE = BEARER + TOKEN;

    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    public static final ObjectMapper OBJECT_MAPPER = new Jackson2ObjectMapperBuilder()
        .modules(new Jdk8Module())
        .build();


    private WiremockFixtures() {
    }

    // Same issue as here https://github.com/tomakehurst/wiremock/issues/97
    public static class ConnectionClosedTransformer extends ResponseDefinitionTransformer {

        @Override
        public String getName() {
            return "keep-alive-disabler";
        }

        @Override
        public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition,
                                            FileSource files, Parameters parameters) {
            return ResponseDefinitionBuilder.like(responseDefinition)
                .withHeader(HttpHeaders.CONNECTION, "close")
                .build();
        }
    }

    public static void stubDocumentUrl() {
        CaseDocumentMetadata caseDocumentMetadata = new CaseDocumentMetadata();
        caseDocumentMetadata.setCaseId(CASE_ID);

        List<Permission> permissionList = new ArrayList<>();
        permissionList.add(Permission.READ);
        DocumentPermissions documentPermissions = new DocumentPermissions();
        documentPermissions.setId(DOCUMENT_ID.toString());
        documentPermissions.setPermissions(permissionList);

        caseDocumentMetadata.setDocumentPermissions(documentPermissions);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("documentMetadata", caseDocumentMetadata);

        stubFor(WireMock.get(urlPathEqualTo("/cases/" + CASE_ID + DOCUMENTS_URL + DOCUMENT_ID))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .willReturn(aResponse()
                                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .withStatus(HttpStatus.OK.value())
                                    .withBody(getJsonString(body))
                    )
        );
    }

    public static void stubUploadDocument() {
        stubFor(post(urlPathEqualTo(MAIN_URL))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK)
                                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubDocumentManagementUploadDocument() {
        stubFor(post(urlPathEqualTo("/documents"))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK)
                                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubGetDocumentMetaData(StoredDocumentHalResource storedDocumentHalResource) {
        stubFor(WireMock.get(urlPathEqualTo(DOCUMENTS_URL + DOCUMENT_ID))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK)
                                    .withBody(getJsonString(storedDocumentHalResource))
                                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubDeleteDocumentByDocumentId() {
        stubFor(WireMock.delete(urlPathEqualTo(DOCUMENTS_URL + DOCUMENT_ID))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .withQueryParam("permanent", equalTo("false"))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_NO_CONTENT)));
    }

    public static void stubDocumentBinaryContent() {
        stubFor(WireMock.get(urlPathEqualTo(DOCUMENTS_URL + DOCUMENT_ID + "/binary"))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK)
                                    .withHeader("OriginalFileName", "")
                                    .withHeader("Content-Disposition", "")
                                    .withHeader("Data-Source", "")
                                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubPatchDocument(StoredDocumentHalResource storedDocumentHalResource) {
        stubFor(WireMock.patch(urlPathEqualTo(DOCUMENTS_URL + DOCUMENT_ID))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK)
                                    .withBody(getJsonString(storedDocumentHalResource))
                                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubPatchDocumentMetaData(StoredDocumentHalResource storedDocumentHalResource) {
        stubFor(WireMock.patch(urlPathEqualTo("/documents"))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK)
                                    .withBody(getJsonString(storedDocumentHalResource))
                                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    @SuppressWarnings({"PMD.AvoidThrowingRawExceptionTypes", "squid:S112"})
    // Required as wiremock's Json.getObjectMapper().registerModule(..); not working
    // see https://github.com/tomakehurst/wiremock/issues/1127
    public static String getJsonString(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
