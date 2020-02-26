package uk.gov.hmcts.ccd.documentam.befta;

public class TestDataLoaderMain {

    private TestDataLoaderMain() {
    }

    public static void main(String[] args) {
        new CaseDocumentAmTestAutomationAdapter().doLoadTestData();
    }

}
