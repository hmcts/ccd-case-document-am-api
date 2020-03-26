package uk.gov.hmcts.reform.ccd.document.am;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.befta.BeftaMain;
import uk.gov.hmcts.befta.auth.AuthApi;
import uk.gov.hmcts.befta.auth.OAuth2;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.ServiceAuthTokenGenerator;

import java.util.Base64;

@Service
public class BaseTest {

    private static final String AUTHORIZATION_CODE = "authorization_code";
    private static final String CODE = "code";
    private static final String BASIC = "Basic ";


    private static  AuthApi idamApi  = Feign.builder().encoder(new JacksonEncoder()).decoder(new JacksonDecoder()).target(AuthApi.class, BeftaMain.getConfig().getIdamURL());


    public ServiceAuthorisationApi generateServiceAuthorisationApi(@Value("${idam.s2s-auth.url}") final String s2sUrl) {
        return Feign.builder()
                    .encoder(new JacksonEncoder())
                    .contract(new SpringMvcContract())
                    .target(ServiceAuthorisationApi.class, s2sUrl);
    }

    public ServiceAuthTokenGenerator authTokenGenerator(
        @Value("${idam.s2s-auth.totp_secret}") final String secret,
        @Value("${idam.s2s-auth.microservice}") final String microService,
        final ServiceAuthorisationApi serviceAuthorisationApi
                                                       ) {
        return new ServiceAuthTokenGenerator(secret, microService, serviceAuthorisationApi);
    }

    public static String getIdamOauth2Token(String username, String password) {
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
