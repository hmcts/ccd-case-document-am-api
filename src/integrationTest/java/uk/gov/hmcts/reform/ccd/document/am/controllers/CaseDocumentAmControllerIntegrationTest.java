
package uk.gov.hmcts.reform.ccd.document.am.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;


@SpringBootTest
@RunWith(SpringIntegrationSerenityRunner.class)
public class CaseDocumentAmControllerIntegrationTest {

    private final Logger logger = LoggerFactory.getLogger(CaseDocumentAmControllerIntegrationTest.class);
    private MockMvc mockMvc;

    @Value("${integrationTest.api.url}")
    private String url;

    private static final MediaType JSON_CONTENT_TYPE = new MediaType(
        MediaType.APPLICATION_JSON.getType(),
        MediaType.APPLICATION_JSON.getSubtype(),
        Charset.forName("utf8"));

    @Autowired
    CaseDocumentAmController caseDocumentAmController;

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    @Before
    public void setUp() {
        RestAssured.baseURI = url;
        RestAssured.useRelaxedHTTPSValidation();
        this.mockMvc = standaloneSetup(this.caseDocumentAmController).build();
    }

    @Test
    public void welComeAPITest() throws Exception {
        logger.info("\n\nCaseDocumentAmControllerIntegrationTest : Inside  welComeAPITesti method...{}", url);
        /*Response response = SerenityRest
            .given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .when()
            .get(url);*/
        final MvcResult result = mockMvc.perform(get(url).contentType(JSON_CONTENT_TYPE))
            .andExpect(status().is(200))
            .andReturn();
        assertEquals("Assert for data", "Welcome to CCD Case Document AM Controller", result.getResponse().getContentAsString());
    }
}
