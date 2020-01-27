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
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
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

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @Autowired
    CaseDocumentAmController caseDocumentAmController;

    protected static final ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() {
        this.mockMvc = standaloneSetup(this.caseDocumentAmController).build();
        final String targetInstance = StringUtils.defaultIfBlank(System.getenv("TEST_URL"),
                                                                 "Need to update URL here");
        RestAssured.baseURI = targetInstance;
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    public void getWelComeAPI() throws Exception
    {
        Response response = SerenityRest
            .given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .when()
            .get("/")
            .andReturn();
        response.then().assertThat().statusCode(SC_OK);

        final MvcResult result = mockMvc.perform(get("/").contentType(MediaType.APPLICATION_JSON)).
            andExpect(status().isOk()).andReturn();
        assertEquals("Welcome to CCD Case Document AM Controller", result.getResponse().getContentAsString());
    }

    @Test
    public void getCaseDetailsAPI() throws Exception
    {
        Response response = SerenityRest
            .given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .when()
            .get("/")
            .andReturn();
        response.then().assertThat().statusCode(SC_OK);

        final MvcResult result  = mockMvc.perform(get("/cases/").contentType(MediaType.APPLICATION_JSON)).
            andExpect(status().isOk()).andReturn();
        List list = mapper.readValue(result.getResponse().getContentAsString(), ArrayList.class);
        assertEquals("C101", list.get(0));
        assertEquals("C102", list.get(1));
        assertEquals("C103", list.get(2));
        assertEquals("C104", list.get(3));
        assertEquals("C105", list.get(4));
    }
}

