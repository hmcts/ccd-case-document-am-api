package uk.gov.hmcts.reform.ccd.document.am.service.common;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class ValidationService {


    /**
     * Validate a number string using  algorithm.
     *
     * @param numberString =null
     * @return
     */
    public boolean validate(String numberString) {
        if (numberString == null || numberString.length() != 16) {
            return false;
        }

        return true;
    }


}
