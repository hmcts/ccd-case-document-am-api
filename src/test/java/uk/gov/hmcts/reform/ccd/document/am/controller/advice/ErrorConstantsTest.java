package uk.gov.hmcts.reform.ccd.document.am.controller.advice;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.ccd.document.am.controller.advice.ErrorConstants.*;

class ErrorConstantsTest {

    @Test
    void getErrorCode() {
        assertEquals(MALFORMED_JSON.getErrorCode(),1);
        assertEquals(UNSUPPORTED_MEDIA_TYPES.getErrorCode(),2);
        assertEquals(INVALID_REQUEST.getErrorCode(),3);
        assertEquals(RESOURCE_NOT_FOUND.getErrorCode(),4);
        assertEquals(METHOD_ARG_NOT_VALID.getErrorCode(),5);
        assertEquals(DATA_INTEGRITY_VIOLATION.getErrorCode(),6);
        assertEquals(ILLEGAL_ARGUMENT.getErrorCode(),7);
        assertEquals(UNKNOWN_EXCEPTION.getErrorCode(),8);
        assertEquals(OK.getErrorCode(),200);
        assertEquals(CREATED.getErrorCode(),201);
        assertEquals(ACCEPTED.getErrorCode(),202);
        assertEquals(NON_AUTHORITATIVE_INFORMATION.getErrorCode(),203);
        assertEquals(NO_CONTENT.getErrorCode(),204);
        assertEquals(MOVED_PERMANENTLY.getErrorCode(),301);
        assertEquals(FOUND.getErrorCode(),302);
        assertEquals(BAD_REQUEST.getErrorCode(),400);
        assertEquals(UNAUTHORIZED.getErrorCode(),401);
        assertEquals(PAYMENT_REQUIRED.getErrorCode(),402);
        assertEquals(ACCESS_DENIED.getErrorCode(),403);
        assertEquals(NOT_FOUND.getErrorCode(),404);
        assertEquals(METHOD_NOT_ALLOWED.getErrorCode(),405);
        assertEquals(NOT_ACCEPTABLE.getErrorCode(),406);
        assertEquals(INTERNAL_SERVER_ERROR.getErrorCode(),500);
        assertEquals(BAD_GATEWAY.getErrorCode(),502);
        assertEquals(SERVICE_UNAVAILABLE.getErrorCode(),503);
        assertEquals(GATEWAY_TIMEOUT.getErrorCode(),504);
    }

    @Test
    void getErrorMessage() {
        assertEquals(MALFORMED_JSON.getErrorMessage(),"Malformed Input Request");
        assertEquals(UNSUPPORTED_MEDIA_TYPES.getErrorMessage(),"Unsupported Media Type");
        assertEquals(INVALID_REQUEST.getErrorMessage(),"There is a problem with your request. Please check and try again");
        assertEquals(RESOURCE_NOT_FOUND.getErrorMessage(),"Resource not found");
        assertEquals(METHOD_ARG_NOT_VALID.getErrorMessage(),"validation on an argument failed");
        assertEquals(DATA_INTEGRITY_VIOLATION.getErrorMessage(),"attempt to insert or update data resulted in violation of an integrity constraint");
        assertEquals(ILLEGAL_ARGUMENT.getErrorMessage(),"method has been passed an illegal or inappropriate argument");
        assertEquals(UNKNOWN_EXCEPTION.getErrorMessage(),"error was caused by an unknown exception");
        assertEquals(OK.getErrorMessage(),"OK");
        assertEquals(CREATED.getErrorMessage(),"Created");
        assertEquals(ACCEPTED.getErrorMessage(),"Accepted");
        assertEquals(NON_AUTHORITATIVE_INFORMATION.getErrorMessage(),"Non-Authoritative Information");
        assertEquals(NO_CONTENT.getErrorMessage(),"No Content");
        assertEquals(MOVED_PERMANENTLY.getErrorMessage(),"Moved Permanently");
        assertEquals(FOUND.getErrorMessage(),"Found");
        assertEquals(BAD_REQUEST.getErrorMessage(),"Bad Request");
        assertEquals(UNAUTHORIZED.getErrorMessage(),"Unauthorized");
        assertEquals(PAYMENT_REQUIRED.getErrorMessage(),"Payment Required");
        assertEquals(ACCESS_DENIED.getErrorMessage(),"Forbidden");
        assertEquals(NOT_FOUND.getErrorMessage(),"Not Found");
        assertEquals(METHOD_NOT_ALLOWED.getErrorMessage(),"Method Not Allowed");
        assertEquals(NOT_ACCEPTABLE.getErrorMessage(),"Not Acceptable");
        assertEquals(INTERNAL_SERVER_ERROR.getErrorMessage(),"Internal Server Error");
        assertEquals(BAD_GATEWAY.getErrorMessage(),"Bad Gateway");
        assertEquals(SERVICE_UNAVAILABLE.getErrorMessage(),"Service Unavailable");
        assertEquals(GATEWAY_TIMEOUT.getErrorMessage(),"Gateway Timeout");
    }
}
