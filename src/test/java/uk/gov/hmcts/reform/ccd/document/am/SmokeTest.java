package uk.gov.hmcts.reform.ccd.document.am;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.get;

public class SmokeTest {

    @Before
    public void before() {
        String appUrl = System.getenv("TEST_URL");
        if (appUrl == null) {
            appUrl = "http://localhost:4455";
        }

        RestAssured.baseURI = appUrl;
        RestAssured.useRelaxedHTTPSValidation();
        //LOGGER.info("Base Url set to: " + RestAssured.baseURI);
    }

    @Test
    @Tag("SmokeTest")
    public void shouldReturnWelcomeMessage() {
        Response response = get("/health");
        Assert.assertEquals("message", response.getStatusCode(), 200);

    }
}
