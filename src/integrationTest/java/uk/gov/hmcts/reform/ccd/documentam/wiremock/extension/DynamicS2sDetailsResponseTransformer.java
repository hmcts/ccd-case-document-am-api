package uk.gov.hmcts.reform.ccd.documentam.wiremock.extension;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.BEARER;

/*
 * Replaces response body with service name read from supplied token before returning the response
 */
public class DynamicS2sDetailsResponseTransformer extends AbstractDynamicResponseTransformer {

    static final String DYNAMIC_S2S_DETAILS_RESPONSE_TRANSFORMER = "dynamic-s2s-details-response-transformer";

    @Override
    protected String dynamicResponse(Request request, Response response, Parameters parameters) {
        String serviceName = null;

        String s2sToken = removeBearerFromToken(request.getHeader(AUTHORIZATION));

        if (s2sToken != null) {
            DecodedJWT jwt = JWT.decode(s2sToken);
            if (jwt.getExpiresAt().before(new Date())) {
                throw new SecurityException();
            }
            serviceName =  jwt.getSubject();
        }

        return serviceName;
    }

    @Override
    public String getName() {
        return DYNAMIC_S2S_DETAILS_RESPONSE_TRANSFORMER;
    }

    private String removeBearerFromToken(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        return token.startsWith(BEARER) ? token.substring(BEARER.length()) : token;
    }

}
