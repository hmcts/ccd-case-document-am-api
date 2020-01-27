package uk.gov.hmcts.reform.ccd.document.am.befta;

import uk.gov.hmcts.befta.BeftaMain;

public class CaseDocumentAmBeftaMain extends BeftaMain {

    public static void main(String[] args) {
        setTaAdapter(new CaseDocumentAmTestAutomationAdapter());
        BeftaMain.main(args);
    }

}
