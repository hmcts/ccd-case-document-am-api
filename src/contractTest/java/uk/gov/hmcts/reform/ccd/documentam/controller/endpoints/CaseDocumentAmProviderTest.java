package uk.gov.hmcts.reform.ccd.documentam.controller.endpoints;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.spring6.Spring6MockMvcTestTarget;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.collections4.map.HashedMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;
import uk.gov.hmcts.reform.ccd.documentam.dto.UploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedService;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Classification;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.documentam.service.DocumentManagementService;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
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
    // Minimal PDF magic bytes so Pact contentType matching accepts application/pdf
    private static final byte[] FILE_CONTENT = "%PDF-1.4\ntest document content\n%%EOF"
        .getBytes(StandardCharsets.US_ASCII);

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
        ObjectMapper objectMapper = new ObjectMapper()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        Spring6MockMvcTestTarget testTarget = new Spring6MockMvcTestTarget();
        testTarget.setControllers(caseDocumentAmController);
        testTarget.setMessageConverters(
            new ByteArrayHttpMessageConverter(),
            new ResourceHttpMessageConverter(),
            new MappingJackson2HttpMessageConverter(objectMapper)
        );
        if (context != null) {
            context.setTarget(testTarget);
        }

        // Upload interaction has no provider state, so stub it for every verification.
        given(documentManagementService.checkServicePermission(
            eq("CIVIL"),
            eq("CIVIL"),
            any(),
            eq(Permission.CREATE),
            eq(SERVICE_PERMISSION_ERROR),
            eq("CIVIL CIVIL")
        )).willReturn(AuthorisedService.builder().build());

        Document.Links links = new Document.Links();
        links.self = new Document.Link();
        links.self.href = "http://localhost:6680/cases/documents/" + DOCUMENT_ID_UUID;
        links.binary = new Document.Link();
        links.binary.href = "http://localhost:6680/cases/documents/" + DOCUMENT_ID_UUID + "/binary";

        Document uploadedDocument = Document.builder()
            .classification(Classification.RESTRICTED)
            .size(21L)
            .mimeType(MediaType.APPLICATION_PDF_VALUE)
            .originalDocumentName("0000-claim.pdf")
            .hashToken("hash-token")
            .createdOn(Date.from(Instant.parse("2026-08-13T10:15:30.000Z")))
            .links(links)
            .build();
        given(documentManagementService.uploadDocuments(any()))
            .willReturn(new UploadResponse(List.of(uploadedDocument)));
    }

    @State({"I have existing document"})
    public void existingDocument() {
        Map<String, String> metadata = new HashedMap<>();
        metadata.put(Constants.METADATA_CASE_TYPE_ID, CASE_TYPE_ID);
        metadata.put(Constants.METADATA_JURISDICTION_ID, JURISDICTION_ID);
        metadata.put(Constants.METADATA_CASE_ID, CASE_ID);

        Document document = Document.builder()
            .metadata(metadata)
            .build();

        given(documentManagementService.getDocumentMetadata(DOCUMENT_ID_UUID)).willReturn(document);

        // securityUtils is left unstubbed, so service name is null (same as previous style)
        given(documentManagementService.checkServicePermission(
            document.getCaseTypeId(),
            document.getJurisdictionId(),
            null,
            Permission.READ,
            SERVICE_PERMISSION_ERROR,
            DOCUMENT_ID_UUID.toString()
        )).willReturn(AuthorisedService.builder().build());

        given(documentManagementService.checkServicePermission(
            document.getCaseTypeId(),
            document.getJurisdictionId(),
            null,
            Permission.UPDATE,
            SERVICE_PERMISSION_ERROR,
            DOCUMENT_ID_UUID.toString()
        )).willReturn(AuthorisedService.builder().build());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        ResponseEntity<ByteArrayResource> binaryResponse = new ResponseEntity<>(
            new ByteArrayResource(FILE_CONTENT),
            headers,
            HttpStatus.OK
        );
        given(documentManagementService.getDocumentBinaryContent(DOCUMENT_ID_UUID)).willReturn(binaryResponse);

        doNothing().when(documentManagementService).deleteDocument(eq(DOCUMENT_ID_UUID), any());
    }
}
