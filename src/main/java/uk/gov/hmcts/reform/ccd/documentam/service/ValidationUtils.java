package uk.gov.hmcts.reform.ccd.documentam.service;

import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.INPUT_CASE_ID_PATTERN;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.INPUT_STRING_PATTERN;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.inject.Named;
import javax.inject.Singleton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.ccd.documentam.exception.BadRequestException;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Classification;

@Named
@Singleton
@Slf4j
public class ValidationUtils {

    /**
     * Validate a number string using  algorithm.
     *
     * @param numberString =null
     * @return
     */
    public boolean validate(String numberString) {
        validateInputParams(INPUT_CASE_ID_PATTERN, numberString);
        return (numberString != null && numberString.length() == 16);
    }

    public void isValidSecurityClassification(String securityClassification) {
        try {
            Enum.valueOf(Classification.class, securityClassification);
        } catch (final IllegalArgumentException ex) {
            log.info("The security classification is not valid");
            throw new BadRequestException("The security classification " + securityClassification + " is not valid");
        }
    }

    public void validateInputParams(String pattern, String... inputString) {
        for (String input : inputString) {
            if (StringUtils.isEmpty(input)) {
                throw new BadRequestException("The input parameter is Null/Empty");
            } else if (!Pattern.matches(pattern, input)) {
                throw new BadRequestException("The input parameter: \"" + input +  "\", does not comply with the required pattern");
            }
        }
    }

    public void validateLists(List<?>... inputLists) {
        for (List<?> list : inputLists) {
            if (CollectionUtils.isEmpty(list)) {
                throw new BadRequestException("The List is empty");
            }
        }
    }

    public boolean validateTTL(String strDate) {
        if (strDate.length() < 24) {
            return false;
        }
        String timeZone = strDate.substring(20);

        if (timeZone.chars().allMatch(Character::isDigit)) {
            SimpleDateFormat sdfrmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
            sdfrmt.setLenient(false);
            try {
                Date javaDate = sdfrmt.parse(strDate);
                log.info("TTL {}", javaDate);
            } catch (ParseException e) {
                return false;
            }
            return true;
        }
        return false;
    }

    public void validateDocumentId(String documentId) {
        validateInputParams(INPUT_STRING_PATTERN, documentId);
        try {
            UUID uuid = UUID.fromString(documentId);
            log.info("UUID {}", uuid);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(String.format(
                "The input parameter: %s is not a valid UUID.",
                documentId), exception);
        }
    }
}
