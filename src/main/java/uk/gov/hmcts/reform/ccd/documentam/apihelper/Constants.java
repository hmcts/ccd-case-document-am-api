package uk.gov.hmcts.reform.ccd.documentam.apihelper;

import java.time.format.DateTimeFormatter;

import static java.time.format.DateTimeFormatter.ofPattern;

public final class Constants {

    public static final String BAD_REQUEST = "Bad Request";
    public static final String RESOURCE_NOT_FOUND = "Resource not found";
    public static final String DOCUMENT_METADATA_NOT_FOUND = "Meta data does not exist for documentId: %s";
    public static final String FORBIDDEN = "Forbidden: Insufficient permissions";
    public static final String APPLICATION_JSON = "application/json";
    public static final String SERVICE_AUTHORIZATION = "serviceauthorization";
    public static final String BEARER = "Bearer ";
    public static final String ORIGINAL_FILE_NAME = "OriginalFileName";
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    public static final String DATA_SOURCE = "data-source";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_LENGTH = "Content-Length";
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
    public static final String EMBEDDED = "_embedded";
    public static final String CREATED_BY = "createdBy";
    public static final String LAST_MODIFIED_BY = "lastModifiedBy";
    public static final String MODIFIED_ON = "modifiedOn";
    public static final String THUMBNAIL = "thumbnail";
    public static final String DOCUMENTS = "documents";
    public static final String MIME_TYPE = "mimeType";
    public static final String ORIGINAL_DOCUMENT_NAME = "originalDocumentName";
    public static final String INPUT_STRING_PATTERN = "^[a-zA-Z0-9_-]*$";
    public static final String INPUT_CASE_ID_PATTERN = "^[0-9]*$";
    public static final String CASE_DOCUMENT_NOT_FOUND = "Case document not found";
    public static final String CASE_DOCUMENT_ID_INVALID = "Case document Id is not valid";
    public static final String CASE_ID_NOT_VALID = "Case ID is not valid";
    public static final String CASE_DOCUMENT_HASH_TOKEN_INVALID = "Case document hash-token is not valid for document"
        + " Id : %s";
    public static final String CASE_TYPE_ID_INVALID = "Case Type ID is not valid";
    public static final String JURISDICTION_ID_INVALID = "Jurisdiction ID is not valid";
    public static final String CLASSIFICATION_ID_INVALID = "Classification is not valid";
    public static final String EXCEPTION_ERROR_ON_DOCUMENT_MESSAGE = "Exception occurred with operation on document "
        + "id: %s";
    public static final String EXCEPTION_ERROR_MESSAGE = "Exception occurred with operation";
    public static final String CASE_ID = "caseId";
    public static final String CASE_TYPE_ID = "caseTypeId";
    public static final String JURISDICTION_ID = "jurisdictionId";
    public static final String USER_PERMISSION_ERROR = "User doesn't have read permission on requested document {}";
    public static final String SERVICE_PERMISSION_ERROR = "Service doesn't have sufficient permission on requested "
        + "API {}";
    public static final String EXCEPTION_SERVICE_ID_NOT_AUTHORISED = "Service Id is not authorized to access API: %s ";

    public static final String CASE_ID_MISSING = "Provide the Case ID";
    public static final String CASE_TYPE_ID_MISSING = "Provide the Case Type ID";
    public static final String JURISDICTION_ID_MISSING = "Provide the Jurisdiction ID";
    public static final String CLASSIFICATION_MISSING = "Provide the Classification";

    public static final String DM_ZONED_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

    public static final DateTimeFormatter DM_DATE_TIME_FORMATTER = ofPattern(DM_ZONED_DATE_TIME_FORMAT);

    private Constants() {
    }
}
