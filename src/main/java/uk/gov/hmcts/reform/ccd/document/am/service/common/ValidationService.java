package uk.gov.hmcts.reform.ccd.document.am.service.common;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class ValidationService {


    /**
     * Validate a number string using Luhn algorithm.
     *
     * @param numberString =null
     * @return
     */
    public boolean validate(String numberString) {
        if (numberString == null || numberString.length() != 16) {
            return false;
        }
        try {
            Long.parseLong(numberString);
        } catch (NumberFormatException nfe) {
            return false;
        }

        return false;
    }


}
