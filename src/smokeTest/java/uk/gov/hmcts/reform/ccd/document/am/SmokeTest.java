package uk.gov.hmcts.reform.ccd.document.am;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import net.serenitybdd.rest.SerenityRest;
import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.ccd.document.am.utils.IdamUtils;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest
@RunWith(SpringIntegrationSerenityRunner.class)
//@ConfigurationProperties()
public class SmokeTest extends BaseTest {

    @Value("${idam.s2s-auth.totp_secret}")
    String secret;
    @Value("${idam.s2s-auth.microservice}")
    String microService;
    @Value("${idam.s2s-auth.url}")
    String s2sUrl;
    @Value("${caseDocumentAmUrl}")
    String caseDocumentAmUrl;

    IdamUtils idamUtils =  new IdamUtils();

    String username = "befta.caseworker.2.solicitor.2@gmail.com";
    String password = "Pa55word11";
    String userToken = idamUtils.getIdamOauth2Token(username, password);
    String documentId = "00000000-0000-0000-0000-000000000000";


    @Test
    public void should_receive_response_for_a_get_document_meta_data() {

        RestAssured.baseURI = caseDocumentAmUrl + "/cases/documents/" + documentId;
        RestAssured.useRelaxedHTTPSValidation();

        Response response = SerenityRest
            .given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .header("user-roles", "caseworker")
            .header("ServiceAuthorization", "Bearer " + getServiceAuth())
            .header("Authorization", "Bearer " + userToken)
            .when()
            .get("/")
            .andReturn();
        response.then().assertThat().statusCode(HttpStatus.NOT_FOUND.value())
            .body("message", Matchers.equalTo("Resource not found " + documentId));
    }

    @Test
    public void should_receive_response_for_a_get_document_binary() {

        RestAssured.baseURI = caseDocumentAmUrl + "/cases/documents/" + documentId + "/binary";
        RestAssured.useRelaxedHTTPSValidation();

        Response response = SerenityRest
            .given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .header("ServiceAuthorization", "Bearer " + getServiceAuth())
            .header("Authorization", "Bearer " + userToken)
            .when()
            .get("/")
            .andReturn();
        response.then().assertThat().statusCode(HttpStatus.NOT_FOUND.value())
            .body("message", Matchers.equalTo("Resource not found " + documentId));
    }

    @Test
    public void should_receive_response_for_patch_ttl() throws JSONException {

        JSONObject requestBody = new JSONObject();
        requestBody.put("ttl", "2025-10-31T10:10:10+0000");

        RestAssured.baseURI = caseDocumentAmUrl + "/cases/documents/" + documentId;
        RestAssured.useRelaxedHTTPSValidation();

        Response response = SerenityRest
            .given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .header("ServiceAuthorization", "Bearer " + getServiceAuth())
            .header("Authorization", "Bearer " + userToken)
            .body(requestBody.toString())
            .when()
            .patch("/")
            .andReturn();
        response.then().assertThat().statusCode(HttpStatus.NOT_FOUND.value())
            .body("message", Matchers.equalTo("Resource not found " + documentId));
    }

    @Test
    public void should_receive_response_for_patch_attach_to_document() throws JSONException {

        JSONObject document1 = new JSONObject();
        document1.put("id", documentId);
        document1.put("hashToken", "3aba5fe28560118793e7f086b442ec40f933c8607ac4e32dc4adf865a0be41c2");

        JSONArray documents = new JSONArray();
        documents.put(document1);

        JSONObject requestBody = new JSONObject();
        requestBody.put("caseId", "1234123412341234");
        requestBody.put("documents", documents);

        RestAssured.baseURI = caseDocumentAmUrl + "/cases/documents/" + documentId;
        RestAssured.useRelaxedHTTPSValidation();

        Response response = SerenityRest
            .given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .header("ServiceAuthorization", "Bearer " + getServiceAuth())
            .header("Authorization", "Bearer " + userToken)
            .body(requestBody.toString())
            .when()
            .patch("/")
            .andReturn();
        response.then().assertThat().statusCode(HttpStatus.NOT_FOUND.value())
            .body("message", Matchers.equalTo("Resource not found " + documentId));
    }

    private String getServiceAuth() {
        return new BaseTest().authTokenGenerator(secret, microService,
            generateServiceAuthorisationApi(
                "http://rpe-service-auth-provider-aat.service.core-compute-aat.internal")).generate();
    }
}
