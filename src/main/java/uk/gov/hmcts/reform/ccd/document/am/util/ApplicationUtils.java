package uk.gov.hmcts.reform.ccd.document.am.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationUtils {

    private ApplicationUtils() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationUtils.class);

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
            StringBuilder hashtext = new StringBuilder();
            hashtext.append(no.toString(16));

            // Add preceding 0s to make it 32 bit
            while (hashtext.length() < 32) {
                hashtext.append("0").append(hashtext);
            }

            // return the HashText
            return hashtext.toString();
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Error while generating the hashcode :{}", e.getMessage());
        }
        return null;
    }

}
