package uk.gov.hmcts.reform.ccd.document.am.service.common;

import javax.inject.Named;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.SecurityClassification;
import uk.gov.hmcts.reform.ccd.document.am.service.impl.CaseDataStoreServiceImpl;

@Named
@Singleton
@Slf4j
public class ValidationService {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationService.class);
    /**
     * Validate a number string using  algorithm.
     *
     * @param numberString =null
     * @return
     */
    public static boolean validate(String numberString) {
        if (numberString == null || numberString.length() != 16) {
            return false;
        }

        return true;
    }

    public static boolean isValidSecurityClassification(String securityClassification) throws IllegalArgumentException{
        try {
            Enum.valueOf(SecurityClassification.class, securityClassification);
            return true;
        } catch (final IllegalArgumentException ex) {
            LOG.info("The security classification %s is not valid" , securityClassification );
            throw ex;
        }
    }
}
