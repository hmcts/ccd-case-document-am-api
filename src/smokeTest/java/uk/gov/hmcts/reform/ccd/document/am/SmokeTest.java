package uk.gov.hmcts.reform.ccd.document.am;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.StringUtils;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import net.serenitybdd.rest.SerenityRest;
import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

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

        String serviceAuth = new BaseTest().authTokenGenerator(secret, microService,
            generateServiceAuthorisationApi(s2sUrl)).generate();
        String targetInstance = baseURI + "/cases/documents/00000000-0000-0000-0000-000000000000";

        RestAssured.baseURI = targetInstance;
        RestAssured.useRelaxedHTTPSValidation();

        Response response = SerenityRest
            .given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .header("user-roles", "caseworker")
            .header( "ServiceAuthorization", "Bearer " + serviceAuth)
            .header( "Authorization", "Bearer " + "Authorization")
            .when()
            .get("/")
            .andReturn();
        response.then().assertThat().statusCode( HttpStatus.NOT_FOUND.value())
            .body("message", Matchers.equalTo("Resource not found 00000000-0000-0000-0000-000000000000"));
    }
//
//    @Test
//    public void should_receive_response_for_a_get_document_binary() {
//
//        String targetInstance = baseURI + "/cases/documents/00000000-0000-0000-0000-000000000000/binary";
//        String serviceAuth = new BaseTest().authTokenGenerator(secret, microService,
//            generateServiceAuthorisationApi(s2sUrl)).generate();
//
//        RestAssured.baseURI = targetInstance;
//        RestAssured.useRelaxedHTTPSValidation();
//
//        Response response = SerenityRest
//            .given()
//            .relaxedHTTPSValidation()
//            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
//            .header( "ServiceAuthorization", "Bearer " + serviceAuth)
//            .header( "Authorization", "Bearer " + BaseTest.getIdamOauth2Token("befta.caseworker.2.solicitor.2@gmail.com", "Pa55word11"))
//            .when()
//            .get("/")
//            .andReturn();
//        response.then().assertThat().statusCode( HttpStatus.NOT_FOUND.value())
//            .body("message", Matchers.equalTo("Resource not found 00000000-0000-0000-0000-000000000000"));
//    }
//
//    @Test
//    public void should_receive_response_for_patch_ttl() {
//
//        String targetInstance = baseURI + "/cases/documents/00000000-0000-0000-0000-000000000000";
//        String serviceAuth = new BaseTest().authTokenGenerator(secret, microService,
//            generateServiceAuthorisationApi(s2sUrl)).generate();
//
//        JSONObject requestBody = new JSONObject();
//        requestBody.put("ttl", "2025-10-31T10:10:10+0000");
//
//        RestAssured.baseURI = targetInstance;
//        RestAssured.useRelaxedHTTPSValidation();
//
//        Response response = SerenityRest
//            .given()
//            .relaxedHTTPSValidation()
//            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
//            .header( "ServiceAuthorization", "Bearer " + serviceAuth)
//            .header( "Authorization", "Bearer " + "Authorization")
//            .body(requestBody.toString())
//            .when()
//            .patch("/")
//            .andReturn();
//        response.then().assertThat().statusCode( HttpStatus.NOT_FOUND.value())
//            .body("message", Matchers.equalTo("Resource not found 00000000-0000-0000-0000-000000000000"));
//    }
//
//    @Test
//    public void should_receive_response_for_patch_attach_to_document() {
//
//        String documentId = "00000000-0000-0000-0000-000000000000";
//        String targetInstance = baseURI + "/cases/documents/"+ documentId;
//        String serviceAuth = new BaseTest().authTokenGenerator(secret, microService,
//            generateServiceAuthorisationApi(s2sUrl)).generate();
//
//        JSONObject document1 = new JSONObject();
//        document1.put("id", documentId);
//        document1.put("hashToken", "3aba5fe28560118793e7f086b442ec40f933c8607ac4e32dc4adf865a0be41c2");
//
//        JSONArray documents = new JSONArray();
//        documents.put(document1);
//
//        JSONObject requestBody = new JSONObject();
//        requestBody.put("caseId", "1234123412341234");
//        requestBody.put("documents", documents);
//
//        RestAssured.baseURI = targetInstance;
//        RestAssured.useRelaxedHTTPSValidation();
//
//        Response response = SerenityRest
//            .given()
//            .relaxedHTTPSValidation()
//            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
//            .header( "ServiceAuthorization", "Bearer " + serviceAuth)
//            .header( "Authorization", "Bearer " + "Authorization")
//            .body(requestBody.toString())
//            .when()
//            .patch("/")
//            .andReturn();
//        response.then().assertThat().statusCode( HttpStatus.NOT_FOUND.value())
//            .body("message", Matchers.equalTo("Resource not found 00000000-0000-0000-0000-000000000000"));
//    }
}
