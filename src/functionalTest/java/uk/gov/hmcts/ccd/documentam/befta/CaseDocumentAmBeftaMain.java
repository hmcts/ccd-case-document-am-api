package uk.gov.hmcts.ccd.documentam.befta;

import uk.gov.hmcts.befta.BeftaMain;

public class CaseDocumentAmBeftaMain {

    private CaseDocumentAmBeftaMain() {
    }

    public static void main(String[] args) {
        BeftaMain.main(args, new CaseDocumentAmTestAutomationAdapter());
    }
}
