package uk.gov.hmcts.reform.ccd.document.am.apihelper;

public class Constants {

    private Constants() {
    }

    public static final String TAG = "case-document-controller";
    public static final String BAD_REQUEST = "Bad Request";
    public static final String UNAUTHORIZED = "Unauthorized";
    public static final String RESOURCE_NOT_FOUND = "Resource not found";
    public static final String FORBIDDEN = "Forbidden";
    public static final String APPLICATION_JSON = "application/json";
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    public static final String S2S_API_PARAM = "Service Auth (S2S). Use it when accessing the API on App Tier level.";
    public static final String ORIGINAL_FILE_NAME = "OriginalFileName";
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    public static final String DATA_SOURCE = "data-source";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_LENGTH = "Content-Length";
}
