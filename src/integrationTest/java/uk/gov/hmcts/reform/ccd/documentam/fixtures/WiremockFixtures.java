package uk.gov.hmcts.reform.ccd.documentam.fixtures;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.DmUploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentPermissions;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static uk.gov.hmcts.reform.ccd.documentam.TestFixture.objectToJsonString;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.BEARER;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.SERVICE_AUTHORIZATION;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.ExcessiveImports"})
public class WiremockFixtures implements TestFixture {
    public static final String DOCUMENTS_URL = "/documents/";

    public static final String TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE1ODM0NDUyOTd9"
        + ".WWRzROlKxLQCJw5h0h0dHb9hHfbBhF2Idwv1z4L4FnqSw3VZ38ZRLuDmwr3tj-8oOv6EfLAxV0dJAPtUT203Iw";
    public static final String SERVICE_AUTHORISATION_VALUE = BEARER + TOKEN;

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
                                    .withBody(objectToJsonString(body))
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
                                    .withBody(objectToJsonString(dmUploadResponse))));
    }

    public static void stubGetDocumentMetaData(final Document document) {
        stubFor(WireMock.get(urlPathEqualTo(DOCUMENTS_URL + DOCUMENT_ID))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK)
                                    .withBody(objectToJsonString(document))
                                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubGetDocumentMetaData(final Document document, final UUID documentId) {
        stubFor(WireMock.get(urlPathEqualTo(DOCUMENTS_URL + documentId))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK)
                                    .withBody(objectToJsonString(document))
                                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubDeleteDocumentByDocumentId() {
        stubFor(WireMock.delete(urlPathEqualTo(DOCUMENTS_URL + DOCUMENT_ID))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .withQueryParam("permanent", equalTo("false"))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_NO_CONTENT)));
    }

    public static void stubNotFoundDeleteDocumentByDocumentId(final UUID random) {
        final ObjectNode objectNode = TestFixture.objectMapper().createObjectNode();
        objectNode.put("status", 404);
        objectNode.put("path", DOCUMENTS_URL + random);
        objectNode.put("error", "Delete error");
        objectNode.put("exception", "not.found.Exception");
        objectNode.put("timestamp", "2021-07-30T08:23:34+0000");

        stubFor(WireMock.delete(urlPathEqualTo(DOCUMENTS_URL + random))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .withQueryParam("permanent", equalTo("false"))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_NOT_FOUND)
                                    .withBody(objectToJsonString(objectNode))));
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

    public static void stubPatchDocument(final PatchDocumentResponse patchDocumentResponse) {
        stubFor(WireMock.patch(urlPathEqualTo(DOCUMENTS_URL + DOCUMENT_ID))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK)
                                    .withBody(objectToJsonString(patchDocumentResponse))
                                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubPatchDocumentMetaData(final Document response) {
        stubFor(WireMock.patch(urlPathEqualTo("/documents"))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
                    .withRequestBody(containing("\"ttl\":null"))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK)
                                    .withBody(objectToJsonString(response))
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

}
