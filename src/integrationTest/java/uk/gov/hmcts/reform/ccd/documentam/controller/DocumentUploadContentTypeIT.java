package uk.gov.hmcts.reform.ccd.documentam.controller;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.ReflectionUtils;
import uk.gov.hmcts.reform.ccd.documentam.ApplicationParams;
import uk.gov.hmcts.reform.ccd.documentam.BaseTest;
import uk.gov.hmcts.reform.ccd.documentam.TestFixture;
import uk.gov.hmcts.reform.ccd.documentam.model.DmUploadResponse;
import uk.gov.hmcts.reform.ccd.documentam.model.Document;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Classification;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CASE_TYPE_ID;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CLASSIFICATION;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.FILES;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.JURISDICTION_ID;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.stubDocumentManagementUploadDocument;

class DocumentUploadContentTypeIT extends BaseTest implements TestFixture {

    private static final String UPLOAD_URL = "/cases/documents";

    private static final String NON_ASCII_FILENAME = "résumé.pdf";
    private static final List<String> MALFORMED_TYPES = List.of(
        "garbage", "text/", "/html", "text/html;;charset", "text/html; charset=", "*/*", "text/*", " ", "a b/c d");

    private static final byte[] TXT_BYTES = "<html><body>hello</body></html>".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PDF_BYTES = {'%', 'P', 'D', 'F', '-', '1', '.', '7', 0, (byte) 0xFF, (byte) 0xC3, 10};
    private static final byte[] DOCX_BYTES = {'P', 'K', 3, 4, 0, (byte) 0x80, (byte) 0xFE, 0, 1, 2};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationParams applicationParams;

    @BeforeEach
    void setUp() {
        WireMock.resetAllRequests();
        Document document = Document.builder()
            .originalDocumentName(ORIGINAL_DOCUMENT_NAME)
            .size(1L)
            .classification(Classification.PUBLIC)
            .links(TestFixture.getLinks())
            .build();
        stubDocumentManagementUploadDocument(DmUploadResponse.builder()
            .embedded(DmUploadResponse.Embedded.builder().documents(List.of(document)).build())
            .build());
    }

    @AfterEach
    void tearDown() {
        setStreaming(false);
    }

    @Test
    void streaming_forwardsDeclaredContentTypeFilenameAndBytesPerFile() throws Exception {
        setStreaming(true);
        upload(
            new MockMultipartFile(FILES, "sample.txt", "text/html", TXT_BYTES),
            new MockMultipartFile(FILES, "report.pdf", "application/pdf", PDF_BYTES),
            new MockMultipartFile(FILES, "letter.docx", "application/octet-stream", DOCX_BYTES)
        );

        assertFileCount(3);
        assertFile(0, "sample.txt", "text/html", TXT_BYTES);
        assertFile(1, "report.pdf", "application/pdf", PDF_BYTES);
        assertFile(2, "letter.docx", "application/octet-stream", DOCX_BYTES);
    }

    @Test
    void nonStreaming_forwardsDeclaredContentTypeFilenameAndBytesPerFile() throws Exception {
        setStreaming(false);
        upload(
            new MockMultipartFile(FILES, "sample.txt", "text/html", TXT_BYTES),
            new MockMultipartFile(FILES, "report.pdf", "application/pdf", PDF_BYTES),
            new MockMultipartFile(FILES, "letter.docx", "application/octet-stream", DOCX_BYTES)
        );

        assertFileCount(3);
        assertFile(0, "sample.txt", "text/html", TXT_BYTES);
        assertFile(1, "report.pdf", "application/pdf", PDF_BYTES);
        assertFile(2, "letter.docx", "application/octet-stream", DOCX_BYTES);
    }

    @Test
    void nonStreaming_preservesContentTypeParameters() throws Exception {
        setStreaming(false);
        upload(new MockMultipartFile(FILES, "sample.txt", "text/html; charset=ISO-8859-1", TXT_BYTES));

        assertThat(MediaType.parseMediaType(filePart(0).getHeaders().getHeader("Content-Type").firstValue()))
            .isEqualTo(MediaType.parseMediaType("text/html; charset=ISO-8859-1"));
    }

    @Test
    void nonStreaming_missingContentType_forwardedAsOctetStream() throws Exception {
        setStreaming(false);
        upload(new MockMultipartFile(FILES, "sample.txt", null, TXT_BYTES));

        assertThat(filePart(0).getHeaders().getHeader("Content-Type").firstValue())
            .isEqualTo("application/octet-stream");
        assertThat(filePart(0).getBody().asBytes()).isEqualTo(TXT_BYTES);
    }

    @Test
    void nonStreaming_nonAsciiFilename_forwardedUnchanged() throws Exception {
        setStreaming(false);
        upload(new MockMultipartFile(FILES, NON_ASCII_FILENAME, "application/pdf", PDF_BYTES));

        assertThat(forwardedFilename(filePart(0))).isEqualTo(NON_ASCII_FILENAME);
        assertThat(filePart(0).getBody().asBytes()).isEqualTo(PDF_BYTES);
    }

    @Test
    void streaming_nonAsciiFilename_isSentAsIso88591() throws Exception {
        setStreaming(true);
        upload(new MockMultipartFile(FILES, NON_ASCII_FILENAME, "application/pdf", PDF_BYTES));

        final byte[] rawRequest = WireMock.findAll(postRequestedFor(urlPathEqualTo("/documents")))
            .getFirst().getBody();
        final byte[] expected = ("filename=\"" + NON_ASCII_FILENAME + "\"").getBytes(StandardCharsets.ISO_8859_1);
        assertThat(new String(rawRequest, StandardCharsets.ISO_8859_1))
            .contains(new String(expected, StandardCharsets.ISO_8859_1));
    }

    @Test
    void malformedContentTypes_comparisonOfStreamingAndNonStreaming() throws Exception {
        final StringBuilder table = new StringBuilder(String.format(
            "%n| %-28s | %-34s | %-34s |%n|%s|%s|%s|%n", "declared type", "streaming", "non-streaming",
            "-".repeat(30), "-".repeat(36), "-".repeat(36)));
        for (String declared : MALFORMED_TYPES) {
            final String streaming = outcome(true, declared);
            final String nonStreaming = outcome(false, declared);
            table.append(String.format("| %-28s | %-34s | %-34s |%n", "`" + declared + "`", streaming, nonStreaming));
            assertThat(nonStreaming).as("non-streaming outcome for [%s]", declared)
                .isEqualTo(declared.equals("text/html;;charset") ? "200 text/html" : "200 application/octet-stream");
        }
        System.out.println(table);
    }

    private String outcome(boolean streaming, String declared) throws Exception {
        setStreaming(streaming);
        WireMock.resetAllRequests();
        final int status = mockMvc.perform(buildRequest(new MockMultipartFile(FILES, "a.txt", declared, TXT_BYTES)))
            .andReturn().getResponse().getStatus();
        final var requests = WireMock.findAll(postRequestedFor(urlPathEqualTo("/documents")));
        if (requests.isEmpty()) {
            return status + " (not forwarded)";
        }
        return status + " " + filePart(0).getHeaders().getHeader("Content-Type").firstValue();
    }

    private static String forwardedFilename(com.github.tomakehurst.wiremock.http.Request.Part part) {
        final String disposition = part.getHeaders().getHeader("Content-Disposition").firstValue();
        final Matcher matcher = Pattern.compile("filename=\"([^\"]*)\"").matcher(disposition);
        assertThat(matcher.find()).as("filename in [%s]", disposition).isTrue();
        return matcher.group(1);
    }

    @Test
    void streaming_preservesContentTypeParameters() throws Exception {
        setStreaming(true);
        upload(new MockMultipartFile(FILES, "sample.txt", "text/html; charset=ISO-8859-1", TXT_BYTES));

        assertThat(filePart(0).getHeaders().getHeader("Content-Type").firstValue())
            .isEqualTo("text/html; charset=ISO-8859-1");
    }

    @Test
    void streaming_missingContentType_fallsBackToOctetStream() throws Exception {
        setStreaming(true);
        upload(new MockMultipartFile(FILES, "sample.txt", null, TXT_BYTES));

        assertThat(filePart(0).getHeaders().getHeader("Content-Type").firstValue())
            .isEqualTo("application/octet-stream");
        assertThat(filePart(0).getBody().asBytes()).isEqualTo(TXT_BYTES);
    }

    private void assertFileCount(int expected) {
        final var requests = WireMock.findAll(postRequestedFor(urlPathEqualTo("/documents")));
        assertThat(requests).hasSize(1);
        assertThat(requests.getFirst().getParts().stream().filter(p -> FILES.equals(p.getName())))
            .hasSize(expected);
    }

    private void assertFile(int index, String filename, String contentType, byte[] bytes) {
        final var part = filePart(index);
        assertThat(part.getHeaders().getHeader("Content-Disposition").firstValue())
            .as("Content-Disposition of file %d", index)
            .contains("name=\"files\"")
            .contains("filename=\"" + filename + "\"");
        assertThat(part.getHeaders().getHeader("Content-Type").firstValue())
            .as("Content-Type of file %d (%s)", index, filename).isEqualTo(contentType);
        assertThat(part.getBody().asBytes()).as("bytes of file %d", index).isEqualTo(bytes);
    }

    private com.github.tomakehurst.wiremock.http.Request.Part filePart(int index) {
        final LoggedRequest request = WireMock.findAll(postRequestedFor(urlPathEqualTo("/documents"))).getFirst();
        return request.getParts().stream().filter(p -> FILES.equals(p.getName())).toList().get(index);
    }

    private void upload(MockMultipartFile... files) throws Exception {
        mockMvc.perform(buildRequest(files)).andExpect(status().isOk());
    }

    private MockMultipartHttpServletRequestBuilder buildRequest(MockMultipartFile... files) throws Exception {
        final var builder = MockMvcRequestBuilders.multipart(UPLOAD_URL);
        for (MockMultipartFile file : files) {
            builder.file(file);
        }
        return (MockMultipartHttpServletRequestBuilder) builder
            .part(new MockPart(CLASSIFICATION, "PUBLIC".getBytes()))
            .part(new MockPart(CASE_TYPE_ID, CASE_TYPE_ID_VALUE.getBytes()))
            .part(new MockPart(JURISDICTION_ID, JURISDICTION_ID_VALUE.getBytes()))
            .headers(createHttpHeaders(SERVICE_NAME_XUI_WEBAPP))
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE);
    }

    private void setStreaming(boolean enabled) {
        Field field = ReflectionUtils.findField(ApplicationParams.class, "isStreamUploadEnabled");
        assert field != null;
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, applicationParams, enabled);
    }
}
