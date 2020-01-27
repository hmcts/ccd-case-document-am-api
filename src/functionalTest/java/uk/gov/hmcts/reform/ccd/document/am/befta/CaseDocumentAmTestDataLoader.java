package uk.gov.hmcts.reform.ccd.document.am.befta;

@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class CaseDocumentAmTestDataLoader {

    public static void main(String[] args) {
        new CaseDocumentAmTestAutomationAdapter().loadTestDataIfNecessary();
    }

}
