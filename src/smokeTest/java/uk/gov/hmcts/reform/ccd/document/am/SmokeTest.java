package uk.gov.hmcts.reform.ccd.document.am;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import net.serenitybdd.rest.SerenityRest;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
@ConfigurationProperties
public class SmokeTest extends BaseTest {

    public String baseURI = "http://localhost:4455";
    @Value("${idam.s2s-auth.totp_secret}")
    String secret;
    @Value("${idam.s2s-auth.microservice}")
    String microService;
    @Value("${idam.s2s-auth.url}")
    String s2sUrl;

//    private static final String targetInstance = StringUtils.defaultIfBlank(System.getenv("TEST_URL"),
//        "http://localhost:4455");

    @Test
    public void should_receive_response_for_a_get_document_meta_data() {

        String serviceAuth = new BaseTest().authTokenGenerator(secret, microService, generateServiceAuthorisationApi(s2sUrl)   )
                                            .generate();

        String targetInstance = baseURI + "/cases/documents/00000000-0000-0000-0000-000000000000";

        RestAssured.baseURI = targetInstance;
        RestAssured.useRelaxedHTTPSValidation();

        Response response = SerenityRest
            .given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .header("user-roles", "caseworker")
            .header( "ServiceAuthorization", "Bearer " + serviceAuth)
            .when()
            .get("/")
            .andReturn();
        response.then().assertThat().statusCode(HttpStatus.NOT_FOUND.value())
                .body("message", Matchers.equalTo("Resource not found 00000000-0000-0000-0000-000000000000"));
    }

    //@Test
    public void should_receive_response_for_a_get_document_binary() {

        String targetInstance = baseURI + "/cases/documents/00000000-0000-0000-0000-000000000000/binary";

        RestAssured.baseURI = targetInstance;
        RestAssured.useRelaxedHTTPSValidation();

        Response response = SerenityRest
            .given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .header("user-roles", "caseworker")
            .header(
                "ServiceAuthorization",
                "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE1ODQ1NDk3MjB9" +
                ".N0YyrWubmWhADNAKROkjD1OXI9gPWaT1f5D07IViOnf1x_jpuDSnP0M3n6SPAlLImMCY2ExV5cZVHv9OJjufvQ"
                   )
            .when()
            .get("/")
            .andReturn();
        response.then().assertThat().statusCode(HttpStatus.NOT_FOUND.value())
                .body("message", Matchers.equalTo("Resource not found 00000000-0000-0000-0000-000000000000"));
    }

}
