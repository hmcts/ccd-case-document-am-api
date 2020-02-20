package uk.gov.hmcts.reform.ccd.document.am.befta;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import javax.validation.constraints.NotNull;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.befta.BeftaMain;
import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.data.UserData;
import uk.gov.hmcts.befta.exception.FunctionalTestException;

public class CaseDocumentAmTestAutomationAdapter extends DefaultTestAutomationAdapter {

    private static final Logger logger = LoggerFactory.getLogger(CaseDocumentAmTestAutomationAdapter.class);
    private static final int CREATED = 201;
    private static final String[] TEST_DEFINITIONS_NEEDED_FOR_TA = {
        "src/functionalTest/resources/CCD_BEFTA_JURISDICTION2.xlsx"
    };
    private static final String PUBLIC = "PUBLIC";
    private static final String[][] ccdRolesNeededForTA =
        {
            {"caseworker-befta_jurisdiction_2", PUBLIC},
            {"caseworker-befta_jurisdiction_2-solicitor_1", PUBLIC},
            {"caseworker-befta_jurisdiction_2-solicitor_2", PUBLIC},
            {"caseworker-befta_jurisdiction_2-solicitor_3", PUBLIC},
            {"citizen", "PUBLIC"},
            };


    @Override
    public void doLoadTestData() {
        //addCcdRoles();
        //importDefinitions();
    }

    private void addCcdRoles() {
        logger.info("{} roles will be added to '{}'.", ccdRolesNeededForTA.length,
                    BeftaMain.getConfig().getDefinitionStoreUrl());

        for (String[] roleInfo : ccdRolesNeededForTA) {
            try {
                logger.info("\n\nAdding CCD Role {}, {}...", roleInfo[0], roleInfo[1]);
                addCcdRole(roleInfo[0], roleInfo[1]);
                logger.info("\n\nAdded CCD Role {}, {}...", roleInfo[0], roleInfo[1]);
            } catch (Exception e) {
                logger.info("\n\nCouldn't adding CCD Role {}, {} - Exception: {}.\\n\\n", roleInfo[0], roleInfo[1], e);
            }
        }
    }

    private void addCcdRole(String role, String classification) {
        int divisor = 100;
        int num = 2;
        ConcurrentHashMap<String, String> ccdRoleInfo = new ConcurrentHashMap<>();
        ccdRoleInfo.put("role", role);
        ccdRoleInfo.put("security_classification", classification);
        Response response = asAutoTestImporter().given()
                                                .header("Content-type", "application/json").body(ccdRoleInfo).when()
                                                .put("/api/user-role");


        if (response.getStatusCode() / divisor != num) {
            throw new FunctionalTestException(getResponseMessage(response).toString());
        }
    }

    private StringBuilder getResponseMessage(Response response) {
        return new StringBuilder("Import failed with response body: ").append(response.body().prettyPrint())
                                                                      .append("\nand http code: ").append(response.statusCode());
    }

    private void importDefinitions() {
        logger.info("{} definition files will be uploaded to '{}'.", TEST_DEFINITIONS_NEEDED_FOR_TA.length,
                    BeftaMain.getConfig().getDefinitionStoreUrl());
        for (String fileName : TEST_DEFINITIONS_NEEDED_FOR_TA) {
            try {
                logger.info("\n\nImporting {}...", fileName);
                importDefinition(fileName);
                logger.info("Imported {}.\n\n", fileName);
            } catch (Exception e) {
                logger.info("Couldn't import {} - Exception: {}.\n\n", fileName, e);
            }
        }
    }

    private void importDefinition(String file) {
        Response response = asAutoTestImporter().given().multiPart(new File(file)).when().post("/import");
        if (response.getStatusCode() != CREATED) {
            throw new FunctionalTestException(getResponseMessage(response).toString());
        }
    }

    private RequestSpecification asAutoTestImporter() {
        UserData caseworker = new UserData(BeftaMain.getConfig().getImporterAutoTestEmail(),
                                           BeftaMain.getConfig().getImporterAutoTestPassword());
        authenticate(caseworker);

        String s2sToken = getNewS2SToken();
        return RestAssured
            .given(new RequestSpecBuilder().setBaseUri(BeftaMain.getConfig().getDefinitionStoreUrl())
                                           .build())
            .header("Authorization", "Bearer " + caseworker.getAccessToken())
            .header("ServiceAuthorization", s2sToken);
    }

}
