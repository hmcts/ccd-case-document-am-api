package uk.gov.hmcts.reform.ccd.documentam.consumer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import io.vavr.control.Either;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.documentam.ApplicationParams;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;
import uk.gov.hmcts.reform.ccd.documentam.client.dmstore.DocumentStoreClient;
import uk.gov.hmcts.reform.ccd.documentam.exception.ResourceNotFoundException;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentUpdate;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentsCommand;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "em_dm_store")
public class DmStoreApiConsumerTest {

    /*
    consumer name = ccd_caseDocumentApi
    The following api interactions exercised here
        GET /documents/{documentId} - state "document exists to GET"
        DELETE /documents/{documentId}?permanent={boolean} - state "document exists to DELETE"
        PATCH /documents/ - state "a list of documents exist"
        POST /documents/ - this can't be expressed with current version of pact 4.6.17. There is
                           no way to express multipart/form-data in a dynamic way. Hardcoding the whole
                           request is not an option as existing POST method adds timestamp with value
                           of now()

     */

    private SecurityUtils securityUtils = mock(SecurityUtils.class);
    private ApplicationParams applicationParams = mock(ApplicationParams.class);
    private CloseableHttpClient httpClient = mock(CloseableHttpClient.class);

    private DocumentStoreClient documentStoreClient;

    public static final String DUMMY_TOKEN = "Bearer some-dummy-token";
    public static final String DOC_ID = "3b33c9c3-17f3-49ea-8423-9bb1a81ffde5";
    public static final String CONTENT_TYPE_REGEX_DOCUMENT =
        "application/vnd\\.uk\\.gov\\.hmcts\\.dm\\.document\\.v1\\+hal\\+json;charset=UTF-8";

    @BeforeEach
    void setUp() {
        var factory = new HttpComponentsClientHttpRequestFactory(HttpClients.createDefault());
        documentStoreClient = new DocumentStoreClient(
            securityUtils, new RestTemplate(factory), httpClient, applicationParams);
        HttpHeaders headers = new HttpHeaders();
        headers.add(Constants.SERVICE_AUTHORIZATION, DUMMY_TOKEN);

        when(securityUtils.serviceAuthorizationHeaders()).thenReturn(headers);
    }

    void commonSetup(MockServer mockServer) {
        when(applicationParams.getDocumentURL()).thenReturn(mockServer.getUrl());
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUid()).thenReturn(UUID.randomUUID().toString());
        when(securityUtils.getUserInfo()).thenReturn(userInfo);
    }

    @Pact(consumer = "ccd_caseDocumentApi")
    V4Pact getDocumentByIdPact(PactDslWithProvider builder) {
        return builder
            .given("document exists to GET") // state
            .uponReceiving("get document metadata by id request") // description
            .pathFromProviderState("/documents/${documentId}", "/documents/" + DOC_ID)
            .method("GET")
            .matchHeader(Constants.SERVICE_AUTHORIZATION, DUMMY_TOKEN)
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .matchHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_REGEX_DOCUMENT)
            .body(createDmStoreApiGetDocumentResponse())
            .toPact(V4Pact.class);
    }

    @Pact(consumer = "ccd_caseDocumentApi")
    V4Pact deleteDocumentByIdPact(PactDslWithProvider builder) {
        return builder
            .given("document exists to DELETE") // state
            .uponReceiving("delete document by id request") // description
            .pathFromProviderState("/documents/${documentId}", "/documents/" + DOC_ID)
            .query("permanent=true")
            .method("DELETE")
            .matchHeader(Constants.SERVICE_AUTHORIZATION, DUMMY_TOKEN)
            .willRespondWith()
            .status(HttpStatus.NO_CONTENT.value())
            .toPact(V4Pact.class);
    }

    @Pact(consumer = "ccd_caseDocumentApi")
    V4Pact patchListOfDocumentsPact(PactDslWithProvider builder) {
        return builder
            .given("a list of documents exist") // state
            .uponReceiving("patch multiple documents request") // description
            .path("/documents")
            .method("PATCH")
            .matchHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .matchHeader(Constants.SERVICE_AUTHORIZATION, DUMMY_TOKEN)
            .body(createPatchListOfDocumentsRequestBody())
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .matchHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(new PactDslJsonBody()
                .stringType("result", "Success"))
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getDocumentByIdPact")
    void getDocumentByIdPactTest(MockServer mockServer) {
        commonSetup(mockServer);
        Either<ResourceNotFoundException, Document> response =
            documentStoreClient.getDocument(UUID.fromString(DOC_ID));
        assertThat(response.get()).isNotNull();
    }

    @Test
    @PactTestFor(pactMethod = "deleteDocumentByIdPact")
    void deleteDocumentByIdPactTest(MockServer mockServer) {
        commonSetup(mockServer);
        assertThatCode(() -> documentStoreClient.deleteDocument(UUID.fromString(DOC_ID), true))
            .doesNotThrowAnyException();
    }

    @Test
    @PactTestFor(pactMethod = "patchListOfDocumentsPact")
    void patchListOfDocumentsPactTest(MockServer mockServer) {
        commonSetup(mockServer);
        List<DocumentUpdate> documents = List.of(
            new DocumentUpdate(UUID.fromString(DOC_ID), Map.of("case_type_id", "CIVIL")),
            new DocumentUpdate(UUID.fromString(DOC_ID), Map.of("jurisdiction", "CIVIL"))
        );
        UpdateDocumentsCommand command = new UpdateDocumentsCommand(new Date(System.currentTimeMillis()), documents);
        assertThatCode(() -> documentStoreClient.patchDocumentMetadata(command))
            .doesNotThrowAnyException();
    }

    private PactDslJsonBody createPatchListOfDocumentsRequestBody() {
        return new PactDslJsonBody()
            .numberType("ttl", System.currentTimeMillis())
            .minArrayLike("documents", 1, 2)
            .stringType("documentId", DOC_ID)
            .object("metadata")
                .eachKeyLike("anyKey", PactDslJsonRootValue.stringType("anyValue"))
            .closeObject()
            .closeArray()
            .asBody();
    }

    private PactDslJsonBody createDmStoreApiGetDocumentResponse() {
        return new PactDslJsonBody()
            .numberType("size")
            .stringType("mimeType", "application/pdf")
            .stringType("originalDocumentName", "hearingAdjourned 8 Jun 2025 0942.pdf")
            .stringType("createdBy", "22cb52eb-9490-42ce-837b-58b81702855a")
            .stringType("lastModifiedBy", "22cb52eb-9490-42ce-837b-58b81702855a")
            .datetime("modifiedOn", "yyyy-MM-dd'T'HH:mm:ssZ")
            .datetime("createdOn", "yyyy-MM-dd'T'HH:mm:ssZ")
            .stringMatcher("classification", "PUBLIC|RESTRICTED|PRIVATE", "PUBLIC")
            .array("roles").stringType("caseworker").closeArray()
            .object("metadata")
                .stringType("case_id", "1742576321265670")
                .stringType("case_type_id", "Asylum")
                .stringType("jurisdiction", "IA")
            .closeObject()
            .object("_links")
                .object("self")
                    .stringType("href", "http://url/documents/" + DOC_ID)
                .closeObject()
                .object("binary")
                    .stringType("href", "http://url/documents/" + DOC_ID + "/binary")
                .closeObject()
            .closeObject()
            .asBody();
    }

}
