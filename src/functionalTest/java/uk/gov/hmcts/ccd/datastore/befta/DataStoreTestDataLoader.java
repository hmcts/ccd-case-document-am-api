package uk.gov.hmcts.ccd.datastore.befta;

@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class DataStoreTestDataLoader {

    public static void main(String[] args) {
        new DataStoreTestAutomationAdapter().loadTestDataIfNecessary();
    }

}
