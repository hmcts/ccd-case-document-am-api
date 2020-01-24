package uk.gov.hmcts.reform.ccd.document.am.controllers.endpoints;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;



import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.inject.Inject;

import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestEndpointTest {
    @Inject
    private WebApplicationContext wac;

    protected static final MediaType JSON_CONTENT_TYPE = new MediaType(
        MediaType.APPLICATION_JSON.getType(),
        MediaType.APPLICATION_JSON.getSubtype(),
        Charset.forName("utf8"));

    private  final String URL = "http://localhost:4455/test/helloworld";


    private MockMvc mockMvc;

    @Before
    public void setUP(){
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void verifyTestS2SAuthenticate()
    {
        try {
            final MvcResult result = mockMvc.perform(get(URL).contentType(JSON_CONTENT_TYPE).header(AUTHORIZATION, "Bearer user1"))
                .andExpect(status().is(200))
                .andReturn();
         String responseAsString = result.getResponse().getContentAsString();
            assertNotNull(responseAsString);
            assertEquals(responseAsString, "Hello World !!");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
