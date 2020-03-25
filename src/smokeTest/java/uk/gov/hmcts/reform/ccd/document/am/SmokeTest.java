package uk.gov.hmcts.reform.ccd.document.am;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.StringUtils;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import net.serenitybdd.rest.SerenityRest;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

@SpringBootTest
@RunWith(SpringIntegrationSerenityRunner.class)
//@ConfigurationProperties
public class SmokeTest extends BaseTest {

    @Value("${idam.s2s-auth.totp_secret}")
    String secret;
    @Value("${idam.s2s-auth.microservice}")
    String microService;
    @Value("${idam.s2s-auth.url}")
    String s2sUrl;

    private static final String baseURI = StringUtils.defaultIfBlank(System.getenv("CASE_DOCUMENT_AM_URL"),
        "http://localhost:4455");

    @Test
    public void should_receive_response_for_a_get_document_meta_data() {

        String serviceAuth = new BaseTest().authTokenGenerator(secret, microService, generateServiceAuthorisationApi(s2sUrl)).generate();

        String targetInstance = baseURI + "/cases/documents/00000000-0000-0000-0000-000000000000";

        RestAssured.baseURI = targetInstance;
        RestAssured.useRelaxedHTTPSValidation();

        Response response = SerenityRest
            .given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .header("user-roles", "caseworker")
            .header("ServiceAuthorization", "Bearer " + serviceAuth)
            .when()
            .get("/")
            .andReturn();
        response.then().assertThat().statusCode(HttpStatus.NOT_FOUND.value())
            .body("message", Matchers.equalTo("Resource not found 00000000-0000-0000-0000-000000000000"));
    }

    @Test
    public void should_receive_response_for_a_get_document_binary() {

        String targetInstance = baseURI + "/cases/documents/00000000-0000-0000-0000-000000000000/binary";

        RestAssured.baseURI = targetInstance;
        RestAssured.useRelaxedHTTPSValidation();

        Response response = SerenityRest
            .given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .header("user-roles", "caseworker")
            .header("ServiceAuthorization", "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE1ODQ2Mjg1ODl9."
                + "FFxdkaELH1Hip7qaLQaDqQj_gFYTZuU5SnQTT7s2Od4Fz2d9K4Qj2TaxMEMKx0eK8PMSO0IscSpLKUAGjJ4-tw")
            .when()
            .get("/")
            .andReturn();
        response.then().assertThat().statusCode(HttpStatus.NOT_FOUND.value())
            .body("message", Matchers.equalTo("Resource not found 00000000-0000-0000-0000-000000000000"));
    }
}
