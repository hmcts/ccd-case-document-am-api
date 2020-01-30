package uk.gov.hmcts.reform.ccd.document.am.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import net.serenitybdd.rest.SerenityRest;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;


@SpringBootTest
@RunWith(SpringIntegrationSerenityRunner.class)
public class CaseDocumentAmControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    CaseDocumentAmController caseDocumentAmController;

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    @Before
    public void setUp() {
        this.mockMvc = standaloneSetup(this.caseDocumentAmController).build();
        final String targetInstance = StringUtils.defaultIfBlank(System.getenv("TEST_URL"),
                                                                 "http://localhost:4455");
        RestAssured.baseURI = targetInstance;
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    public void welComeAPITest() throws Exception {
        Response response = SerenityRest
            .given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .when()
            .get("/")
            .andReturn();
        response.then().assertThat().statusCode(SC_OK);

        final MvcResult result = mockMvc.perform(get("/").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()).andReturn();
        assertEquals("Assert for data","Welcome to CCD Case Document AM Controller", result.getResponse().getContentAsString());
    }

    @Test
    public void caseDetailsAPITest() throws Exception {
        Response response = SerenityRest
            .given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .when()
            .get("/")
            .andReturn();
        response.then().assertThat().statusCode(SC_OK);
        String message = "Assert for case value";
        final MvcResult result  = mockMvc.perform(get("/cases/").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()).andReturn();
        List list = MAPPER.readValue(result.getResponse().getContentAsString(), List.class);
        assertEquals(message, "C101", list.get(0));
        assertEquals(message, "C102", list.get(1));
        assertEquals(message,"C103", list.get(2));
        assertEquals(message,"C104", list.get(3));
        assertEquals(message,"C105", list.get(4));
    }
}

