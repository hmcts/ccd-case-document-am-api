package uk.gov.hmcts.reform.ccd.documentam.client.datastore;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ForbiddenException;
import uk.gov.hmcts.reform.ccd.documentam.exception.ServiceException;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentMetadata;
import uk.gov.hmcts.reform.ccd.documentam.model.CaseDocumentResource;
import uk.gov.hmcts.reform.ccd.documentam.model.DocumentPermissions;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;
import uk.gov.hmcts.reform.ccd.documentam.security.SecurityUtils;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class CaseDataStoreClientImplTest implements TestFixture {
    @Mock
    private RestTemplate restTemplate;

    @Mock
    private SecurityUtils securityUtils;

    private final String caseDataStoreUrl = "http://localhost";

    private CaseDataStoreClientImpl sut;

    private final String documentUrl = String.format(
        "%s/cases/%s/documents/%s",
        caseDataStoreUrl,
        CASE_ID_VALUE,
        MATCHED_DOCUMENT_ID
    );

    @BeforeEach
    void prepare() {
        MockitoAnnotations.openMocks(this);

        sut = new CaseDataStoreClientImpl(restTemplate, caseDataStoreUrl, securityUtils);
    }

    @Test
    void getCaseDocumentMetadata_HappyPath() {

        final List<Permission> permissionsList = List.of(Permission.READ);
        final DocumentPermissions doc = DocumentPermissions.builder()
            .id(MATCHED_DOCUMENT_ID)
            .permissions(permissionsList)
            .build();
        final CaseDocumentMetadata caseDocumentMetadata = CaseDocumentMetadata.builder()
            .caseId(CASE_ID_VALUE)
            .documentPermissions(doc)
            .build();

        HttpHeaders headers = prepareRequestForUpload();

        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);
        final CaseDocumentResource caseDocumentResource = CaseDocumentResource.builder()
            .documentMetadata(caseDocumentMetadata)
            .build();

        doReturn(new ResponseEntity<>(caseDocumentResource, HttpStatus.OK))
            .when(restTemplate).exchange(documentUrl, HttpMethod.GET, requestEntity, CaseDocumentResource.class);

        final Optional<DocumentPermissions> caseDocumentMetadataResponse =
            sut.getCaseDocumentMetadata(CASE_ID_VALUE, MATCHED_DOCUMENT_ID);

        assertNotNull(caseDocumentMetadataResponse);
        assertEquals(MATCHED_DOCUMENT_ID,caseDocumentMetadataResponse.get().getId());
    }


    @Test
    void getCaseDocumentMetadata_HttpClientErrorNotFound() {

        HttpHeaders headers = prepareRequestForUpload();
        HttpEntity<LinkedMultiValueMap<String, CaseDocumentResource>> requestEntity = new HttpEntity<>(headers);

        HttpClientErrorException failure = new HttpClientErrorException(HttpStatus.NOT_FOUND, "original diagnostic");
        doThrow(failure)
            .when(restTemplate).exchange(documentUrl, HttpMethod.GET, requestEntity, CaseDocumentResource.class);

        Logger logger = (Logger) LoggerFactory.getLogger(CaseDataStoreClientImpl.class);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            logger.setLevel(Level.INFO);
            assertThat(sut.getCaseDocumentMetadata(CASE_ID_VALUE, MATCHED_DOCUMENT_ID)).isEmpty();
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
            assertThat(appender.list.get(0).getThrowableProxy().getMessage()).contains("original diagnostic");
        } finally {
            logger.setLevel(previousLevel);
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void getCaseDocumentMetadata_HttpClientErrorForbiddenException() {

        HttpHeaders headers = prepareRequestForUpload();
        HttpEntity<LinkedMultiValueMap<String, CaseDocumentResource>> requestEntity = new HttpEntity<>(headers);

        Mockito.when(restTemplate.exchange(documentUrl, HttpMethod.GET, requestEntity, CaseDocumentResource.class))
            .thenThrow(HttpClientErrorException.NotFound.create("woopsie", HttpStatus.FORBIDDEN,
                                                                "404", new HttpHeaders(), new byte[1],
                                                                Charset.defaultCharset()));

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> sut.getCaseDocumentMetadata(CASE_ID_VALUE, MATCHED_DOCUMENT_ID))
            .withCauseInstanceOf(HttpClientErrorException.class);
    }

    @Test
    void getCaseDocumentMetadata_HttpClientErrorBadRequestException() {

        HttpHeaders headers = prepareRequestForUpload();
        HttpEntity<LinkedMultiValueMap<String, CaseDocumentResource>> requestEntity = new HttpEntity<>(headers);

        Mockito.when(restTemplate.exchange(documentUrl, HttpMethod.GET, requestEntity, CaseDocumentResource.class))
            .thenThrow(HttpClientErrorException.NotFound.create("woopsie", HttpStatus.BAD_REQUEST,
                                                                "404", new HttpHeaders(), new byte[1],
                                                                Charset.defaultCharset()));
        assertThatExceptionOfType(BadRequestException.class)
            .isThrownBy(() -> sut.getCaseDocumentMetadata(CASE_ID_VALUE, MATCHED_DOCUMENT_ID))
            .withCauseInstanceOf(HttpClientErrorException.class);
    }

    @Test
    void getCaseDocumentMetadata_ServiceException() {

        HttpHeaders headers = prepareRequestForUpload();
        HttpEntity<LinkedMultiValueMap<String, CaseDocumentResource>> requestEntity = new HttpEntity<>(headers);

        Mockito.when(restTemplate.exchange(documentUrl, HttpMethod.GET, requestEntity, CaseDocumentResource.class))
            .thenThrow(HttpClientErrorException.NotFound.create("woopsie", HttpStatus.UNAUTHORIZED,
                                                                "404", new HttpHeaders(), new byte[1],
                                                                Charset.defaultCharset()));

        assertThatExceptionOfType(ServiceException.class)
            .isThrownBy(() -> sut.getCaseDocumentMetadata(CASE_ID_VALUE, MATCHED_DOCUMENT_ID))
            .withCauseInstanceOf(HttpClientErrorException.class);
    }

    private HttpHeaders prepareRequestForUpload() {

        when(securityUtils.authorizationHeaders()).thenReturn(new HttpHeaders());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }
}
