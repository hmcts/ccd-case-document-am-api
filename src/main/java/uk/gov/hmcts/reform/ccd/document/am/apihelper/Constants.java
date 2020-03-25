package uk.gov.hmcts.reform.ccd.document.am.apihelper;

public class Constants {

    private Constants() {
    }

    public static final String TAG = "case-document-controller";
    public static final String BAD_REQUEST = "Bad Request";
    public static final String UNAUTHORIZED = "Unauthorized";
    public static final String RESOURCE_NOT_FOUND = "Resource not found";
    public static final String FORBIDDEN = "Forbidden: Insufficient permissions";
    public static final String APPLICATION_JSON = "application/json";
    public static final String SERVICE_AUTHORIZATION = "serviceauthorization";
    public static final String S2S_API_PARAM = "Service Auth (S2S). Use it when accessing the API on App Tier level.";
    public static final String AUTHORIZATION = "Authorization";
    public static final String AUTHORIZATION_DESCRIPTION = "Authorization (user-token) of the IDAM user.";
    public static final String ORIGINAL_FILE_NAME = "OriginalFileName";
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    public static final String DATA_SOURCE = "data-source";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CASE_ID_INVALID = "The case reference for requested document is not valid";
    public static final String INPUT_INVALID = "The case reference or document id is not valid";
    public static final String INSUFFICIENT_PERMISSION = "Insufficient permission on requested  document";
    public static final String CLASSIFICATION = "classification";
    public static final String ROLES = "roles";
    public static final String USERID = "user-id";
    public static final String USER_ROLES = "user-roles";
    public static final String FILES = "files";
    public static final String LINKS = "_links";
    public static final String HREF = "href";
    public static final String SELF = "self";
    public static final String BINARY = "binary";
    public static final String HASHCODE = "hashcode";
    public static final String TEST_URL = "TEST_URL";
    public static final String EMBEDDED = "_embedded";
    public static final String THUMBNAIL = "thumbnail";
    public static final String DOCUMENTS = "documents";
    public static final String INPUT_STRING_PATTERN = "^[a-zA-Z0-9_-]*$";

}
