package uk.gov.hmcts.reform.ccd.documentam.utils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;

public class KeyGenUtil {

    private static RSAKey rsaJWK;
    private static final String KEY_ID = "23456789";

    private KeyGenUtil() {
    }

    public static RSAKey getRsaJWK() throws JOSEException {
        if (rsaJWK == null) {
            rsaJWK = new RSAKeyGenerator(2048)
                .keyID(KEY_ID)
                .generate();
        }
        return rsaJWK;
    }

}
