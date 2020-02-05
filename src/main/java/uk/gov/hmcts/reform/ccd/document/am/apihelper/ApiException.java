package uk.gov.hmcts.reform.ccd.document.am.apihelper;

public class ApiException extends Exception {
    private int code;

    public ApiException(int code, String msg) {
        super(msg);
        this.code = code;
    }
}
