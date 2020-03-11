package uk.gov.hmcts.reform.ccd.document.am.service.common;

import java.util.regex.Pattern;
import javax.inject.Named;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.SecurityClassification;

@Named
@Singleton
@Slf4j
public class ValidationService {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationService.class);

    private ValidationService() {
    }

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

    public static void isValidSecurityClassification(String securityClassification) throws IllegalArgumentException {
        try {
            Enum.valueOf(SecurityClassification.class, securityClassification);
        } catch (final IllegalArgumentException ex) {
            LOG.info("The security classification is not valid");
            throw new IllegalArgumentException("The security classification" + securityClassification + " is not valid");
        }
    }

    public static void validateInputs(String pattern, String... inputString) {
        for (String input : inputString) {
            if (StringUtils.isNotEmpty(input) && !Pattern.matches(pattern, input)) {
                throw new IllegalArgumentException("The input parameter "
                                                   + input + " does not complies with the required pattern");
            }
        }
    }
}
