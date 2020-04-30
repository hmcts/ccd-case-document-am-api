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
    public static final String SERVICE_AUTHORIZATION2 = "ServiceAuthorization";
    public static final String BEARER = "Bearer ";
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
    public static final String USERID = "user-id";
    public static final String FILES = "files";
    public static final String LINKS = "_links";
    public static final String HREF = "href";
    public static final String SELF = "self";
    public static final String BINARY = "binary";
    public static final String HASHTOKEN = "hashToken";
    public static final String TEST_URL = "TEST_URL";
    public static final String EMBEDDED = "_embedded";
    public static final String CREATED_BY = "createdBy";
    public static final String LAST_MODIFIED_BY = "lastModifiedBy";
    public static final String MODIFIED_ON = "modifiedOn";
    public static final String THUMBNAIL = "thumbnail";
    public static final String DOCUMENTS = "documents";
    public static final String INPUT_STRING_PATTERN = "^[a-zA-Z0-9_-]*$";
    public static final String INPUT_CASE_ID_PATTERN = "^[0-9]*$";
    public static final String CASE_DOCUMENT_NOT_FOUND = "Case document not found";
    public static final String CASE_DOCUMENT_ID_INVALID = "Case document Id is not valid";
    public static final String CASE_ID_NOT_VALID = "Case Id is not valid";
    public static final String CASE_DOCUMENT_HASH_TOKEN_INVALID = "Case document hash-token is not valid for document Id : %s";
    public static final String CASE_TYPE_ID_INVALID = "Case Type Id Id is not valid";
    public static final String JURISDICTION_ID_INVALID = "Jurisdiction Id is not valid";
    public static final String CLASSIFICATION_ID_INVALID = "Jurisdiction Id is not valid";
    public static final String EXCEPTION_ERROR_ON_DOCUMENT_MESSAGE = "Exception occurred with operation on document id: %s because of %s";
    public static final String EXCEPTION_ERROR_MESSAGE = "Exception occurred with operation because of %s";
    public static final String CASE_ID = "caseId";
    public static final String CASE_TYPE_ID = "caseTypeId";
    public static final String JURISDICTION_ID = "jurisdictionId";
    public static final String SERVICE_PERMISSION_ERROR = "Service doesn't have sufficient permission on requested API {}";
    public static final String SERVICES = "services";
    public static final String XUI_WEBAPP = "xui_webapp";
    public static final String BULK_SCAN_PROCESSOR = "bulk_scan_processor";
    public static final String CCD_DATA = "ccd_data";
    public static final String SIZE = "size";
    public static final String METADATA = "metadata";
    public static final String ROLES = "roles";
    public static final String TTL = "ttl";
    public static final String CREATED_ON = "createdOn";
    public static final String DOCUMENT_LINKS = "links";


}
