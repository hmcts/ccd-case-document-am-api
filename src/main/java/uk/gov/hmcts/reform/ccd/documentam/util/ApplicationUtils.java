package uk.gov.hmcts.reform.ccd.documentam.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApplicationUtils {

    private ApplicationUtils() {
    }

    public static String generateHashCode(String input) {
        try {
            // getInstance() method is called with algorithm SHA-512
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // digest() method is called
            // to calculate message digest of the input string
            // returned as array of byte
            byte[] messageDigest = md.digest(input.getBytes());

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            StringBuilder hashText = new StringBuilder();
            hashText.append(no.toString(16));

            // Add preceding 0s to make it 32 bit
            while (hashText.length() < 32) {
                hashText.append("0").append(hashText);
            }

            return hashText.toString();
        } catch (NoSuchAlgorithmException exception) {
            log.error("Error while generating the hashcode :", exception);
        }
        return null;
    }

}
