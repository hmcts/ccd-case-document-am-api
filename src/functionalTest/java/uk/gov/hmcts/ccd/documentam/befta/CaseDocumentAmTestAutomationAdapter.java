package uk.gov.hmcts.ccd.documentam.befta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.dse.ccd.TestDataLoaderToDefinitionStore;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
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
        return super.calculateCustomValue(scenarioContext, key);
    }
}
