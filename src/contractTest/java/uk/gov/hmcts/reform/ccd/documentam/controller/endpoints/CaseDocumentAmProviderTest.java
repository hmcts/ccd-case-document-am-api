package uk.gov.hmcts.reform.ccd.documentam.controller.endpoints;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import org.apache.commons.collections4.map.HashedMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedService;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.documentam.service.DocumentManagementService;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

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
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        //System.getProperties().setProperty("pact.verifier.publishResults", "true");
        testTarget.setControllers(caseDocumentAmController);
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

        ResponseEntity response = new ResponseEntity(HttpStatus.OK);
        given(documentManagementService.getDocumentBinaryContent(DOCUMENT_ID_UUID)).willAnswer(x -> response);
    }
}
