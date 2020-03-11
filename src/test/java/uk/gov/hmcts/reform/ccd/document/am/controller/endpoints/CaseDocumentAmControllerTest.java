package uk.gov.hmcts.reform.ccd.document.am.controller.endpoints;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.document.am.model.*;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.document.am.service.CaseDataStoreService;
import uk.gov.hmcts.reform.ccd.document.am.service.DocumentManagementService;
import uk.gov.hmcts.reform.ccd.document.am.service.common.ValidationService;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Arrays;
import java.util.UUID;
import java.util.Date;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class CaseDocumentAmControllerTest {
    @InjectMocks
    private transient CaseDocumentAmController testee;

    @Mock
    private transient DocumentManagementService documentManagementService;

    @Mock
    private transient ValidationService validationService;

    @Mock
    private transient CaseDataStoreService caseDataStoreService;

    private transient ResponseEntity responseEntity = new ResponseEntity(HttpStatus.OK);
    private transient String serviceAuthorization = "";

    private static final String MATCHED_DOCUMENT_ID = "41334a2b-79ce-44eb-9168-2d49a744be9c";
    private static final String UNMATCHED_DOCUMENT_ID = "41334a2b-79ce-44eb-9168-2d49a744be9d";
    private static final String CASE_ID = "1582550122096256";
    private static final String VALID_RESPONSE = "Valid Response from API";
    private static final String RESPONSE_CODE = "Status code is OK";

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(validationService.validate(any(String.class))).thenReturn(TRUE);
    }

    @Test
    public void shouldGetValidMetaDataResponse() {
        Optional<CaseDocumentMetadata> caseDocumentMetadata = Optional.ofNullable(getCaseDocumentMetadata(
            MATCHED_DOCUMENT_ID,
            Arrays.asList(
                Permission.CREATE,
                Permission.READ
            )
        ));
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        doReturn(TRUE).when(documentManagementService).checkUserPermission(setDocumentMetaData(),getUuid());


        ResponseEntity response = testee.getDocumentbyDocumentId(serviceAuthorization, getUuid(), "", "");

        assertAll(
            () ->  assertNotNull(response, "Valid Response from API"),
            () -> assertNotNull(response.getBody(), "Valid response body"),
            () -> assertEquals(HttpStatus.OK, response.getStatusCode(), "Status code is OK")
        );
    }

    @Test
    public void shouldNotGetValidMetaDataResponse() {
        Optional<CaseDocumentMetadata> caseDocumentMetadata = Optional.ofNullable(getCaseDocumentMetadata(
            UNMATCHED_DOCUMENT_ID,
            Arrays.asList(
                Permission.CREATE,
                Permission.READ
            )
        ));
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        doReturn(FALSE).when(documentManagementService).checkUserPermission(setDocumentMetaData(),getUuid());


        Assertions.assertThrows(ForbiddenException.class, () -> {
            testee.getDocumentbyDocumentId(serviceAuthorization, getUuid(), "", "");
        });
    }

    @Test
    @DisplayName("should get 200 document binary content")
    public void shouldGetDocumentBinaryContent() {
        Optional<CaseDocumentMetadata> caseDocumentMetadata = Optional.ofNullable(getCaseDocumentMetadata(
            MATCHED_DOCUMENT_ID,
            Arrays.asList(
               Permission.CREATE,
               Permission.READ
           )
        ));
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        doReturn(TRUE).when(documentManagementService).checkUserPermission(setDocumentMetaData(),getUuid());
        doReturn(setDocumentBinaryContent("OK")).when(documentManagementService).getDocumentBinaryContent(getUuid());

        ResponseEntity<Object> response = testee.getDocumentBinaryContentbyDocumentId(
            serviceAuthorization,
            getUuid(),
            "",
            ""
        );

        assertAll(
            () -> verify(documentManagementService).getDocumentMetadata(getUuid()),
            () -> verify(documentManagementService, times(1)).getDocumentBinaryContent(getUuid()),
            () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
            () -> assertNotNull(response.getBody())
        );
    }

    @Test
    @DisplayName("should throw 403 forbidden  when the requested document does not have read permission")
    public void shouldThrowForbiddenWhenDocumentDoesNotHaveReadPermission() {
        Optional<CaseDocumentMetadata> caseDocumentMetadata = Optional.ofNullable(getCaseDocumentMetadata(
            MATCHED_DOCUMENT_ID,
            Arrays.asList(Permission.CREATE)
        ));
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        doReturn(setDocumentBinaryContent("forbidden")).when(documentManagementService).getDocumentBinaryContent(getUuid());


        Assertions.assertThrows(ForbiddenException.class, () -> {
            testee.getDocumentBinaryContentbyDocumentId(
                serviceAuthorization,
                getUuid(),
                "",
                ""
            );
        });


    }


    @Test
    @DisplayName("should throw 403 forbidden when the requested document does not match with available doc")
    public void shouldThrowForbiddenWhenDocumentDoesNotMatch() {
        Optional<CaseDocumentMetadata> caseDocumentMetadata = Optional.ofNullable(getCaseDocumentMetadata(
            UNMATCHED_DOCUMENT_ID,
            Arrays.asList(
                Permission.CREATE,
                Permission.READ
            )
        ));

        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        doReturn(CASE_ID).when(documentManagementService).extractCaseIdFromMetadata(setDocumentMetaData().getBody());
        doReturn(caseDocumentMetadata).when(caseDataStoreService).getCaseDocumentMetadata(CASE_ID, getUuid());
        doReturn(setDocumentBinaryContent("forbidden")).when(documentManagementService).getDocumentBinaryContent(getUuid());

        Assertions.assertThrows(ForbiddenException.class, () -> {
            testee.getDocumentBinaryContentbyDocumentId(
                serviceAuthorization,
                getUuid(),
                "",
                ""
            );
        });

    }

    @Test
    public void shouldDeleteDocumentbyDocumentId() {
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        ResponseEntity response = testee.deleteDocumentbyDocumentId("", getUuid(), TRUE, "", "");

        assertAll(
            () ->  assertNotNull(response, VALID_RESPONSE),
            () -> assertEquals(HttpStatus.OK, response.getStatusCode(), RESPONSE_CODE)
        );
    }

    @Test
    public void shouldPatchDocumentbyDocumentId() {
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        UpdateDocumentCommand body = null;
        ResponseEntity response = testee.patchDocumentbyDocumentId(body,"", getUuid(), "", "");

        assertAll(
            () ->  assertNotNull(response, "Valid Response from API"),
            () -> assertEquals(HttpStatus.OK, response.getStatusCode(), RESPONSE_CODE)
        );
    }

    @Test
    public void shouldPostDocumentsSearchCommand() {
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        MetadataSearchCommand body = null;
        ResponseEntity response = testee.postDocumentsSearchCommand(body,"", "", "", 10L, 10, 10, TRUE, TRUE, TRUE, TRUE);

        assertAll(
            () ->  assertNotNull(response, VALID_RESPONSE),
            () -> assertEquals(HttpStatus.OK, response.getStatusCode(), RESPONSE_CODE)
        );
    }

    @Test
    public void shouldPostDocumentsWithBinaryFile() {
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        MetadataSearchCommand body = null;
        ResponseEntity response = testee.postDocumentsWithBinaryFile("", new Date(), new ArrayList<>(), new ArrayList<>(), "", "", "", "", "");

        assertAll(
            () ->  assertNotNull(response, VALID_RESPONSE),
            () -> assertEquals(HttpStatus.OK, response.getStatusCode(), RESPONSE_CODE)
        );
    }



    @Test
    public void shouldPatchMetaDataOnDocuments() {
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        CaseDocumentMetadata body = null;
        ResponseEntity response = testee.patchMetaDataOnDocuments(body,"", "", "");

        assertAll(
            () ->  assertNotNull(response, VALID_RESPONSE),
            () -> assertEquals(HttpStatus.OK, response.getStatusCode(), RESPONSE_CODE)
        );
    }

    private ResponseEntity<StoredDocumentHalResource> setDocumentMetaData() {
        StoredDocumentHalResource resource = new StoredDocumentHalResource();
        resource.setCreatedBy("test");
        resource.setOriginalDocumentName("test.png");
        return new ResponseEntity<StoredDocumentHalResource>(resource, HttpStatus.OK);
    }

    private UUID getUuid() {
        return UUID.fromString(MATCHED_DOCUMENT_ID);
    }

    private CaseDocumentMetadata getCaseDocumentMetadata(String docId, List<Permission> permission) {
        Document document = Document.builder().permissions(permission).id(docId).build();
        return CaseDocumentMetadata.builder().caseId(CASE_ID).document(Optional.of(document)).build();
    }

    private ResponseEntity<ByteArrayResource> setDocumentBinaryContent(String responseType) {
        if (responseType.equals("OK")) {
            return new ResponseEntity<ByteArrayResource>(
                new ByteArrayResource("test document content".getBytes()),
                getHttpHeaders(),
                HttpStatus.OK
            );
        } else if (responseType.equals("forbidden")) {
            return new ResponseEntity<ByteArrayResource>(
                new ByteArrayResource("".getBytes()),
                getHttpHeaders(),
                HttpStatus.FORBIDDEN
            );
        }

        return new ResponseEntity<ByteArrayResource>(new ByteArrayResource("".getBytes()), HttpStatus.OK);
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("OriginalFileName", "OriginalFileName");
        headers.set("Content-Disposition", "Content-Disposition");
        headers.set("data-source", "data-source");
        headers.set("Content-Length", "25");
        headers.set("Content-Type", "application/json");
        return headers;
    }
}
