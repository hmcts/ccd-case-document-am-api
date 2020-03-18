package uk.gov.hmcts.reform.ccd.document.am;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import net.serenitybdd.rest.SerenityRest;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RunWith(SpringIntegrationSerenityRunner.class)
public class SmokeTest {

    public String baseURI = "http://localhost:4455";
//    private static final String targetInstance = StringUtils.defaultIfBlank(System.getenv("TEST_URL"),
//        "http://localhost:4455");

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Test
    public void should_receive_response_for_a_get_document_meta_data() {

        String targetInstance = baseURI + "/cases/documents/00000000-0000-0000-0000-000000000000";

        RestAssured.baseURI = targetInstance;
        RestAssured.useRelaxedHTTPSValidation();

        Response response = SerenityRest
            .given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .header("user-roles", "caseworker")
            .header( "ServiceAuthorization", "Bearer " + authTokenGenerator.generate())
            .when()
            .get("/")
            .andReturn();
        response.then().assertThat().statusCode( HttpStatus.NOT_FOUND.value())
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
            .header( "ServiceAuthorization", "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE1ODQ1NDk3MjB9.N0YyrWubmWhADNAKROkjD1OXI9gPWaT1f5D07IViOnf1x_jpuDSnP0M3n6SPAlLImMCY2ExV5cZVHv9OJjufvQ" )
            .when()
            .get("/")
            .andReturn();
        response.then().assertThat().statusCode( HttpStatus.NOT_FOUND.value())
            .body("message", Matchers.equalTo("Resource not found 00000000-0000-0000-0000-000000000000"));
    }

}
