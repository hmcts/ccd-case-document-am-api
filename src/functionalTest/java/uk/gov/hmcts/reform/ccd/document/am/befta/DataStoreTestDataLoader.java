package uk.gov.hmcts.reform.ccd.document.am.befta;

@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class DataStoreTestDataLoader {

    public static void main(String[] args) {
        new DataStoreTestAutomationAdapter().loadTestDataIfNecessary();
    }

}
