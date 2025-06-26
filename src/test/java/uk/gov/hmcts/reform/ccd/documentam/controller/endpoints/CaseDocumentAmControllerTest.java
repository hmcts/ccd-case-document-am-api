package uk.gov.hmcts.reform.ccd.documentam.controller.endpoints;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.ccd.documentam.ApplicationParams;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants;
import uk.gov.hmcts.reform.ccd.documentam.client.datastore.CaseDataStoreClient;
import uk.gov.hmcts.reform.ccd.documentam.dto.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.documentam.dto.UpdateTtlRequest;
import uk.gov.hmcts.reform.ccd.documentam.dto.UploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedService;
import uk.gov.hmcts.reform.ccd.documentam.model.AuthorisedServices;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentHashToken;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentPermissions;
import uk.gov.hmcts.reform.ccd.documentam.model.GeneratedHashCodeResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentMetaDataResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.PatchDocumentResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Classification;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;
import uk.gov.hmcts.reform.ccd.documentam.service.DocumentManagementService;
import uk.gov.hmcts.reform.ccd.documentam.service.impl.DocumentManagementServiceImpl;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.SERVICE_PERMISSION_ERROR;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.USER_PERMISSION_ERROR;

class CaseDocumentAmControllerTest implements TestFixture {
    private static final String VALID_RESPONSE = "Valid Response from API";
    private static final String RESPONSE_CODE = "Status code is OK";
    private static final String NO_CONTENT_RESPONSE_CODE = "Status code is No Content";
    private static final String FORBIDDEN = "forbidden";
    private static final String TEST_S2S_TOKEN = "Test s2sToken";

    private static final Document DOCUMENT = Document.builder()
        .originalDocumentName("test.png")
        .build();

    private static final Document DOCUMENT_WITH_FUTURE_TTL = Document.builder()
            .originalDocumentName("test.png")
            .ttl(Date.from(LocalDateTime.now().plusHours(6).toInstant(ZoneOffset.UTC)))
            .build();

    private static final Document DOCUMENT_WITH_PAST_TTL = Document.builder()
            .originalDocumentName("test.png")
            .ttl(Date.from(LocalDateTime.now().minusHours(6).toInstant(ZoneOffset.UTC)))
            .build();

    private static final Document DOCUMENT_WITH_CASE_ID = Document.builder()
            .originalDocumentName("test.png")
            .metadata(Map.of(Constants.METADATA_CASE_ID, CASE_ID_VALUE))
            .build();

    private static final Document DOCUMENT_WITH_CASE_TYPE_ID = Document.builder()
        .originalDocumentName("test.png")
        .metadata(Map.of(Constants.METADATA_CASE_TYPE_ID, CASE_TYPE_ID_VALUE
        ))
        .build();

    private static final Document DOCUMENT_WITH_JURISDICTION_ID = Document.builder()
        .originalDocumentName("test.png")
        .metadata(Map.of(Constants.METADATA_JURISDICTION_ID, JURISDICTION_ID_VALUE))
        .build();

    private CaseDocumentAmController testee;

    @Mock
    private DocumentManagementService documentManagementService;
    @Mock
    private CaseDataStoreClient caseDataStoreService;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private BindingResult bindingResult;
    @Mock
    AuthorisedServices serviceConfig;
    @Mock
    private ApplicationParams applicationParams;
    @Mock
    private HttpServletResponse httpResponse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testee = new CaseDocumentAmController(documentManagementService, securityUtils, applicationParams);
        when(securityUtils.getServiceNameFromS2SToken(TEST_S2S_TOKEN)).thenReturn(SERVICE_NAME_XUI_WEBAPP);
        doReturn(DOCUMENT_WITH_FUTURE_TTL).when(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID);
    }

    @Test
    void shouldGetValidMetaDataResponse() {
        doNothing().when(documentManagementService)
            .checkUserPermission(
                DOCUMENT_WITH_FUTURE_TTL.getCaseId(),
                MATCHED_DOCUMENT_ID,
                Permission.READ,
                USER_PERMISSION_ERROR,
                MATCHED_DOCUMENT_ID.toString());
        doReturn(AuthorisedService.builder().build()).when(documentManagementService)
            .checkServicePermission(
                DOCUMENT_WITH_FUTURE_TTL.getCaseTypeId(),
                DOCUMENT_WITH_FUTURE_TTL.getJurisdictionId(),
                SERVICE_NAME_XUI_WEBAPP,
                Permission.READ,
                SERVICE_PERMISSION_ERROR,
                MATCHED_DOCUMENT_ID.toString());

        final ResponseEntity<Document> response = testee.getDocumentByDocumentId(MATCHED_DOCUMENT_ID, TEST_S2S_TOKEN);

        assertAll(
            () -> assertNotNull(response, "Valid Response from API"),
            () -> assertNotNull(response.getBody(), "Valid response body"),
            () -> assertEquals(HttpStatus.OK, response.getStatusCode(), "Status code is OK")
        );
    }

    @Test
    void shouldGetValidMetaDataResponseWithValidCaseId() {

        doReturn(DOCUMENT_WITH_CASE_ID).when(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID);
        doNothing().when(documentManagementService)
                .checkUserPermission(
                        DOCUMENT_WITH_CASE_ID.getCaseId(),
                        MATCHED_DOCUMENT_ID,
                        Permission.READ,
                        USER_PERMISSION_ERROR,
                        MATCHED_DOCUMENT_ID.toString());
        doReturn(AuthorisedService.builder().build()).when(documentManagementService)
                .checkServicePermission(
                        DOCUMENT_WITH_CASE_ID.getCaseTypeId(),
                        DOCUMENT_WITH_CASE_ID.getJurisdictionId(),
                        SERVICE_NAME_XUI_WEBAPP,
                        Permission.READ,
                        SERVICE_PERMISSION_ERROR,
                        MATCHED_DOCUMENT_ID.toString());

        final ResponseEntity<Document> response = testee.getDocumentByDocumentId(MATCHED_DOCUMENT_ID, TEST_S2S_TOKEN);

        assertAll(
            () -> assertNotNull(response, "Valid Response from API"),
            () -> assertNotNull(response.getBody(), "Valid response body"),
            () -> assertEquals(HttpStatus.OK, response.getStatusCode(), "Status code is OK")
        );
    }

    @Test
    void shouldGetValidMetaDataResponseWithoutCallingDatastoreWhenDocumentMetadataHasTTLInFutureButNoCaseId() {
        doReturn(AuthorisedService.builder().build()).when(documentManagementService)
                .checkServicePermission(
                        DOCUMENT_WITH_FUTURE_TTL.getCaseTypeId(),
                        DOCUMENT_WITH_FUTURE_TTL.getJurisdictionId(),
                        SERVICE_NAME_XUI_WEBAPP,
                        Permission.READ,
                        SERVICE_PERMISSION_ERROR,
                        MATCHED_DOCUMENT_ID.toString());


        final ResponseEntity<Document> response = testee.getDocumentByDocumentId(MATCHED_DOCUMENT_ID, TEST_S2S_TOKEN);

        assertAll(
            () -> assertNotNull(response, "Valid Response from API"),
            () -> assertNotNull(response.getBody(), "Valid response body"),
            () -> assertEquals(HttpStatus.OK, response.getStatusCode(), "Status code is OK"),
            () -> verify(documentManagementService, never())
                    .checkUserPermission(
                        DOCUMENT_WITH_FUTURE_TTL.getCaseId(),
                        MATCHED_DOCUMENT_ID,
                        Permission.READ,
                        USER_PERMISSION_ERROR,
                        MATCHED_DOCUMENT_ID.toString())

        );
    }

    @Test
    void shouldThrowForbiddenExceptionWhenGetDocumentByDocumentIdDocumentMetadataHasTTLInPastButNoCaseId() {
        doReturn(AuthorisedService.builder().build()).when(documentManagementService)
                .checkServicePermission(
                        DOCUMENT_WITH_FUTURE_TTL.getCaseTypeId(),
                        DOCUMENT_WITH_FUTURE_TTL.getJurisdictionId(),
                        SERVICE_NAME_XUI_WEBAPP,
                        Permission.READ,
                        SERVICE_PERMISSION_ERROR,
                        MATCHED_DOCUMENT_ID.toString());


        doReturn(DOCUMENT_WITH_PAST_TTL).when(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID);

        ForbiddenException thrown = assertThrows(
            ForbiddenException.class,
            () -> testee.getDocumentByDocumentId(MATCHED_DOCUMENT_ID, TEST_S2S_TOKEN),
            "Failed to throw ForbiddenException"
        );

        assertAll(
            () -> assertTrue(thrown.getMessage()
                    .contains("Document " + MATCHED_DOCUMENT_ID + " can not be downloaded as TTL has expired")),
            () -> verify(documentManagementService, never()).checkUserPermission(
                    DOCUMENT_WITH_FUTURE_TTL.getCaseId(),
                    MATCHED_DOCUMENT_ID,
                    Permission.READ,
                    USER_PERMISSION_ERROR,
                    MATCHED_DOCUMENT_ID.toString())
        );
    }

    @Test
    void shouldThrowForbiddenExceptionWhenGetDocumentByDocumentIdDocumentMetadataHasNullTTL() {
        doReturn(AuthorisedService.builder().build()).when(documentManagementService)
                .checkServicePermission(
                        DOCUMENT_WITH_FUTURE_TTL.getCaseTypeId(),
                        DOCUMENT_WITH_FUTURE_TTL.getJurisdictionId(),
                        SERVICE_NAME_XUI_WEBAPP,
                        Permission.READ,
                        SERVICE_PERMISSION_ERROR,
                        MATCHED_DOCUMENT_ID.toString());


        doReturn(DOCUMENT).when(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID);

        ForbiddenException thrown = assertThrows(
            ForbiddenException.class,
            () -> testee.getDocumentByDocumentId(MATCHED_DOCUMENT_ID, TEST_S2S_TOKEN),
            "Failed to throw ForbiddenException"
        );

        assertAll(
            () -> assertTrue(thrown.getMessage()
                        .contains("Document " + MATCHED_DOCUMENT_ID + " can not be downloaded as TTL has expired")),
            () -> verify(documentManagementService, never()).checkUserPermission(
                DOCUMENT_WITH_FUTURE_TTL.getCaseId(),
                MATCHED_DOCUMENT_ID,
                Permission.READ,
                USER_PERMISSION_ERROR,
                MATCHED_DOCUMENT_ID.toString())
        );
    }

    @Test
    void shouldNotGetValidMetaDataResponseWhenServiceNotAuthorised() {
        doReturn(DOCUMENT).when(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID);
        doNothing().when(documentManagementService)
            .checkUserPermission(DOCUMENT.getCaseId(),
                                 MATCHED_DOCUMENT_ID,
                                 Permission.READ,
                                 USER_PERMISSION_ERROR,
                                 MATCHED_DOCUMENT_ID.toString());
        doThrow(ForbiddenException.class).when(documentManagementService)
            .checkServicePermission(DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    SERVICE_NAME_XUI_WEBAPP,
                                    Permission.READ,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString());

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> testee.getDocumentByDocumentId(MATCHED_DOCUMENT_ID, TEST_S2S_TOKEN));
    }

    @Test
    void shouldNotGetValidMetaDataResponseWhenUserNotAuthorised() {

        Map<String, String> metaData = new HashMap<>();
        metaData.put(Constants.METADATA_CASE_ID, CASE_ID_VALUE);

        final Document documentWithCaseId = Document.builder()
                .originalDocumentName("test.png")
                .metadata(metaData)
                .build();

        doReturn(documentWithCaseId).when(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID);

        doThrow(ForbiddenException.class).when(documentManagementService)
            .checkUserPermission(documentWithCaseId.getCaseId(),
                                 MATCHED_DOCUMENT_ID,
                                 Permission.READ,
                                 USER_PERMISSION_ERROR,
                                 MATCHED_DOCUMENT_ID.toString());
        doReturn(AuthorisedService.builder().build()).when(documentManagementService)
            .checkServicePermission(documentWithCaseId.getCaseTypeId(),
                                    documentWithCaseId.getJurisdictionId(),
                                    SERVICE_NAME_XUI_WEBAPP,
                                    Permission.READ,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString());

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> testee.getDocumentByDocumentId(MATCHED_DOCUMENT_ID, TEST_S2S_TOKEN));
    }

    @Test
    @DisplayName("should get 200 document binary content")
    void shouldGetDocumentBinaryContent() {
        doReturn(false).when(applicationParams).isStreamDownloadEnabled();
        doNothing().when(documentManagementService)
            .checkUserPermission(DOCUMENT.getCaseId(),
                                 MATCHED_DOCUMENT_ID,
                                 Permission.READ,
                                 USER_PERMISSION_ERROR,
                                 MATCHED_DOCUMENT_ID.toString());
        doReturn(AuthorisedService.builder().build()).when(documentManagementService)
            .checkServicePermission(DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    SERVICE_NAME_XUI_WEBAPP,
                                    Permission.READ,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString());
        doReturn(setDocumentBinaryContent("OK"))
            .when(documentManagementService).getDocumentBinaryContent(MATCHED_DOCUMENT_ID);

        ResponseEntity<ByteArrayResource> response =
            testee.getDocumentBinaryContentByDocumentId(MATCHED_DOCUMENT_ID, null,
                                                        TEST_S2S_TOKEN, Map.of());

        assertAll(
            () -> verify(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID),
            () -> verify(documentManagementService, times(1)).getDocumentBinaryContent(MATCHED_DOCUMENT_ID),
            () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
            () -> assertNotNull(response.getBody())
        );
    }

    @Test
    @DisplayName("should stream 200 document binary content")
    void shouldStreamDocumentBinaryContent() {
        doReturn(true).when(applicationParams).isStreamDownloadEnabled();
        doNothing().when(documentManagementService)
            .checkUserPermission(DOCUMENT.getCaseId(),
                                 MATCHED_DOCUMENT_ID,
                                 Permission.READ,
                                 USER_PERMISSION_ERROR,
                                 MATCHED_DOCUMENT_ID.toString());
        doReturn(AuthorisedService.builder().build()).when(documentManagementService)
            .checkServicePermission(DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    SERVICE_NAME_XUI_WEBAPP,
                                    Permission.READ,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString());

        Map<String, String> headers = new HashMap<>();
        doNothing().when(documentManagementService)
            .streamDocumentBinaryContent(MATCHED_DOCUMENT_ID, httpResponse, headers);

        ResponseEntity<ByteArrayResource> response =
            testee.getDocumentBinaryContentByDocumentId(MATCHED_DOCUMENT_ID, httpResponse,
                                                        TEST_S2S_TOKEN, headers);

        assertAll(
            () -> verify(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID),
            () -> verify(documentManagementService, times(1))
                .streamDocumentBinaryContent(MATCHED_DOCUMENT_ID, httpResponse, headers),
            () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
            () -> assertNull(response.getBody())
        );
    }

    @Test
    void shouldGetDocumentBinaryContentWithValidCaseId() {
        doReturn(false).when(applicationParams).isStreamDownloadEnabled();
        doReturn(DOCUMENT_WITH_CASE_ID).when(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID);
        doNothing().when(documentManagementService)
                .checkUserPermission(
                        DOCUMENT_WITH_CASE_ID.getCaseId(),
                        MATCHED_DOCUMENT_ID,
                        Permission.READ,
                        USER_PERMISSION_ERROR,
                        MATCHED_DOCUMENT_ID.toString());
        doReturn(AuthorisedService.builder().build()).when(documentManagementService)
                .checkServicePermission(
                        DOCUMENT_WITH_CASE_ID.getCaseTypeId(),
                        DOCUMENT_WITH_CASE_ID.getJurisdictionId(),
                        SERVICE_NAME_XUI_WEBAPP,
                        Permission.READ,
                        SERVICE_PERMISSION_ERROR,
                        MATCHED_DOCUMENT_ID.toString());
        doReturn(setDocumentBinaryContent("OK"))
                .when(documentManagementService).getDocumentBinaryContent(MATCHED_DOCUMENT_ID);

        final ResponseEntity<ByteArrayResource> response =
                testee.getDocumentBinaryContentByDocumentId(MATCHED_DOCUMENT_ID, null,
                                                            TEST_S2S_TOKEN, Map.of());

        assertAll(
            () -> verify(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID),
            () -> verify(documentManagementService, times(1)).getDocumentBinaryContent(MATCHED_DOCUMENT_ID),
            () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
            () -> assertNotNull(response.getBody())
        );
    }

    @Test
    void shouldGetValidDocumentBinaryContentWithoutCallingDatastoreWhenDocumentMetadataHasTTLInFutureButNoCaseId() {
        doReturn(false).when(applicationParams).isStreamDownloadEnabled();
        doReturn(AuthorisedService.builder().build()).when(documentManagementService)
                .checkServicePermission(
                        DOCUMENT_WITH_FUTURE_TTL.getCaseTypeId(),
                        DOCUMENT_WITH_FUTURE_TTL.getJurisdictionId(),
                        SERVICE_NAME_XUI_WEBAPP,
                        Permission.READ,
                        SERVICE_PERMISSION_ERROR,
                        MATCHED_DOCUMENT_ID.toString());

        doReturn(setDocumentBinaryContent("OK"))
                .when(documentManagementService).getDocumentBinaryContent(MATCHED_DOCUMENT_ID);
        final ResponseEntity<ByteArrayResource> response =
                testee.getDocumentBinaryContentByDocumentId(MATCHED_DOCUMENT_ID, null,
                                                            TEST_S2S_TOKEN, Map.of());

        assertAll(
            () -> verify(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID),
            () -> verify(documentManagementService, times(1)).getDocumentBinaryContent(MATCHED_DOCUMENT_ID),
            () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
            () -> assertNotNull(response.getBody()),
            () -> verify(documentManagementService, never()).checkUserPermission(
                    DOCUMENT_WITH_FUTURE_TTL.getCaseId(),
                    MATCHED_DOCUMENT_ID,
                    Permission.READ,
                    USER_PERMISSION_ERROR,
                    MATCHED_DOCUMENT_ID.toString())
        );
    }

    @Test
    void shouldThrowForbiddenExceptionWhenRetrievingDocumentBinaryContentWhenDocumentMetadataHasTTLInPastButNoCaseId() {
        doReturn(false).when(applicationParams).isStreamDownloadEnabled();
        doReturn(AuthorisedService.builder().build()).when(documentManagementService)
                .checkServicePermission(
                        DOCUMENT_WITH_FUTURE_TTL.getCaseTypeId(),
                        DOCUMENT_WITH_FUTURE_TTL.getJurisdictionId(),
                        SERVICE_NAME_XUI_WEBAPP,
                        Permission.READ,
                        SERVICE_PERMISSION_ERROR,
                        MATCHED_DOCUMENT_ID.toString());


        doReturn(DOCUMENT_WITH_PAST_TTL).when(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID);

        ForbiddenException thrown = assertThrows(
            ForbiddenException.class,
            () -> testee.getDocumentBinaryContentByDocumentId(MATCHED_DOCUMENT_ID, null,
                                                              TEST_S2S_TOKEN, Map.of()),
            "Failed to throw ForbiddenException"
        );

        assertAll(
            () -> assertTrue(thrown.getMessage()
                    .contains("Document " + MATCHED_DOCUMENT_ID + " can not be downloaded as TTL has expired")),
            () -> verify(documentManagementService, never()).checkUserPermission(
                    DOCUMENT_WITH_FUTURE_TTL.getCaseId(),
                    MATCHED_DOCUMENT_ID,
                    Permission.READ,
                    USER_PERMISSION_ERROR,
                    MATCHED_DOCUMENT_ID.toString())
        );
    }

    @Test
    void shouldThrowForbiddenExceptionWhenRetrievingDocumentBinaryContentWhenDocumentMetadataHasNullTTL() {
        doReturn(false).when(applicationParams).isStreamDownloadEnabled();
        doReturn(AuthorisedService.builder().build()).when(documentManagementService)
                .checkServicePermission(
                        DOCUMENT_WITH_FUTURE_TTL.getCaseTypeId(),
                        DOCUMENT_WITH_FUTURE_TTL.getJurisdictionId(),
                        SERVICE_NAME_XUI_WEBAPP,
                        Permission.READ,
                        SERVICE_PERMISSION_ERROR,
                        MATCHED_DOCUMENT_ID.toString());


        doReturn(DOCUMENT).when(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID);

        ForbiddenException thrown = assertThrows(
            ForbiddenException.class,
            () -> testee.getDocumentBinaryContentByDocumentId(MATCHED_DOCUMENT_ID, null,
                                                              TEST_S2S_TOKEN, Map.of()),
            "Failed to throw ForbiddenException"
        );

        assertAll(
            () -> assertTrue(thrown.getMessage()
                    .contains("Document " + MATCHED_DOCUMENT_ID + " can not be downloaded as TTL has expired")),
            () -> verify(documentManagementService, never()).checkUserPermission(
                    DOCUMENT_WITH_FUTURE_TTL.getCaseId(),
                    MATCHED_DOCUMENT_ID,
                    Permission.READ,
                    USER_PERMISSION_ERROR,
                    MATCHED_DOCUMENT_ID.toString())
        );
    }

    @Test
    @DisplayName("should throw 403 forbidden  when the requested document does not have read permission")
    void shouldThrowForbiddenWhenDocumentDoesNotHaveReadPermission() {
        doReturn(false).when(applicationParams).isStreamDownloadEnabled();
        doReturn(AuthorisedService.builder().build()).when(documentManagementService)
            .checkServicePermission(DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    SERVICE_NAME_XUI_WEBAPP,
                                    Permission.READ,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString());
        doThrow(ForbiddenException.class).when(documentManagementService)
            .getDocumentBinaryContent(MATCHED_DOCUMENT_ID);

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> testee.getDocumentBinaryContentByDocumentId(MATCHED_DOCUMENT_ID, null,
                                                                          TEST_S2S_TOKEN,Map.of()));
    }


    @Test
    @DisplayName("should throw 403 forbidden when the requested document does not match with available doc")
    void shouldThrowForbiddenWhenDocumentDoesNotMatch() {
        doReturn(false).when(applicationParams).isStreamDownloadEnabled();
        Optional<DocumentPermissions> documentPermissions = Optional.of(getDocumentPermissions(
            Arrays.asList(
                Permission.CREATE,
                Permission.READ
            )
        ));
        doReturn(DOCUMENT_WITH_CASE_ID).when(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID);
        doReturn(documentPermissions).when(caseDataStoreService)
            .getCaseDocumentMetadata(CASE_ID_VALUE, MATCHED_DOCUMENT_ID);
        doThrow(ForbiddenException.class).when(documentManagementService)
            .getDocumentBinaryContent(MATCHED_DOCUMENT_ID);
        doReturn(AuthorisedService.builder().build()).when(documentManagementService)
            .checkServicePermission(DOCUMENT_WITH_CASE_ID.getCaseTypeId(),
                                    DOCUMENT_WITH_CASE_ID.getJurisdictionId(),
                                    SERVICE_NAME_XUI_WEBAPP,
                                    Permission.READ,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString());

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> testee.getDocumentBinaryContentByDocumentId(MATCHED_DOCUMENT_ID, null,
                                                                          TEST_S2S_TOKEN, Map.of()));
    }

    @Test
    @DisplayName("should throw 403 forbidden when the service is not authorised to access")
    void shouldThrowForbiddenWhenServiceIsNotAuthorised() {
        doReturn(false).when(applicationParams).isStreamDownloadEnabled();
        Optional<DocumentPermissions> documentPermissions = Optional.of(getDocumentPermissions(
            Arrays.asList(
                Permission.CREATE,
                Permission.READ
            )
        ));
        doReturn(DOCUMENT).when(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID);
        doReturn(documentPermissions).when(caseDataStoreService)
            .getCaseDocumentMetadata(CASE_ID_VALUE, MATCHED_DOCUMENT_ID);
        doReturn(setDocumentBinaryContent(FORBIDDEN)).when(documentManagementService)
            .getDocumentBinaryContent(MATCHED_DOCUMENT_ID);
        doThrow(ForbiddenException.class).when(documentManagementService)
            .checkServicePermission(DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    SERVICE_NAME_XUI_WEBAPP,
                                    Permission.READ,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString());

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> testee.getDocumentBinaryContentByDocumentId(MATCHED_DOCUMENT_ID, null,
                                                                          TEST_S2S_TOKEN, Map.of()));
    }

    @Test
    @DisplayName("should get 204 when document delete is successful")
    void shouldDeleteDocumentByDocumentId() {
        doReturn(AuthorisedService.builder().build()).when(documentManagementService)
            .checkServicePermission(DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    SERVICE_NAME_XUI_WEBAPP,
                                    Permission.UPDATE,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString());
        doNothing().when(documentManagementService)
            .checkUserPermission(DOCUMENT.getCaseId(),
                                 MATCHED_DOCUMENT_ID,
                                 Permission.UPDATE,
                                 USER_PERMISSION_ERROR,
                                 MATCHED_DOCUMENT_ID.toString());
        doNothing().when(documentManagementService).deleteDocument(MATCHED_DOCUMENT_ID, true);

        final ResponseEntity<Void> response = testee.deleteDocumentByDocumentId(MATCHED_DOCUMENT_ID,
                                                                                true,
                                                                                TEST_S2S_TOKEN);

        assertAll(
            () -> assertNotNull(response, VALID_RESPONSE),
            () -> assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode(), NO_CONTENT_RESPONSE_CODE)
        );
    }

    @Test
    @DisplayName("should get 403 when service is not authorised")
    void shouldNotAllowDeleteDocumentByDocumentIdWhenServiceIsNotAuthorised() {
        doThrow(ForbiddenException.class).when(documentManagementService)
            .checkServicePermission(DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    SERVICE_NAME_XUI_WEBAPP,
                                    Permission.UPDATE,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString());
        doNothing().when(documentManagementService)
            .checkUserPermission(DOCUMENT.getCaseId(),
                                 MATCHED_DOCUMENT_ID,
                                 Permission.UPDATE,
                                 USER_PERMISSION_ERROR,
                                 MATCHED_DOCUMENT_ID.toString());

        doNothing().when(documentManagementService).deleteDocument(MATCHED_DOCUMENT_ID, true);

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> testee.deleteDocumentByDocumentId(MATCHED_DOCUMENT_ID, true, TEST_S2S_TOKEN));
    }

    @Test
    void shouldPatchDocumentByDocumentId() {
        doReturn(AuthorisedService.builder().build()).when(documentManagementService)
            .checkServicePermission(DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    SERVICE_NAME_XUI_WEBAPP,
                                    Permission.UPDATE,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString());
        doNothing().when(documentManagementService)
            .checkUserPermission(DOCUMENT.getCaseId(),
                                 MATCHED_DOCUMENT_ID,
                                 Permission.UPDATE,
                                 USER_PERMISSION_ERROR,
                                 MATCHED_DOCUMENT_ID.toString());

        final UpdateTtlRequest body = new UpdateTtlRequest(null);
        final PatchDocumentResponse patchDocumentResponse = PatchDocumentResponse.builder()
            .originalDocumentName("test.png")
            .build();
        doReturn(patchDocumentResponse)
            .when(documentManagementService).patchDocument(MATCHED_DOCUMENT_ID, body);

        final ResponseEntity<PatchDocumentResponse> response = testee.patchDocumentByDocumentId(
            MATCHED_DOCUMENT_ID,
            body,
            TEST_S2S_TOKEN
        );
        assertAll(
            () -> assertNotNull(response, VALID_RESPONSE),
            () -> assertEquals(HttpStatus.OK, response.getStatusCode(), RESPONSE_CODE)
        );
    }

    @Test
    void shouldNotAllowPatchDocumentByDocumentIdWhenServiceIsNotAuthorised() {
        doReturn(DOCUMENT).when(documentManagementService).getDocumentMetadata(DOCUMENT_ID);
        doThrow(ForbiddenException.class).when(documentManagementService)
            .checkServicePermission(DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    SERVICE_NAME_XUI_WEBAPP,
                                    Permission.UPDATE,
                                    SERVICE_PERMISSION_ERROR,
                                    DOCUMENT_ID.toString());
        final UpdateTtlRequest body = new UpdateTtlRequest();

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> testee.patchDocumentByDocumentId(DOCUMENT_ID, body, TEST_S2S_TOKEN));
    }

    @Test
    void shouldNotPatchMetaDataOnDocumentsWhenServiceIsNotAuthorised() {
        doThrow(ForbiddenException.class).when(documentManagementService).checkServicePermission(
            anyString(),
            anyString(),
            anyString(),
            eq(Permission.ATTACH),
            eq(SERVICE_PERMISSION_ERROR),
            anyString()
        );
        final CaseDocumentsMetadata body = CaseDocumentsMetadata.builder()
            .caseTypeId("")
            .jurisdictionId("")
            .build();

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> testee.patchMetaDataOnDocuments(body, TEST_S2S_TOKEN));
    }

    @Test
    void shouldPatchMetaDataOnDocuments() {
        doReturn(AuthorisedService.builder().build()).when(documentManagementService)
            .checkServicePermission(eq(DOCUMENT.getCaseTypeId()),
                                    eq(DOCUMENT.getJurisdictionId()),
                                    eq(SERVICE_NAME_XUI_WEBAPP),
                                    eq(Permission.ATTACH),
                                    eq(SERVICE_PERMISSION_ERROR),
                                    anyString());
        DocumentHashToken document = DocumentHashToken.builder()
            .id(UUID.fromString("cab18c21-8b7c-452b-937c-091225e0cc12"))
            .build();
        CaseDocumentsMetadata body = CaseDocumentsMetadata.builder()
            .caseId("1111122222333334")
            .documentHashTokens(Collections.singletonList(document))
            .caseTypeId(CASE_TYPE_ID_VALUE)
            .jurisdictionId(JURISDICTION_ID_VALUE)
            .build();

        final ResponseEntity<PatchDocumentMetaDataResponse> response = testee.patchMetaDataOnDocuments(
            body,
            TEST_S2S_TOKEN
        );

        assertAll(
            () -> assertNotNull(response, VALID_RESPONSE),
            () -> assertEquals(HttpStatus.OK, response.getStatusCode(), RESPONSE_CODE),
            () -> verify(documentManagementService).checkServicePermission(eq(CASE_TYPE_ID_VALUE),
                                                                           eq(JURISDICTION_ID_VALUE),
                                                                           eq(SERVICE_NAME_XUI_WEBAPP),
                                                                           eq(Permission.ATTACH),
                                                                           eq(SERVICE_PERMISSION_ERROR),
                                                                           anyString())
        );
    }

    @Test
    @DisplayName("Should go through happy path")
    void uploadDocuments_HappyPath() {
        doReturn(false).when(applicationParams).isStreamUploadEnabled();

        UploadResponse mockResponse = new UploadResponse(List.of(Document.builder().build()));

        doReturn(AuthorisedService.builder().build()).when(documentManagementService).checkServicePermission(
            eq(CASE_TYPE_ID_VALUE),
            eq(JURISDICTION_ID_VALUE),
            eq(SERVICE_NAME_XUI_WEBAPP),
            eq(Permission.CREATE),
            eq(SERVICE_PERMISSION_ERROR),
            anyString()
        );
        List<MultipartFile> multipartFiles = generateMultipartList();


        final DocumentUploadRequest documentUploadRequest = new DocumentUploadRequest(
            multipartFiles,
            Classification.PUBLIC.name(),
            CASE_TYPE_ID_VALUE,
            JURISDICTION_ID_VALUE
        );

        doReturn(mockResponse).when(documentManagementService).uploadDocuments(documentUploadRequest);

        UploadResponse finalResponse = testee.uploadDocuments(
            documentUploadRequest,
            bindingResult,
            TEST_S2S_TOKEN
        );

        assertEquals(finalResponse, mockResponse);
    }

    @Test
    @DisplayName("Should go through happy path streaming")
    void uploadDocumentsAsStreaming_HappyPath() {
        doReturn(true).when(applicationParams).isStreamUploadEnabled();

        UploadResponse mockResponse = new UploadResponse(List.of(Document.builder().build()));

        doReturn(AuthorisedService.builder().build()).when(documentManagementService).checkServicePermission(
            eq(CASE_TYPE_ID_VALUE),
            eq(JURISDICTION_ID_VALUE),
            eq(SERVICE_NAME_XUI_WEBAPP),
            eq(Permission.CREATE),
            eq(SERVICE_PERMISSION_ERROR),
            anyString()
        );
        List<MultipartFile> multipartFiles = generateMultipartList();


        final DocumentUploadRequest documentUploadRequest = new DocumentUploadRequest(
            multipartFiles,
            Classification.PUBLIC.name(),
            CASE_TYPE_ID_VALUE,
            JURISDICTION_ID_VALUE
        );

        doReturn(mockResponse).when(documentManagementService).uploadStreamDocuments(documentUploadRequest);

        UploadResponse finalResponse = testee.uploadDocuments(
            documentUploadRequest,
            bindingResult,
            TEST_S2S_TOKEN
        );

        assertEquals(finalResponse, mockResponse);
        verify(documentManagementService, times(1)).uploadStreamDocuments(documentUploadRequest);
    }

    @Test
    void testShouldRaiseExceptionWhenBindingResultHasErrorsDuringUploadDocuments() {
        doReturn(false).when(applicationParams).isStreamUploadEnabled();

        doReturn(true).when(bindingResult).hasErrors();

        final DocumentUploadRequest documentUploadRequest = new DocumentUploadRequest(
            generateMultipartList(),
            Classification.PUBLIC.name(),
            CASE_TYPE_ID_VALUE,
                JURISDICTION_ID_VALUE
        );

        assertThatExceptionOfType(BadRequestException.class)
            .isThrownBy(() -> testee.uploadDocuments(documentUploadRequest, bindingResult, TEST_S2S_TOKEN));
    }

    private DocumentPermissions getDocumentPermissions(List<Permission> permission) {
        return DocumentPermissions.builder()
            .permissions(permission)
            .id(UNMATCHED_DOCUMENT_ID)
            .build();
    }

    private ResponseEntity<ByteArrayResource> setDocumentBinaryContent(String responseType) {
        if (responseType.equals("OK")) {
            return new ResponseEntity<>(
                new ByteArrayResource("test document content".getBytes()),
                getHttpHeaders(),
                HttpStatus.OK
            );
        } else if (responseType.equals(FORBIDDEN)) {
            return new ResponseEntity<>(
                new ByteArrayResource("".getBytes()),
                getHttpHeaders(),
                HttpStatus.FORBIDDEN
            );
        }

        return new ResponseEntity<>(new ByteArrayResource("".getBytes()), HttpStatus.OK);
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
        final String contentType = "image/jpeg";

        return List.of(
            new MockMultipartFile("file1", "test1.jpg", contentType, "HelloString".getBytes()),
            new MockMultipartFile("file2", "test2.jpg", contentType, "HelloString2".getBytes())
        );
    }

    @Test
    void generateHashCode_HappyPath() {

        doReturn(DOCUMENT).when(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID);
        doReturn(AuthorisedService.builder().build()).when(documentManagementService)
            .checkServicePermission(
                DOCUMENT.getCaseTypeId(),
                DOCUMENT.getJurisdictionId(),
                SERVICE_NAME_XUI_WEBAPP,
                Permission.HASHTOKEN,
                SERVICE_PERMISSION_ERROR,
                MATCHED_DOCUMENT_ID.toString()
        );
        when(documentManagementService.generateHashToken(MATCHED_DOCUMENT_ID, AuthorisedService.builder().build(),
                                                         Permission.HASHTOKEN
        ))
            .thenReturn("hashToken");

        final ResponseEntity<GeneratedHashCodeResponse> responseEntity =
            testee.generateHashCode(MATCHED_DOCUMENT_ID, TEST_S2S_TOKEN);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("hashToken", Objects.requireNonNull(responseEntity.getBody()).getHashToken());
    }

    @Test
    //this test returns an illegal argument exception because UUID.fromString() contains a throw for illegal arguments
    void generateHashCode_BadRequest() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> testee.generateHashCode(UUID.fromString("A.A"), TEST_S2S_TOKEN));
    }

    @Test
    void generateHashCode_BadRequestWhenServiceIsNotAuthorised() {

        doReturn(DOCUMENT).when(documentManagementService).getDocumentMetadata(MATCHED_DOCUMENT_ID);
        doThrow(ForbiddenException.class).when(documentManagementService)
            .checkServicePermission(
                DOCUMENT.getCaseTypeId(),
                DOCUMENT.getJurisdictionId(),
                SERVICE_NAME_XUI_WEBAPP,
                Permission.HASHTOKEN,
                SERVICE_PERMISSION_ERROR,
                MATCHED_DOCUMENT_ID.toString()
        );
        when(documentManagementService.generateHashToken(
            MATCHED_DOCUMENT_ID,
            AuthorisedService.builder().build(),
            Permission.HASHTOKEN
        ))
            .thenReturn("hashToken");

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> testee.generateHashCode(MATCHED_DOCUMENT_ID, TEST_S2S_TOKEN));
    }

    @Test
    void shouldPassCheckServicePermissionWithEmptyCaseTypeIdWithCaseTypeIdOptionalFor() {

        List<AuthorisedService> authServices =
            List.of(AuthorisedService.builder()
                        .id("xui_webapp")
                        .caseTypeId(List.of("*"))
                        .jurisdictionId("*")
                        .permissions(List.of(Permission.HASHTOKEN, Permission.READ))
                        .caseTypeIdOptionalFor(List.of(Permission.HASHTOKEN))
                        .build());
        doReturn(authServices).when(serviceConfig).getAuthServices();
        DocumentManagementService documentManagementService =
            new DocumentManagementServiceImpl(null, null,
                                              serviceConfig, null
            );

        assertDoesNotThrow(() -> documentManagementService
            .checkServicePermission(
                DOCUMENT_WITH_JURISDICTION_ID.getCaseTypeId(),
                DOCUMENT_WITH_JURISDICTION_ID.getJurisdictionId(),
                SERVICE_NAME_XUI_WEBAPP,
                Permission.HASHTOKEN,
                SERVICE_PERMISSION_ERROR,
                MATCHED_DOCUMENT_ID.toString()
            ));
        verify(serviceConfig, times(1)).getAuthServices();
    }

    @Test
    void shouldPassCheckServicePermissionWithEmptyJurisdictionIdWithJurisdictionIdOptionalFor() {

        List<AuthorisedService> authServices =
            List.of(AuthorisedService.builder()
                        .id("xui_webapp")
                        .caseTypeId(List.of("*"))
                        .jurisdictionId("*")
                        .permissions(List.of(Permission.HASHTOKEN, Permission.READ))
                        .jurisdictionIdOptionalFor(List.of(Permission.HASHTOKEN))
                        .build());
        doReturn(authServices).when(serviceConfig).getAuthServices();
        DocumentManagementService documentManagementService =
            new DocumentManagementServiceImpl(null, null,
                                              serviceConfig, null
            );

        assertDoesNotThrow(() -> documentManagementService
            .checkServicePermission(
                DOCUMENT_WITH_CASE_TYPE_ID.getCaseTypeId(),
                DOCUMENT_WITH_CASE_TYPE_ID.getJurisdictionId(),
                SERVICE_NAME_XUI_WEBAPP,
                Permission.HASHTOKEN,
                SERVICE_PERMISSION_ERROR,
                MATCHED_DOCUMENT_ID.toString()
            ));
        verify(serviceConfig, times(1)).getAuthServices();
    }

    @Test
    void shouldPassCheckServicePermissionWithNonEmptyCaseTypeIdWithoutCaseTypeIdOptionalFor() {

        List<AuthorisedService> authServices =
            List.of(AuthorisedService.builder()
                        .id("xui_webapp")
                        .caseTypeId(List.of("*"))
                        .jurisdictionId("*")
                        .permissions(List.of(Permission.CREATE, Permission.READ))
                        .build());
        doReturn(authServices).when(serviceConfig).getAuthServices();
        DocumentManagementService documentManagementService =
            new DocumentManagementServiceImpl(null, null,
                                              serviceConfig, null
            );

        assertDoesNotThrow(() -> documentManagementService
            .checkServicePermission(
                DOCUMENT_WITH_CASE_TYPE_ID.getCaseTypeId(),
                DOCUMENT_WITH_JURISDICTION_ID.getJurisdictionId(),
                SERVICE_NAME_XUI_WEBAPP,
                Permission.READ,
                SERVICE_PERMISSION_ERROR,
                MATCHED_DOCUMENT_ID.toString()
            ));
        verify(serviceConfig, times(1)).getAuthServices();
    }

    @Test
    void shouldPassCheckServicePermissionWithNonEmptyJurisdictionIdWithoutJurisdictionIdOptionalFor() {

        List<AuthorisedService> authServices =
            List.of(AuthorisedService.builder()
                        .id("xui_webapp")
                        .caseTypeId(List.of("*"))
                        .jurisdictionId("*")
                        .permissions(List.of(Permission.CREATE, Permission.READ))
                        .build());
        doReturn(authServices).when(serviceConfig).getAuthServices();
        DocumentManagementService documentManagementService =
            new DocumentManagementServiceImpl(null, null,
                                              serviceConfig, null
            );

        assertDoesNotThrow(() -> documentManagementService
            .checkServicePermission(
                DOCUMENT_WITH_CASE_TYPE_ID.getCaseTypeId(),
                DOCUMENT_WITH_JURISDICTION_ID.getJurisdictionId(),
                SERVICE_NAME_XUI_WEBAPP,
                Permission.READ,
                SERVICE_PERMISSION_ERROR,
                MATCHED_DOCUMENT_ID.toString()
            ));
        verify(serviceConfig, times(1)).getAuthServices();
    }

    @Test
    void shouldPassCheckServicePermissionWithNonEmptyCaseTypeIdWithCaseTypeIdOptionalFor() {

        List<AuthorisedService> authServices =
            List.of(AuthorisedService.builder()
                        .id("xui_webapp")
                        .caseTypeId(List.of("*"))
                        .jurisdictionId("*")
                        .permissions(List.of(Permission.CREATE, Permission.HASHTOKEN))
                        .caseTypeIdOptionalFor(List.of(Permission.HASHTOKEN))
                        .build());
        doReturn(authServices).when(serviceConfig).getAuthServices();
        DocumentManagementService documentManagementService =
            new DocumentManagementServiceImpl(null, null,
                                              serviceConfig, null
            );

        assertDoesNotThrow(() -> documentManagementService
            .checkServicePermission(
                DOCUMENT_WITH_CASE_TYPE_ID.getCaseTypeId(),
                DOCUMENT_WITH_JURISDICTION_ID.getJurisdictionId(),
                SERVICE_NAME_XUI_WEBAPP,
                Permission.HASHTOKEN,
                SERVICE_PERMISSION_ERROR,
                MATCHED_DOCUMENT_ID.toString()
            ));
        verify(serviceConfig, times(1)).getAuthServices();
    }

    @Test
    void shouldPassCheckServicePermissionWithNonEmptyJurisdictionIdWithJurisdictionIdOptionalFor() {

        List<AuthorisedService> authServices =
            List.of(AuthorisedService.builder()
                        .id("xui_webapp")
                        .caseTypeId(List.of("*"))
                        .jurisdictionId("*")
                        .permissions(List.of(Permission.CREATE, Permission.HASHTOKEN))
                        .caseTypeIdOptionalFor(List.of(Permission.HASHTOKEN))
                        .jurisdictionIdOptionalFor(List.of(Permission.HASHTOKEN))
                        .build());
        doReturn(authServices).when(serviceConfig).getAuthServices();
        DocumentManagementService documentManagementService =
            new DocumentManagementServiceImpl(null, null,
                                              serviceConfig, null
            );

        assertDoesNotThrow(() -> documentManagementService
            .checkServicePermission(
                DOCUMENT_WITH_CASE_TYPE_ID.getCaseTypeId(),
                DOCUMENT_WITH_JURISDICTION_ID.getJurisdictionId(),
                SERVICE_NAME_XUI_WEBAPP,
                Permission.HASHTOKEN,
                SERVICE_PERMISSION_ERROR,
                MATCHED_DOCUMENT_ID.toString()
            ));
        verify(serviceConfig, times(1)).getAuthServices();
    }

    @Test
    void shouldFailCheckServicePermissionWithEmptyCaseTypeIdAndWithoutCaseTypeIdOptionalFor() {
        List<AuthorisedService> authServices =
            List.of(AuthorisedService.builder()
                        .id("xui_webapp")
                        .caseTypeId(List.of("*"))
                        .jurisdictionId("*")
                        .permissions(List.of(Permission.HASHTOKEN, Permission.READ))
                        .build());
        doReturn(authServices).when(serviceConfig).getAuthServices();
        DocumentManagementService documentManagementService =
            new DocumentManagementServiceImpl(null, null,
                                              serviceConfig, null
            );

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() ->
                            documentManagementService
                                .checkServicePermission(
                                    DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    SERVICE_NAME_XUI_WEBAPP,
                                    Permission.READ,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString()
                                ));
        verify(serviceConfig, times(1)).getAuthServices();
    }

    @Test
    void shouldFailCheckServicePermissionWithEmptyJurisdictionIdAndWithoutJurisdictionIdOptionalFor() {
        List<AuthorisedService> authServices =
            List.of(AuthorisedService.builder()
                        .id("xui_webapp")
                        .caseTypeId(List.of("*"))
                        .jurisdictionId("*")
                        .permissions(List.of(Permission.HASHTOKEN, Permission.READ))
                        .caseTypeIdOptionalFor(List.of(Permission.HASHTOKEN))
                        .build());
        doReturn(authServices).when(serviceConfig).getAuthServices();
        DocumentManagementService documentManagementService =
            new DocumentManagementServiceImpl(null, null,
                                              serviceConfig, null
            );

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() ->
                            documentManagementService
                                .checkServicePermission(
                                    DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    SERVICE_NAME_XUI_WEBAPP,
                                    Permission.HASHTOKEN,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString()
                                ));
        verify(serviceConfig, times(1)).getAuthServices();
    }

    @Test
    void shouldFailCheckServicePermissionWithEmptyCaseTypeIdAndWithInsufficientCaseTypeIdOptionalFor() {
        List<AuthorisedService> authServices =
            List.of(AuthorisedService.builder()
                        .id("xui_webapp")
                        .caseTypeId(List.of("*"))
                        .jurisdictionId("*")
                        .permissions(List.of(Permission.HASHTOKEN, Permission.READ))
                        .caseTypeIdOptionalFor(List.of(Permission.HASHTOKEN))
                        .build());
        doReturn(authServices).when(serviceConfig).getAuthServices();
        DocumentManagementService documentManagementService =
            new DocumentManagementServiceImpl(null, null,
                                              serviceConfig, null
            );

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() ->
                            documentManagementService
                                .checkServicePermission(
                                    DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    SERVICE_NAME_XUI_WEBAPP,
                                    Permission.READ,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString()
                                ));
        verify(serviceConfig, times(1)).getAuthServices();
    }

    @Test
    void shouldFailCheckServicePermissionWithEmptyJurisdictionIdAndWithInsufficientJurisdictionIdOptionalFor() {
        List<AuthorisedService> authServices =
            List.of(AuthorisedService.builder()
                        .id("xui_webapp")
                        .caseTypeId(List.of("*"))
                        .jurisdictionId("*")
                        .permissions(List.of(Permission.HASHTOKEN, Permission.READ))
                        .caseTypeIdOptionalFor(List.of(Permission.READ))
                        .jurisdictionIdOptionalFor(List.of(Permission.HASHTOKEN))
                        .build());
        doReturn(authServices).when(serviceConfig).getAuthServices();
        DocumentManagementService documentManagementService =
            new DocumentManagementServiceImpl(null, null,
                                              serviceConfig, null
            );

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() ->
                            documentManagementService
                                .checkServicePermission(
                                    DOCUMENT.getCaseTypeId(),
                                    DOCUMENT.getJurisdictionId(),
                                    SERVICE_NAME_XUI_WEBAPP,
                                    Permission.READ,
                                    SERVICE_PERMISSION_ERROR,
                                    MATCHED_DOCUMENT_ID.toString()
                                ));
        verify(serviceConfig, times(1)).getAuthServices();
    }
}
