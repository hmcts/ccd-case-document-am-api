package uk.gov.hmcts.reform.ccd.documentam.wiremock.extension;

import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.nimbusds.jose.JOSEException;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.ccd.documentam.utils.KeyGenUtil;

/*
 * Replaces response body with the OAuth JWK Set, i.e. the public keys used to sign the mock OAAuth token
 */
@Slf4j
public class DynamicOAuthJwkSetResponseTransformer extends AbstractDynamicResponseTransformer {

    static final String DYNAMIC_OAUTH_JWK_SET_RESPONSE_TRANSFORMER = "dynamic-oauth-jwk-set-response-transformer";

    @Override
    protected String dynamicResponse(Request request, Response response, Parameters parameters) {

        try {

            String json = "{"
                + "   \"keys\": [" +  KeyGenUtil.getRsaJWK().toJSONObject() + "]"
                + "}";

            return json;

        } catch (JOSEException ex) {
            log.error("Failure running RSA JWK Generator", ex);
            return null;
        }

    }

    @Override
    public String getName() {
        return DYNAMIC_OAUTH_JWK_SET_RESPONSE_TRANSFORMER;
    }

}
