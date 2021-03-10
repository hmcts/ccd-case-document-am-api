package uk.gov.hmcts.reform.ccd.documentam.controller.endpoints;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentHashToken;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentPermissions;
import uk.gov.hmcts.reform.ccd.documentam.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.documentam.model.UpdateDocumentCommand;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Classification;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.ccd.documentam.service.CaseDataStoreService;
import uk.gov.hmcts.reform.ccd.documentam.service.DocumentManagementService;
import uk.gov.hmcts.reform.ccd.documentam.service.ValidationUtils;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.XUI_WEBAPP;

public class CaseDocumentAmControllerTest {
    private static final String MATCHED_DOCUMENT_ID = "41334a2b-79ce-44eb-9168-2d49a744be9c";
    private static final String UNMATCHED_DOCUMENT_ID = "41334a2b-79ce-44eb-9168-2d49a744be9d";
    private static final String CASE_ID = "1582550122096256";
    private static final String BEFTA_CASETYPE_2 = "BEFTA_CASETYPE_2";
    private static final String BEFTA_JURISDICTION_2 = "BEFTA_JURISDICTION_2";
    private static final String VALID_RESPONSE = "Valid Response from API";
    private static final String RESPONSE_CODE = "Status code is OK";
    private static final String NO_CONTENT_RESPONSE_CODE = "Status code is No Content";
    private static final String FORBIDDEN = "forbidden";
    private static final String TEST_S2S_TOKEN = "Test s2sToken";

    @InjectMocks
    private CaseDocumentAmController testee;
    @Mock
    private DocumentManagementService documentManagementService;
    @Mock
    private CaseDataStoreService caseDataStoreService;
    @Mock
    private SecurityUtils securityUtils;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        testee = new CaseDocumentAmController(documentManagementService, new ValidationUtils(), securityUtils);
        when(securityUtils.getServiceNameFromS2SToken(TEST_S2S_TOKEN)).thenReturn(XUI_WEBAPP);
    }

    @Test
    public void shouldGetValidMetaDataResponse() {
        Optional<DocumentPermissions> documentPermissions = Optional.ofNullable(getDocumentPermissions(
            MATCHED_DOCUMENT_ID,
            Arrays.asList(
                Permission.CREATE,
                Permission.READ
            )
        ));
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        doReturn(TRUE).when(documentManagementService)
            .checkUserPermission(setDocumentMetaData(), getUuid(), Permission.READ);
        doReturn(TRUE).when(documentManagementService)
            .checkServicePermission(setDocumentMetaData(), XUI_WEBAPP, Permission.READ);

        ResponseEntity response = testee
            .getDocumentbyDocumentId(getUuid(), TEST_S2S_TOKEN);

        assertAll(
            () -> assertNotNull(response, "Valid Response from API"),
            () -> assertNotNull(response.getBody(), "Valid response body"),
            () -> assertEquals(HttpStatus.OK, response.getStatusCode(), "Status code is OK")
        );
    }

    @Test
    public void shouldNotGetValidMetaDataResponseWhenServiceNotAuthorised() {
        Optional<DocumentPermissions> documentPermissions = Optional.ofNullable(getDocumentPermissions(
            UNMATCHED_DOCUMENT_ID,
            Arrays.asList(
                Permission.CREATE,
                Permission.READ
            )
        ));
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        doReturn(FALSE).when(documentManagementService)
            .checkUserPermission(setDocumentMetaData(), getUuid(), Permission.READ);
        doReturn(FALSE).when(documentManagementService)
            .checkServicePermission(setDocumentMetaData(), XUI_WEBAPP, Permission.READ);

        Assertions.assertThrows(ForbiddenException.class, () -> {
            testee.getDocumentbyDocumentId(getUuid(), TEST_S2S_TOKEN);
        });
    }

    @Test
    public void shouldNotGetValidMetaDataResponseWhenUserNotAuthorised() {
        Optional<DocumentPermissions> documentPermissions = Optional.ofNullable(getDocumentPermissions(
            UNMATCHED_DOCUMENT_ID,
            Arrays.asList(
                Permission.CREATE,
                Permission.READ
            )
        ));
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        doReturn(FALSE).when(documentManagementService)
            .checkUserPermission(setDocumentMetaData(), getUuid(), Permission.READ);
        doReturn(TRUE).when(documentManagementService)
            .checkServicePermission(setDocumentMetaData(), XUI_WEBAPP, Permission.READ);

        Assertions.assertThrows(ForbiddenException.class, () -> {
            testee.getDocumentbyDocumentId(getUuid(), TEST_S2S_TOKEN);
        });
    }

    @Test
    @DisplayName("should get 200 document binary content")
    public void shouldGetDocumentBinaryContent() {
        Optional<DocumentPermissions> documentPermissions = Optional.ofNullable(getDocumentPermissions(
            MATCHED_DOCUMENT_ID,
            Arrays.asList(
                Permission.CREATE,
                Permission.READ
            )
        ));
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        doReturn(TRUE).when(documentManagementService)
            .checkUserPermission(setDocumentMetaData(), getUuid(), Permission.READ);
        doReturn(TRUE).when(documentManagementService)
            .checkServicePermission(setDocumentMetaData(), XUI_WEBAPP, Permission.READ);
        doReturn(setDocumentBinaryContent("OK")).when(documentManagementService).getDocumentBinaryContent(getUuid());

        ResponseEntity<Object> response = testee.getDocumentBinaryContentbyDocumentId(getUuid(), TEST_S2S_TOKEN);

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
        Optional<DocumentPermissions> documentPermissions = Optional.ofNullable(getDocumentPermissions(
            MATCHED_DOCUMENT_ID,
            Arrays.asList(Permission.CREATE)
        ));
        doReturn(TRUE).when(documentManagementService)
            .checkServicePermission(setDocumentMetaData(), XUI_WEBAPP, Permission.READ);
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        doReturn(setDocumentBinaryContent(FORBIDDEN)).when(documentManagementService)
            .getDocumentBinaryContent(getUuid());

        Assertions.assertThrows(ForbiddenException.class, () ->
            testee.getDocumentBinaryContentbyDocumentId(getUuid(), TEST_S2S_TOKEN));
    }


    @Test
    @DisplayName("should throw 403 forbidden when the requested document does not match with available doc")
    public void shouldThrowForbiddenWhenDocumentDoesNotMatch() {
        Optional<DocumentPermissions> documentPermissions = Optional.ofNullable(getDocumentPermissions(
            UNMATCHED_DOCUMENT_ID,
            Arrays.asList(
                Permission.CREATE,
                Permission.READ
            )
        ));
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        doReturn(CASE_ID).when(documentManagementService).extractCaseIdFromMetadata(setDocumentMetaData().getBody());
        doReturn(documentPermissions).when(caseDataStoreService).getCaseDocumentMetadata(CASE_ID, getUuid());
        doReturn(setDocumentBinaryContent(FORBIDDEN)).when(documentManagementService)
            .getDocumentBinaryContent(getUuid());
        doReturn(TRUE).when(documentManagementService)
            .checkServicePermission(setDocumentMetaData(), XUI_WEBAPP, Permission.READ);

        Assertions.assertThrows(ForbiddenException.class, () -> testee.getDocumentBinaryContentbyDocumentId(
            getUuid(), TEST_S2S_TOKEN
        ));
    }

    @Test
    @DisplayName("should throw 403 forbidden when the service is not authorised to access")
    public void shouldThrowForbiddenWhenServiceIsNotAuthorised() {
        Optional<DocumentPermissions> documentPermissions = Optional.ofNullable(getDocumentPermissions(
            UNMATCHED_DOCUMENT_ID,
            Arrays.asList(
                Permission.CREATE,
                Permission.READ
            )
        ));
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        doReturn(CASE_ID).when(documentManagementService).extractCaseIdFromMetadata(setDocumentMetaData().getBody());
        doReturn(documentPermissions).when(caseDataStoreService).getCaseDocumentMetadata(CASE_ID, getUuid());
        doReturn(setDocumentBinaryContent(FORBIDDEN)).when(documentManagementService)
            .getDocumentBinaryContent(getUuid());
        doReturn(FALSE).when(documentManagementService)
            .checkServicePermission(setDocumentMetaData(), XUI_WEBAPP, Permission.READ);

        Assertions.assertThrows(ForbiddenException.class, () -> testee.getDocumentBinaryContentbyDocumentId(
            getUuid(), TEST_S2S_TOKEN
        ));

    }

    @Test
    @DisplayName("should get 204 when document delete is successful")
    public void shouldDeleteDocumentByDocumentId() {
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        doReturn(TRUE).when(documentManagementService)
            .checkServicePermission(setDocumentMetaData(), XUI_WEBAPP, Permission.UPDATE);
        doReturn(TRUE).when(documentManagementService)
            .checkUserPermission(setDocumentMetaData(), getUuid(), Permission.UPDATE);
        doReturn(ResponseEntity.status(HttpStatus.NO_CONTENT).build()).when(documentManagementService)
            .deleteDocument(getUuid(), true);

        ResponseEntity response = testee
            .deleteDocumentbyDocumentId(getUuid(), true, TEST_S2S_TOKEN);

        assertAll(
            () -> assertNotNull(response, VALID_RESPONSE),
            () -> assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode(), NO_CONTENT_RESPONSE_CODE)
        );
    }

    @Test
    @DisplayName("should get 403 when service is not authorised")
    public void shouldNotAllowDeleteDocumentByDocumentIdWhenServiceIsNotAuthorised() {
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        doReturn(FALSE).when(documentManagementService)
            .checkServicePermission(setDocumentMetaData(), XUI_WEBAPP, Permission.UPDATE);
        doReturn(TRUE).when(documentManagementService)
            .checkUserPermission(setDocumentMetaData(), getUuid(), Permission.UPDATE);
        doReturn(ResponseEntity.status(HttpStatus.NO_CONTENT).build()).when(documentManagementService)
            .deleteDocument(getUuid(), true);

        Assertions.assertThrows(ForbiddenException.class, () -> {
            testee
                .deleteDocumentbyDocumentId(getUuid(), true, TEST_S2S_TOKEN);
        });
    }

    @Test
    public void shouldPatchDocumentByDocumentId() {
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        doReturn(TRUE).when(documentManagementService)
            .checkServicePermission(setDocumentMetaData(), XUI_WEBAPP, Permission.UPDATE);
        doReturn(TRUE).when(documentManagementService)
            .checkUserPermission(setDocumentMetaData(), getUuid(), Permission.UPDATE);
        UpdateDocumentCommand body = null;
        doReturn(setDocumentMetaData()).when(documentManagementService).patchDocument(getUuid(), body);

        ResponseEntity response = testee.patchDocumentbyDocumentId(
            body,
            getUuid(),
            TEST_S2S_TOKEN
        );
        assertAll(
            () -> assertNotNull(response, VALID_RESPONSE),
            () -> assertEquals(HttpStatus.OK, response.getStatusCode(), RESPONSE_CODE)
        );
    }

    @Test
    public void shouldNotAllowPatchDocumentByDocumentIdWhenServiceIsNotAuthorised() {
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        doReturn(FALSE).when(documentManagementService)
            .checkServicePermission(setDocumentMetaData(), XUI_WEBAPP, Permission.UPDATE);
        doReturn(TRUE).when(documentManagementService)
            .checkUserPermission(setDocumentMetaData(), getUuid(), Permission.READ);
        UpdateDocumentCommand body = null;
        doReturn(setDocumentMetaData()).when(documentManagementService).patchDocument(getUuid(), body);

        Assertions.assertThrows(ForbiddenException.class, () -> {
            testee.patchDocumentbyDocumentId(
                body,
                getUuid(),
                TEST_S2S_TOKEN
            );
        });
    }

    @Test
    public void shouldNotPatchMetaDataOnDocuments() {
        doReturn(FALSE).when(documentManagementService).checkServicePermission(
            setDocumentMetaData(),
            XUI_WEBAPP,
            Permission.ATTACH
        );
        DocumentHashToken document = DocumentHashToken.builder().id("cab18c21-8b7c-452b-937c-091225e0cc12").build();
        CaseDocumentsMetadata body = CaseDocumentsMetadata.builder()
            .caseId("1111122222333334")
            .documentHashTokens(Arrays.asList(document))
            .caseTypeId("BEFTA_CASETYPE_2_1")
            .jurisdictionId("BEFTA_JURISDICTION_2")
            .build();
        doReturn(setDocumentMetaData()).when(documentManagementService)
            .getDocumentMetadata(UUID.fromString(body.getDocumentHashTokens().get(
            0).getId()));

        Assertions.assertThrows(ForbiddenException.class, () -> {
            testee.patchMetaDataOnDocuments(body, TEST_S2S_TOKEN);
        });
    }


    @Test
    public void shouldNotPatchMetaDataOnDocumentsWhenCaseIdNotValid() {
        doReturn(FALSE).when(documentManagementService).checkServicePermission(
            setDocumentMetaData(),
            XUI_WEBAPP,
            Permission.ATTACH
        );
        DocumentHashToken document = DocumentHashToken.builder().id("cab18c21-8b7c-452b-937c-091225e0cc12").build();
        CaseDocumentsMetadata body = CaseDocumentsMetadata.builder()
            .caseId("111112222233333")
            .documentHashTokens(Arrays.asList(document))
            .caseTypeId("BEFTA_CASETYPE_2_1")
            .jurisdictionId("BEFTA_JURISDICTION_2")
            .build();
        doReturn(setDocumentMetaData()).when(documentManagementService)
            .getDocumentMetadata(UUID.fromString(body.getDocumentHashTokens().get(
            0).getId()));

        Assertions.assertThrows(BadRequestException.class, () -> {
            testee.patchMetaDataOnDocuments(body, TEST_S2S_TOKEN);
        });
    }

    @Test
    public void shouldPatchMetaDataOnDocuments() {
        doReturn(TRUE).when(documentManagementService).checkServicePermission(
            setDocumentMetaData(),
            XUI_WEBAPP,
            Permission.ATTACH
        );
        DocumentHashToken document = DocumentHashToken.builder().id("cab18c21-8b7c-452b-937c-091225e0cc12").build();
        CaseDocumentsMetadata body = CaseDocumentsMetadata.builder()
            .caseId("1111122222333334")
            .documentHashTokens(Arrays.asList(document))
            .caseTypeId("BEFTA_CASETYPE_2_1")
            .jurisdictionId("BEFTA_JURISDICTION_2")
            .build();
        doReturn(setDocumentMetaData()).when(documentManagementService)
            .getDocumentMetadata(UUID.fromString(body.getDocumentHashTokens().get(
            0).getId()));
        ResponseEntity response = testee.patchMetaDataOnDocuments(body, TEST_S2S_TOKEN);

        assertAll(
            () -> assertNotNull(response, VALID_RESPONSE),
            () -> assertEquals(HttpStatus.OK, response.getStatusCode(), RESPONSE_CODE)
        );
    }

    @Test
    @DisplayName("Should go through happy path")
    public void uploadDocuments_HappyPath() {
        doReturn(TRUE).when(documentManagementService).checkServicePermissionsForUpload(
            "BEFTA_CASETYPE_2",
            "BEFTA_JURISDICTION_2",
            XUI_WEBAPP,
            Permission.CREATE
        );
        List<MultipartFile> multipartFiles = generateMultipartList();
        Mockito.when(documentManagementService.uploadDocuments(
            multipartFiles,
            Classification.PUBLIC.name(),
            BEFTA_CASETYPE_2,
            BEFTA_JURISDICTION_2
        ))
            .thenReturn(new ResponseEntity<>(generateEmbeddedLinkedHashMap(), HttpStatus.OK));

        ResponseEntity<Object> responseEntity = testee.uploadDocuments(multipartFiles, Classification.PUBLIC.name(),
                                                                       BEFTA_CASETYPE_2, BEFTA_JURISDICTION_2,
                                                                       TEST_S2S_TOKEN
        );
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @SuppressWarnings("unchecked")
    public LinkedHashMap<String, Object> generateEmbeddedLinkedHashMap() {
        HashMap<String, String> binaryHash = new HashMap<>();
        HashMap<String, String> selfHash = new HashMap<>();
        selfHash.put(Constants.HREF, "http://localhost:4455/cases/documents/35471d43-0dad-42c1-b05a-4821028f50a2");
        binaryHash.put(
            Constants.HREF,
            "http://localhost:4455/cases/documents/35471d43-0dad-42c1-b05a-4821028f50a2/binary"
        );

        LinkedHashMap<String, Object> linksLinkedHashMap = new LinkedHashMap<>();
        LinkedHashMap<String, Object> binarySelfLinkedHashMap = new LinkedHashMap<>();

        binarySelfLinkedHashMap.put(Constants.BINARY, binaryHash);
        binarySelfLinkedHashMap.put(Constants.SELF, selfHash);
        linksLinkedHashMap.put(Constants.LINKS, binarySelfLinkedHashMap);

        ArrayList arrayList = new ArrayList();
        arrayList.add(linksLinkedHashMap);

        LinkedHashMap<String, Object> documentsLinkedHashMap = new LinkedHashMap<>();
        documentsLinkedHashMap.put(Constants.DOCUMENTS, arrayList);

        LinkedHashMap<String, Object> embeddedLinkedHashMap = new LinkedHashMap<>();
        embeddedLinkedHashMap.put(Constants.EMBEDDED, documentsLinkedHashMap);

        return embeddedLinkedHashMap;
    }

    @Test
    @DisplayName("Should throw 400 when the uploaded file is empty")
    public void shouldThrowBadRequestExceptionWhenUploadedFilesIsNull() {
        doReturn(TRUE).when(documentManagementService).checkServicePermissionsForUpload(
            "BEFTA_CASETYPE_2",
            "BEFTA_JURISDICTION_2",
            XUI_WEBAPP,
            Permission.CREATE
        );
        Assertions.assertThrows(BadRequestException.class, () -> {
            testee.uploadDocuments(null, Classification.PUBLIC.name(),
                                   BEFTA_CASETYPE_2, BEFTA_JURISDICTION_2, TEST_S2S_TOKEN
            );
        });
    }

    @Test
    @DisplayName("Should throw 400 when user-roles are empty")
    public void shouldThrowBadRequestExceptionWhenUserRolesAreEmpty() {
        doReturn(TRUE).when(documentManagementService).checkServicePermissionsForUpload(
            "BEFTA_CASETYPE_2",
            "BEFTA@JURISDICTION_2$$$$",
            XUI_WEBAPP,
            Permission.CREATE
        );
        Assertions.assertThrows(BadRequestException.class, () -> {
            testee.uploadDocuments(generateMultipartList(),
                                   Classification.PUBLIC.name(),
                                   BEFTA_CASETYPE_2, "BEFTA@JURISDICTION_2$$$$",
                                   TEST_S2S_TOKEN
            );
        });
    }

    @Test
    @DisplayName("Should throw 400 when caseTypeId input is null")
    public void shouldThrowBadRequestExceptionWhenCaseTypeIdIsNull() {
        doReturn(TRUE).when(documentManagementService).checkServicePermissionsForUpload(
            null,
            "BEFTA_JURISDICTION_2",
            XUI_WEBAPP,
            Permission.CREATE
        );
        Assertions.assertThrows(BadRequestException.class, () -> {
            testee.uploadDocuments(generateMultipartList(),
                                   Classification.PUBLIC.name(),
                                   null, BEFTA_JURISDICTION_2,
                                   TEST_S2S_TOKEN
            );
        });
    }

    @Test
    @DisplayName("Should throw 400 when caseTypeId input is malformed")
    public void shouldThrowBadRequestExceptionWhenCaseTypeIdIsMalformed() {
        doReturn(TRUE).when(documentManagementService).checkServicePermissionsForUpload(
            "BEFTA_CASETYPE_2&&&&&&&&&",
            "BEFTA_JURISDICTION_2",
            XUI_WEBAPP,
            Permission.CREATE
        );
        Assertions.assertThrows(BadRequestException.class, () -> {
            testee.uploadDocuments(generateMultipartList(),
                                   Classification.PUBLIC.name(),
                                   "BEFTA_CASETYPE_2&&&&&&&&&",
                                   "BEFTA_JURISDICTION_2",
                                   TEST_S2S_TOKEN
            );
        });
    }

    @Test
    @DisplayName("Should throw 400 when jurisdictionId input is null")
    public void shouldThrowBadRequestExceptionWhenJurisdictionIdIsNull() {
        doReturn(TRUE).when(documentManagementService).checkServicePermissionsForUpload(
            "BEFTA_CASETYPE_2",
            null,
            XUI_WEBAPP,
            Permission.CREATE
        );
        Assertions.assertThrows(BadRequestException.class, () -> {
            testee.uploadDocuments(generateMultipartList(),
                                   Classification.PUBLIC.name(),
                                   BEFTA_CASETYPE_2,
                                   null,
                                   TEST_S2S_TOKEN
            );
        });
    }

    @Test
    @DisplayName("Should throw 400 when jurisdictionId input is malformed")
    public void shouldThrowBadRequestExceptionWhenJurisdictionIdIsMalformed() {
        doReturn(TRUE).when(documentManagementService).checkServicePermissionsForUpload(
            "BEFTA_CASETYPE_2",
            "BEFTA@JURISDICTION_2$$$$",
            XUI_WEBAPP,
            Permission.CREATE
        );
        Assertions.assertThrows(BadRequestException.class, () -> {
            testee.uploadDocuments(generateMultipartList(),
                                   Classification.PUBLIC.name(),
                                   BEFTA_CASETYPE_2,
                                   "BEFTA@JURISDICTION_2$$$$",
                                   TEST_S2S_TOKEN
            );
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

    private DocumentPermissions getDocumentPermissions(String docId, List<Permission> permission) {
        DocumentPermissions documentPermissions = DocumentPermissions.builder()
            .permissions(permission).id(docId).build();
        return documentPermissions;
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
                                            "image/jpeg", "HelloString".getBytes()
        ));
        listFiles.add(new MockMultipartFile("file2", "test2.jpg",
                                            "image/jpeg", "HelloString2".getBytes()
        ));
        return listFiles;
    }

    @Test
    void generateHashCode_HappyPath() {

        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        when(documentManagementService.checkServicePermission(setDocumentMetaData(), XUI_WEBAPP, Permission.HASHTOKEN))
            .thenReturn(TRUE);
        when(documentManagementService.generateHashToken(UUID.fromString(MATCHED_DOCUMENT_ID)))
            .thenReturn("hashToken");

        ResponseEntity<Object> responseEntity = testee.generateHashCode(UUID.fromString(MATCHED_DOCUMENT_ID),
                                                                        TEST_S2S_TOKEN);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("{hashToken=hashToken}", responseEntity.getBody().toString());
    }

    @Test
    //this test returns an illegal argument exception because UUID.fromString() contains a throw for illegal arguments
    void generateHashCode_BadRequest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            testee.generateHashCode(UUID.fromString("A.A"), TEST_S2S_TOKEN);
        });
    }

    @Test
    void generateHashCode_BadRequestWhenServiceIsNotAuthorised() {

        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());
        when(documentManagementService.checkServicePermission(setDocumentMetaData(), XUI_WEBAPP, Permission.HASHTOKEN))
            .thenReturn(FALSE);
        when(documentManagementService.generateHashToken(UUID.fromString(MATCHED_DOCUMENT_ID)))
            .thenReturn("hashToken");

        Assertions.assertThrows(ForbiddenException.class, () -> {
            testee.generateHashCode(UUID.fromString(MATCHED_DOCUMENT_ID), TEST_S2S_TOKEN);
        });

    }
}
