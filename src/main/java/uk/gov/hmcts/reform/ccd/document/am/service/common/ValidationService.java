package uk.gov.hmcts.reform.ccd.document.am.service.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.inject.Named;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.ccd.document.am.controller.advice.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.document.am.model.enums.SecurityClassification;

import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.INPUT_CASE_ID_PATTERN;
import static uk.gov.hmcts.reform.ccd.document.am.apihelper.Constants.INPUT_STRING_PATTERN;

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
        validateInputParams(INPUT_CASE_ID_PATTERN, numberString);
        return (numberString != null && numberString.length() == 16);
    }

    public static void isValidSecurityClassification(String securityClassification) {
        try {
            Enum.valueOf(SecurityClassification.class, securityClassification);
        } catch (final IllegalArgumentException ex) {
            LOG.info("The security classification is not valid");
            throw new BadRequestException("The security classification " + securityClassification + " is not valid");
        }
    }

    public static void validateInputParams(String pattern, String... inputString) {
        for (String input : inputString) {
            if (StringUtils.isEmpty(input)) {
                throw new BadRequestException("The input parameter is Null/Empty");
            } else if (!Pattern.matches(pattern, input)) {
                throw new BadRequestException("The input parameter: \"" + input +  "\", does not comply with the required pattern");
            }
        }
    }

    public static void validateLists(List<?>... inputList) {
        for (List list : inputList) {
            if (CollectionUtils.isEmpty(list)) {
                throw new BadRequestException("The List is empty");
            }
        }
    }

    public static boolean validateTTL(String strDate) {
        if (strDate.length() < 24) {
            return false;
        }
        String timeZone = strDate.substring(20);

        if (timeZone.chars().allMatch(Character::isDigit)) {
            SimpleDateFormat sdfrmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
            sdfrmt.setLenient(false);
            try {
                Date javaDate = sdfrmt.parse(strDate);
                LOG.info("TTL {}", javaDate);
            } catch (ParseException e) {
                return false;
            }
            return true;
        }
        return false;
    }

    public static void validateDocumentId(String documentId) {
        validateInputParams(INPUT_STRING_PATTERN, documentId);
        try {
            UUID uuid = UUID.fromString(documentId);
            LOG.info("UUID {}", uuid);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(String.format("The input parameter: %s is not a valid UUID", documentId));
        }
    }
}
