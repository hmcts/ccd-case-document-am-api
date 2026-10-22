package uk.gov.hmcts.reform.ccd.documentam.logging;

import jakarta.servlet.http.HttpServletRequest;

/** Avoids logging the same failure again when MVC falls through to the security filter. */
public final class SafeFailureLogging {
    private static final String LOGGED = SafeFailureLogging.class.getName() + ".logged";

    private SafeFailureLogging() {
    }

    public static boolean firstFailure(HttpServletRequest request) {
        if (Boolean.TRUE.equals(request.getAttribute(LOGGED))) {
            return false;
        }
        request.setAttribute(LOGGED, true);
        return true;
    }
}
