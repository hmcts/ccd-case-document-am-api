package uk.gov.hmcts.reform.ccd.documentam.controller.endpoints;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class S2SAuthenticationTest {
    private static final Logger log = LoggerFactory.getLogger(S2SAuthenticationTest.class);

    @Inject
    private transient WebApplicationContext wac;

    private static final MediaType JSON_CONTENT_TYPE = new MediaType(
        MediaType.APPLICATION_JSON.getType(),
        MediaType.APPLICATION_JSON.getSubtype(),
        Charset.forName("utf8"));

    @Value("${testS2S.api.url}")
    private transient String url;

    private transient MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    @Ignore // doing nothing - probably delete??
    public void verifyTestS2SAuthentication() {
        String expected = "S2S Authentication is successful !!";
        try {
            final MvcResult result = mockMvc.perform(get(url).contentType(JSON_CONTENT_TYPE))
                .andExpect(status().is(200))
                .andReturn();
            String actual = result.getResponse().getContentAsString();
            assertNotNull("Response body should not be null", actual);
            assertEquals("Response should be match with expected result", actual, expected);
        } catch (Exception e) {
            log.error("Service is not Authorised");
        }
    }
}
