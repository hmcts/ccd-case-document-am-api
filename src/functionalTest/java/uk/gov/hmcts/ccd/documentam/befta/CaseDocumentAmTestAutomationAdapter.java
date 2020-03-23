package uk.gov.hmcts.ccd.documentam.befta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.hmcts.befta.BeftaMain;
import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.dse.ccd.TestDataLoaderToDefinitionStore;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.EnvironmentVariableUtils;
import uk.gov.hmcts.befta.util.ReflectionUtils;

public class CaseDocumentAmTestAutomationAdapter extends DefaultTestAutomationAdapter {

    private static final Logger logger = LoggerFactory.getLogger(CaseDocumentAmTestAutomationAdapter.class);

    private transient TestDataLoaderToDefinitionStore loader = new TestDataLoaderToDefinitionStore(this);

    @Override
    public void doLoadTestData() {
        loader.addCcdRoles();
        loader.importDefinitions();
    }

    @Override
    public Object calculateCustomValue(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        String docAmUrl = EnvironmentVariableUtils.getRequiredVariable("CASE_DOC_AM_URL");
        if (key.equals("documentIdInTheResponse")) {
            try {
                String href = (String) ReflectionUtils
                    .deepGetFieldInObject(scenarioContext,
                                          "testData.actualResponse.body._embedded.documents[0]._links.self.href");
                return href.substring(href.length() - 36);
            } catch (Exception exception) {
                logger.error("Exception while getting the Document ID from the response", exception.getMessage());
                return "Error extracting the Document Id";
            }
        }
        else if(key.equals(key.equals("S_040_validSelfLink"))) {
            String self = (String) ReflectionUtils
                    .deepGetFieldInObject(scenarioContext,
                            "testData.actualResponse.body.links.self.href");
            if(self!=null && self.startsWith(docAmUrl+"/cases/documents/"))
                return self;
            return "docAmUrl"+"/cases/documents/<a document id>";
            
        }
        else if(key.equals(key.equals("S_040_validBinaryLink"))) {
            String binary = (String) ReflectionUtils
                    .deepGetFieldInObject(scenarioContext,
                            "testData.actualResponse.body.links.binary.href");
            if(binary!=null && binary.startsWith(docAmUrl+"/cases/documents/") && binary.endsWith("/binary"))
                return binary;
            return "docAmUrl"+"/cases/documents/<a document id>/binary";
            
        }
        return super.calculateCustomValue(scenarioContext, key);
    }
}

"href":"{{CASE_DOC_AM_URL}}/cases/documents/${[scenarioContext][childContexts][Default_Document_Upload_Data][customValues][documentIdInTheResponse]}"},"binary":{"href":"{{CASE_DOC_AM_URL}}/cases/documents/${[scenarioContext][childContexts][Default_Document_Upload_Data][customValues][documentIdInTheResponse]}/binary"
