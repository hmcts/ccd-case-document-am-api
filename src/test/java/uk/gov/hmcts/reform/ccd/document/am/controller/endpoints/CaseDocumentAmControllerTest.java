package uk.gov.hmcts.reform.ccd.document.am.controller.endpoints;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.ccd.document.am.model.StoredDocumentHalResource;
import uk.gov.hmcts.reform.ccd.document.am.service.DocumentManagementService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

@SuppressWarnings("PMD.DataflowAnomalyAnalysisRule")
public class CaseDocumentAmControllerTest {
    @InjectMocks
    private transient CaseDocumentAmController testee;

    @Mock
    private transient DocumentManagementService documentManagementService;

    private transient ResponseEntity responseEntity = new ResponseEntity(HttpStatus.OK);
    private transient String serviceAuthorization = "";

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldGetValidMetaDataResponse() {
        doReturn(setDocumentMetaData()).when(documentManagementService).getDocumentMetadata(getUuid());

        ResponseEntity response = testee.getDocumentbyDocumentId(serviceAuthorization, getUuid(), "", "");
        assertNotNull(response, "Valid Response from API");
        assertNotNull(response.getBody(), "Valid response body");
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Status code is OK");
    }

    @Test
    public void shouldNotGetValidMetaDataResponse() {
        doReturn(responseEntity).when(documentManagementService).getDocumentMetadata(getUuid());

        ResponseEntity  response = testee.getDocumentbyDocumentId(serviceAuthorization, getUuid(), "", "");
        assertNull(response.getBody(), "No response");
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Status code");
    }

    @Test
    public void shouldGetDocumentBinaryContent() {
        doReturn(setDocumentBinaryContent()).when(documentManagementService).getDocumentBinaryContent(getUuid());

        ResponseEntity<Object> response = testee.getDocumentBinaryContentbyDocumentId(serviceAuthorization, getUuid(), "", "");
        assertNotNull(response, "Valid binary content from API");
        assertNotNull(response.getBody(), "Valid response body");
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Status code is OK");
    }

    @Test
    public void shouldNotGetDocumentBinaryContentWhenDocumentIdDoesNotExist() {
        doReturn(responseEntity).when(documentManagementService).getDocumentBinaryContent(getUuid());

        ResponseEntity<Object> response = testee.getDocumentBinaryContentbyDocumentId(serviceAuthorization, getUuid(), "", "");
        assertNotNull(response, "Invalid Response from API");
        assertNotNull(response.getBody(), "Empty response body");
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Status code is OK");
    }

    private ResponseEntity<StoredDocumentHalResource> setDocumentMetaData() {
        StoredDocumentHalResource resource = new StoredDocumentHalResource();
        resource.setCreatedBy("test");
        resource.setOriginalDocumentName("test.png");
        return new ResponseEntity<StoredDocumentHalResource>(resource, HttpStatus.ACCEPTED);
    }

    private ResponseEntity<Object> setDocumentBinaryContent() {
        return new ResponseEntity<Object>(new Object(), HttpStatus.ACCEPTED);
    }

    private UUID getUuid() {
        return UUID.fromString("41334a2b-79ce-44eb-9168-2d49a744be9c");
    }
}
