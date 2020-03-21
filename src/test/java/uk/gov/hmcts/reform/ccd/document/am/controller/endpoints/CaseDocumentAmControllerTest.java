package uk.gov.hmcts.reform.ccd.document.am.controller.endpoints;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.MetadataSearchCommand;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.model.UpdateDocumentCommand;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.Classifications;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.document.am.service.CaseDataStoreService;
import uk.gov.hmcts.reform.ccd.document.am.service.DocumentManagementService;
import uk.gov.hmcts.reform.ccd.document.am.service.common.ValidationService;

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
    private static final String DUMMY_ROLE = "dummyRole";
    private static final String BEFTA_CASETYPE_2 =  "BEFTA_CASETYPE_2";
    private static final String BEFTA_JURISDICTION_2 =  "BEFTA_JURISDICTION_2";
    private static final String USER_ID =  "userId";
    private static final String VALID_RESPONSE = "Valid Response from API";
    private static final String RESPONSE_CODE = "Status code is OK";
    private static final String NO_CONTENT_RESPONSE_CODE = "Status code is No Content";
    private static final String AUTHORIZATION = "Authorization";
    private static final String FORBIDDEN = "forbidden";

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        //when(ValidationService.validate("")).thenReturn(TRUE);
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
        doReturn(TRUE).when(documentManagementService)
            .checkUserPermission(setDocumentMetaData(),getUuid(),AUTHORIZATION, Permission.READ);


        ResponseEntity response = testee
            .getDocumentbyDocumentId(serviceAuthorization, getUuid(), AUTHORIZATION,"", "");

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
        doReturn(FALSE).when(documentManagementService)
            .checkUserPermission(setDocumentMetaData(),getUuid(),AUTHORIZATION, Permission.READ);


        Assertions.assertThrows(ForbiddenException.class, () -> {
            testee.getDocumentbyDocumentId(serviceAuthorization, getUuid(), AUTHORIZATION,"", "");
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
        doReturn(TRUE).when(documentManagementService)
            .checkUserPermission(setDocumentMetaData(),getUuid(),AUTHORIZATION, Permission.READ);
        doReturn(setDocumentBinaryContent("OK")).when(documentManagementService).getDocumentBinaryContent(getUuid());

        ResponseEntity<Object> response = testee.getDocumentBinaryContentbyDocumentId(
            serviceAuthorization,
            getUuid(),
            AUTHORIZATION,
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
        doReturn(setDocumentBinaryContent(FORBIDDEN)).when(documentManagementService).getDocumentBinaryContent(getUuid());


        Assertions.assertThrows(ForbiddenException.class, () -> {
            testee.getDocumentBinaryContentbyDocumentId(
                serviceAuthorization,
                getUuid(),
                AUTHORIZATION,
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
        doReturn(caseDocumentMetadata).when(caseDataStoreService).getCaseDocumentMetadata(CASE_ID, getUuid(), AUTHORIZATION);
        doReturn(setDocumentBinaryContent(FORBIDDEN)).when(documentManagementService).getDocumentBinaryContent(getUuid());

        Assertions.assertThrows(ForbiddenException.class, () -> {
            testee.getDocumentBinaryContentbyDocumentId(
                serviceAuthorization,
                getUuid(),
                AUTHORIZATION,
                "",
                ""
            );
        });

    }

    @Test
    @DisplayName("should get 204 when document delete is successful")
    public void shouldDeleteDocumentbyDocumentId() {
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        doReturn(TRUE).when(documentManagementService)
            .checkUserPermission(setDocumentMetaData(),getUuid(),AUTHORIZATION, Permission.UPDATE);
        doReturn(ResponseEntity.status(HttpStatus.NO_CONTENT).build()).when(documentManagementService)
            .deleteDocument(getUuid(),"","",true);

        ResponseEntity response = testee
            .deleteDocumentbyDocumentId("", AUTHORIZATION, getUuid(),"","", true);

        assertAll(
            () ->  assertNotNull(response, VALID_RESPONSE),
            () -> assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode(), NO_CONTENT_RESPONSE_CODE)
        );
    }

    @Test
    @DisplayName("should throw 403 forbidden when user doesn't have UPDATE permission on requested document")
    public void shouldNotDeleteDocumentbyDocumentId() {
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        doReturn(FALSE).when(documentManagementService)
            .checkUserPermission(setDocumentMetaData(),getUuid(),AUTHORIZATION, Permission.UPDATE);

        Assertions.assertThrows(ForbiddenException.class, () -> {
            testee.deleteDocumentbyDocumentId(
                serviceAuthorization,
                AUTHORIZATION,
                getUuid(),
                "",
                "",
                true
            );
        });

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
    public void shouldPatchMetaDataOnDocuments() {
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        DocumentMetadata body = null;
        ResponseEntity response = testee.patchMetaDataOnDocuments(body, "", "", "");

        assertAll(
            () -> assertNotNull(response, VALID_RESPONSE),
            () -> assertEquals(HttpStatus.OK, response.getStatusCode(), RESPONSE_CODE));
    }

    //Tests for UploadDocuments controller.
    @Test
    @DisplayName("Should throw 400 when the uploaded file is empty")
    public void shouldThrowBadRequestExceptionWhenUploadedFilesIsNull() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            testee.uploadDocuments(null, Classifications.PUBLIC.name(), Arrays.asList(DUMMY_ROLE), serviceAuthorization,
                                   BEFTA_CASETYPE_2, BEFTA_JURISDICTION_2, USER_ID, DUMMY_ROLE);
        });
    }

    @Test
    @DisplayName("Should throw 400 when user-roles are empty")
    public void shouldThrowBadRequestExceptionWhenUserRolesAreEmpty() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            testee.uploadDocuments(generateMultipartList(),
                                   Classifications.PUBLIC.name(), Arrays.asList(DUMMY_ROLE), serviceAuthorization,
                                   BEFTA_CASETYPE_2, "BEFTA@JURISDICTION_2$$$$", USER_ID, null);
        });
    }

    @Test
    @DisplayName("Should throw 400 when caseTypeId input is null")
    public void shouldThrowBadRequestExceptionWhenCaseTypeIdIsNull() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            testee.uploadDocuments(generateMultipartList(),
                                   Classifications.PUBLIC.name(), Arrays.asList(DUMMY_ROLE), serviceAuthorization,
                                   null, BEFTA_JURISDICTION_2, USER_ID, DUMMY_ROLE);
        });
    }

    @Test
    @DisplayName("Should throw 400 when caseTypeId input is malformed")
    public void shouldThrowBadRequestExceptionWhenCaseTypeIdIsMalformed() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            testee.uploadDocuments(generateMultipartList(),
                                   Classifications.PUBLIC.name(), Arrays.asList(DUMMY_ROLE), serviceAuthorization,
                                   "BEFTA_CASETYPE_2&&&&&&&&&", "BEFTA_JURISDICTION_2", USER_ID, DUMMY_ROLE);
        });
    }

    @Test
    @DisplayName("Should throw 400 when jurisdictionId input is null")
    public void shouldThrowBadRequestExceptionWhenJurisdictionIdIsNull() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            testee.uploadDocuments(generateMultipartList(),
                                   Classifications.PUBLIC.name(), Arrays.asList(DUMMY_ROLE), serviceAuthorization,
                                   BEFTA_CASETYPE_2, null, USER_ID, DUMMY_ROLE);
        });
    }

    @Test
    @DisplayName("Should throw 400 when jurisdictionId input is malformed")
    public void shouldThrowBadRequestExceptionWhenJurisdictionIdIsMalformed() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            testee.uploadDocuments(generateMultipartList(),
                                   Classifications.PUBLIC.name(), Arrays.asList(DUMMY_ROLE), serviceAuthorization,
                                   BEFTA_CASETYPE_2, "BEFTA@JURISDICTION_2$$$$", USER_ID, DUMMY_ROLE);
        });
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
        return CaseDocumentMetadata.builder().caseId(CASE_ID).document(document).build();
    }

    private ResponseEntity<ByteArrayResource> setDocumentBinaryContent(String responseType) {
        if (responseType.equals("OK")) {
            return new ResponseEntity<ByteArrayResource>(
                new ByteArrayResource("test document content".getBytes()),
                getHttpHeaders(),
                HttpStatus.OK
            );
        } else if (responseType.equals(FORBIDDEN)) {
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

    private List<MultipartFile> generateMultipartList() {
        ArrayList<MultipartFile> listFiles = new ArrayList<>();
        listFiles.add(new MockMultipartFile("file1", "test1.jpg",
                                            "image/jpeg", "HelloString".getBytes()));
        listFiles.add(new MockMultipartFile("file2", "test2.jpg",
                                            "image/jpeg", "HelloString2".getBytes()));
        return listFiles;
    }

    @Test
    void generateHashCode_HappyPath() {
        ResponseEntity<Object> responseEntity = testee.generateHashCode("", "", UUID.fromString(MATCHED_DOCUMENT_ID), BEFTA_CASETYPE_2, BEFTA_JURISDICTION_2);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test //this test returns an illegal argument exception because UUID.fromString() contains a throw for illegal arguments
    void generateHashCode_BadRequest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            testee.generateHashCode("", "", UUID.fromString("A.A"), BEFTA_CASETYPE_2, BEFTA_JURISDICTION_2);
        });
    }

    //this test and the next return Bad Request because the exception handler seems to be made in a way that it trys to return the actual exception thrown
    @Test
    void generateHashCode_BadRequest2() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            testee.generateHashCode("", "", UUID.fromString(MATCHED_DOCUMENT_ID), "A.A", BEFTA_JURISDICTION_2);
        });
    }

    @Test
    void generateHashCode_BadRequest3() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            testee.generateHashCode("","", UUID.fromString(MATCHED_DOCUMENT_ID), BEFTA_CASETYPE_2, "A.A");
        });
    }
}
