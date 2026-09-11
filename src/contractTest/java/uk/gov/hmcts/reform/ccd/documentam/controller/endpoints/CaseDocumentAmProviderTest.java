package uk.gov.hmcts.reform.ccd.documentam.controller.endpoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import org.apache.commons.collections4.map.HashedMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;
import uk.gov.hmcts.reform.ccd.documentam.dto.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.documentam.dto.UploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedService;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Classification;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.documentam.service.DocumentManagementService;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.SERVICE_PERMISSION_ERROR;

@ExtendWith(SpringExtension.class)
@Provider("case-document-am-api")
@PactBroker(url = "${PACT_BROKER_FULL_URL:http://localhost}",
    consumerVersionSelectors = {@VersionSelector(tag = "master")})
@ContextConfiguration(classes = {ContractConfig.class})
@IgnoreNoPactsToVerify
public class CaseDocumentAmProviderTest {

    private static final String CASE_TYPE_ID = "some-case-type-id";
    private static final String JURISDICTION_ID = "some-jurisdiction-id";
    private static final String CASE_ID = "some-case-id";
    private static final UUID DOCUMENT_ID_UUID = UUID.fromString("6c3c3906-2b51-468e-8cbb-a4002eded076");
    private static final byte[] PDF_CONTENT = "%PDF-1.4\n%%EOF".getBytes(StandardCharsets.UTF_8);
    private static final String FILE_NAME = "0000-claim.pdf";
    private static final String DOCUMENT_SELF_URL = "http://localhost:6680/cases/documents/" + DOCUMENT_ID_UUID;
    private static final String DOCUMENT_BINARY_URL = DOCUMENT_SELF_URL + "/binary";
    private static final String HASH_TOKEN = "hash-token";

    @Autowired
    DocumentManagementService documentManagementService;

    @Autowired
    CaseDocumentAmController caseDocumentAmController;

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        JakartaMockMvcTestTarget testTarget = new JakartaMockMvcTestTarget();
        //System.getProperties().setProperty("pact.verifier.publishResults", "true");
        testTarget.setControllers(caseDocumentAmController);
        testTarget.setMessageConverters(
            new ByteArrayHttpMessageConverter(),
            new ResourceHttpMessageConverter(),
            jsonMessageConverter());
        given(documentManagementService.uploadDocuments(any(DocumentUploadRequest.class))).willReturn(uploadResponse());
        if (context != null) {
            context.setTarget(testTarget);
        }
    }

    @State({"I have existing document"})
    public void toAssignUserToCase() throws IOException {

        Map<String, String> metadata = new HashedMap<>();
        metadata.put(Constants.METADATA_CASE_TYPE_ID, CASE_TYPE_ID);
        metadata.put(Constants.METADATA_JURISDICTION_ID, JURISDICTION_ID);
        metadata.put(Constants.METADATA_CASE_ID, CASE_ID);

        Document document = Document.builder()
            .metadata(metadata)
            .build();

        given(documentManagementService.getDocumentMetadata(DOCUMENT_ID_UUID)).willReturn(document);
        given(documentManagementService.checkServicePermission(document.getCaseTypeId(), document.getJurisdictionId(),
                                                               null, Permission.READ, SERVICE_PERMISSION_ERROR,
                                                               DOCUMENT_ID_UUID.toString())).willReturn(
                                                                   AuthorisedService.builder().build());

        ResponseEntity<ByteArrayResource> response = ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .body(new ByteArrayResource(PDF_CONTENT));
        given(documentManagementService.getDocumentBinaryContent(DOCUMENT_ID_UUID)).willReturn(response);
    }

    private UploadResponse uploadResponse() {
        Document.Links links = new Document.Links();
        Document.Link self = new Document.Link();
        Document.Link binary = new Document.Link();

        self.href = DOCUMENT_SELF_URL;
        binary.href = DOCUMENT_BINARY_URL;
        links.self = self;
        links.binary = binary;

        Document document = Document.builder()
            .classification(Classification.RESTRICTED)
            .size((long) "test document content".getBytes(StandardCharsets.UTF_8).length)
            .mimeType(MediaType.APPLICATION_PDF_VALUE)
            .originalDocumentName(FILE_NAME)
            .createdOn(Date.from(Instant.parse("2026-08-13T10:15:30Z")))
            .hashToken(HASH_TOKEN)
            .links(links)
            .build();

        return new UploadResponse(List.of(document));
    }

    private MappingJackson2HttpMessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setDateFormat(dateFormat);

        return new MappingJackson2HttpMessageConverter(objectMapper);
    }
}
