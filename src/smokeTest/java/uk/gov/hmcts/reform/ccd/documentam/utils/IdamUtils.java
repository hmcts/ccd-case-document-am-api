package uk.gov.hmcts.reform.ccd.documentam.utils;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Base64;

import uk.gov.hmcts.befta.BeftaMain;
import uk.gov.hmcts.befta.auth.AuthApi;
import uk.gov.hmcts.befta.auth.OAuth2;

@Service
public class IdamUtils {

    private static final String AUTHORIZATION_CODE = "authorization_code";
    private static final String CODE = "code";
    private static final String BASIC = "Basic ";

    private final AuthApi idamApi;

    @Autowired
    public IdamUtils() {
        idamApi = Feign.builder().encoder(new JacksonEncoder()).decoder(new JacksonDecoder()).target(AuthApi.class, BeftaMain.getConfig().getIdamURL());

    }

    public  String getIdamOauth2Token(String username, String password) {
        String authorisation = username + ":" + password;
        String base64Authorisation = Base64.getEncoder().encodeToString(authorisation.getBytes());

        OAuth2 oauth2 = BeftaMain.getConfig().getOauth2Config();
        AuthApi.AuthenticateUserResponse authenticateUserResponse = idamApi
            .authenticateUser(BASIC + base64Authorisation, CODE, oauth2.getClientId(), oauth2.getRedirectUri());

        AuthApi.TokenExchangeResponse tokenExchangeResponse = idamApi.exchangeCode(authenticateUserResponse.getCode(),
                                                        AUTHORIZATION_CODE, oauth2.getClientId(), oauth2.getClientSecret(), oauth2.getRedirectUri());

        return tokenExchangeResponse.getAccessToken();
    }
}
