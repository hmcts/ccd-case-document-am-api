package uk.gov.hmcts.reform.ccd.documentam.controller.endpoints;

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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;
import uk.gov.hmcts.reform.ccd.documentam.dto.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.documentam.dto.UploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedService;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Classification;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.ccd.documentam.service.DocumentManagementService;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

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
    private static final String HASH_TOKEN = "some-hash-token";
    private static final UUID DOCUMENT_ID_UUID = UUID.fromString("6c3c3906-2b51-468e-8cbb-a4002eded076");
    private static final String DOCUMENT_URL = "http://dm-store/documents/" + DOCUMENT_ID_UUID;
    private static final byte[] PDF_CONTENT = """
        %PDF-1.4
        1 0 obj
        << /Type /Catalog /Pages 2 0 R >>
        endobj
        2 0 obj
        << /Type /Pages /Count 0 >>
        endobj
        trailer
        << /Root 1 0 R >>
        %%EOF
        """.getBytes(StandardCharsets.US_ASCII);

    @Autowired
    DocumentManagementService documentManagementService;

    @Autowired
    CaseDocumentAmController caseDocumentAmController;

    @Autowired
    SecurityUtils securityUtils;

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
        if (context != null) {
            context.setTarget(testTarget);
        }
    }

    @State({"I have existing document"})
    public void toAssignUserToCase() {

        Map<String, String> metadata = new HashedMap<>();
        metadata.put(Constants.METADATA_CASE_TYPE_ID, CASE_TYPE_ID);
        metadata.put(Constants.METADATA_JURISDICTION_ID, JURISDICTION_ID);
        metadata.put(Constants.METADATA_CASE_ID, CASE_ID);

        Document document = Document.builder()
            .metadata(metadata)
            .build();

        given(documentManagementService.getDocumentMetadata(DOCUMENT_ID_UUID)).willReturn(document);
        given(securityUtils.getServiceNameFromS2SToken(any())).willReturn("civil_service");
        given(documentManagementService.checkServicePermission(any(), any(), any(), any(), any(), any()))
            .willReturn(AuthorisedService.builder().build());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        ResponseEntity<ByteArrayResource> response = new ResponseEntity<>(
            new ByteArrayResource(PDF_CONTENT),
            headers,
            HttpStatus.OK
        );
        given(documentManagementService.getDocumentBinaryContent(DOCUMENT_ID_UUID)).willAnswer(x -> response);

        given(documentManagementService.uploadDocuments(any(DocumentUploadRequest.class)))
            .willReturn(new UploadResponse(List.of(
                Document.builder()
                    .classification(Classification.PUBLIC)
                    .mimeType(MediaType.APPLICATION_PDF_VALUE)
                    .originalDocumentName("test.pdf")
                    .size((long) PDF_CONTENT.length)
                    .hashToken(HASH_TOKEN)
                    .metadata(metadata)
                    .links(documentLinks())
                    .build()
            )));
    }

    private Document.Links documentLinks() {
        Document.Link self = new Document.Link();
        self.href = DOCUMENT_URL;

        Document.Link binary = new Document.Link();
        binary.href = DOCUMENT_URL + "/binary";

        Document.Links links = new Document.Links();
        links.self = self;
        links.binary = binary;

        return links;
    }
}
