package uk.gov.hmcts.reform.ccd.document.am.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@SpringBootTest
@RunWith(SpringRunner.class)
public class CaseDocumentAmControllerTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @Autowired
    CaseDocumentAmController caseDocumentAmController;

    protected static final ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() {
        this.mockMvc = standaloneSetup(this.caseDocumentAmController).build();
    }

    @Test
    public void getWelComeAPI() throws Exception
    {
        final MvcResult result = mockMvc.perform(get("/").contentType(MediaType.APPLICATION_JSON)).
            andExpect(status().isOk()).andReturn();
        assertEquals("Welcome to CCD Case Document AM Controller", result.getResponse().getContentAsString());
    }

    @Test
    public void getCaseDetailsAPI() throws Exception
    {
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
