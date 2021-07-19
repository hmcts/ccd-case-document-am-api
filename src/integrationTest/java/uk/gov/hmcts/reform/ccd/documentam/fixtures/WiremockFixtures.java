package uk.gov.hmcts.reform.ccd.documentam.fixtures;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.client.dmstore.DmUploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentPermissions;
import uk.gov.hmcts.reform.ccd.documentam.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.BEARER;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.SERVICE_AUTHORIZATION;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.ExcessiveImports"})
public class WiremockFixtures implements TestFixture {
    public static final String DOCUMENTS_URL = "/documents/";

    public static final String TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE1ODM0NDUyOTd9"
        + ".WWRzROlKxLQCJw5h0h0dHb9hHfbBhF2Idwv1z4L4FnqSw3VZ38ZRLuDmwr3tj-8oOv6EfLAxV0dJAPtUT203Iw";
    public static final String SERVICE_AUTHORISATION_VALUE = BEARER + TOKEN;

    public static final ObjectMapper OBJECT_MAPPER = new Jackson2ObjectMapperBuilder()
        .modules(new Jdk8Module())
        .build();

    private WiremockFixtures() {
    }

    private static void stubDocumentUrl(CaseDocumentMetadata caseDocumentMetadata) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("documentMetadata", caseDocumentMetadata);

        stubFor(WireMock.get(urlPathEqualTo("/cases/" + CASE_ID_VALUE + DOCUMENTS_URL + DOCUMENT_ID))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .willReturn(aResponse()
                                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .withStatus(HttpStatus.OK.value())
                                    .withBody(getJsonString(body))
                    )
        );
    }

    public static void stubDocumentUrlWithReadPermissions() {
        final List<Permission> permissionList = List.of(Permission.READ);

        stubDocumentUrl(getCaseDocumentMetaData(permissionList));
    }

    public static void stubDocumentUrlNoPermissions() {
        ArrayList<Permission> permissionList = new ArrayList<>();

        stubDocumentUrl(getCaseDocumentMetaData(permissionList));
    }

    public static void stubDocumentManagementUploadDocument(DmUploadResponse dmUploadResponse) {
        stubFor(post(urlPathEqualTo("/documents"))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK)
                                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .withBody(getJsonString(dmUploadResponse))));
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

    public static void stubPatchDocumentMetaData(StoredDocumentHalResource response) {
        stubFor(WireMock.patch(urlPathEqualTo("/documents"))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .withRequestBody(containing("\"ttl\":null"))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK)
                                    .withBody(getJsonString(response))
                                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    private static CaseDocumentMetadata getCaseDocumentMetaData(final List<Permission> permissionList) {
        final DocumentPermissions documentPermissions = DocumentPermissions
            .builder()
            .id(DOCUMENT_ID)
            .permissions(permissionList)
            .build();

        return CaseDocumentMetadata
            .builder()
            .caseId(CASE_ID_VALUE)
            .documentPermissions(documentPermissions)
            .build();
    }

    public static void stubGetGreeting() {
        stubFor(WireMock.get(urlPathEqualTo("/greeting"))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK)
                                    .withBody("Hello World!")));
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
