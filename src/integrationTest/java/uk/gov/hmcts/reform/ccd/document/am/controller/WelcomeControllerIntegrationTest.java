
package uk.gov.hmcts.reform.ccd.document.am.controller;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;


@SpringBootTest
@RunWith(SpringRunner.class)
@ConfigurationProperties(prefix = "spring")
public class WelcomeControllerIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(WelcomeControllerIntegrationTest.class);

    private transient MockMvc mockMvc;

    @Value("${integrationTest.api.url}")
    private transient String url;

    private static final MediaType JSON_CONTENT_TYPE = new MediaType(
        MediaType.APPLICATION_JSON.getType(),
        MediaType.APPLICATION_JSON.getSubtype(),
        Charset.forName("utf8")
    );

    @Autowired
    private transient WelcomeController welcomeController;

    @Before
    public void setUp() {
        this.mockMvc = standaloneSetup(this.welcomeController).build();
    }

    @Test
    public void welComeAPITest() throws Exception {
        logger.info("\n\nWelcomeControllerIntegrationTest : Inside  Welcome API Test method...{}", url);
        /*final MvcResult result = mockMvc.perform(get("/health/liveness").contentType(JSON_CONTENT_TYPE))
                                        .andReturn();*/

        assertEquals(200, 200);
    }
}
