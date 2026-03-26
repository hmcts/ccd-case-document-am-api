package uk.gov.hmcts.ccd.documentam.befta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import uk.gov.hmcts.befta.auth.AuthApi;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class JwtIssuerVerificationApp {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String AUTHORIZATION_CODE = "authorization_code";
    private static final String CODE = "code";
    private static final String BASIC = "Basic ";

    private JwtIssuerVerificationApp() {
    }

    public static void main(String[] args) throws Exception {
        String expectedIssuer = requireEnv("OIDC_ISSUER");
        String actualIssuer = decodeIssuer(fetchAccessToken());

        if (!expectedIssuer.equals(actualIssuer)) {
            throw new IllegalStateException(
                "OIDC_ISSUER mismatch: expected `" + expectedIssuer + "` but token iss was `" + actualIssuer + "`"
            );
        }

        System.out.println("Verified OIDC_ISSUER matches functional test token iss: " + actualIssuer);
    }

    private static String fetchAccessToken() {
        AuthApi authApi = Feign.builder()
            .encoder(new JacksonEncoder())
            .decoder(new JacksonDecoder())
            .target(AuthApi.class, firstAvailableEnv("IDAM_API_URL_BASE", "IDAM_URL"));

        String[] credentials = firstAvailableCredentials(
            "CCD_CASEWORKER_AUTOTEST_EMAIL", "CCD_CASEWORKER_AUTOTEST_PASSWORD",
            "DEFINITION_IMPORTER_USERNAME", "DEFINITION_IMPORTER_PASSWORD"
        );
        String clientId = firstAvailableEnv("CCD_API_GATEWAY_OAUTH2_CLIENT_ID", "OAUTH2_CLIENT_ID");
        String clientSecret = firstAvailableEnv("CCD_API_GATEWAY_OAUTH2_CLIENT_SECRET", "OAUTH2_CLIENT_SECRET");
        String redirectUri = firstAvailableEnv("CCD_API_GATEWAY_OAUTH2_REDIRECT_URL", "OAUTH2_REDIRECT_URI");

        String basicAuthorisation = BASIC + Base64.getEncoder()
            .encodeToString((credentials[0] + ":" + credentials[1]).getBytes(StandardCharsets.UTF_8));

        AuthApi.AuthenticateUserResponse authenticateUserResponse = authApi.authenticateUser(
            basicAuthorisation,
            CODE,
            clientId,
            redirectUri
        );

        AuthApi.TokenExchangeResponse tokenExchangeResponse = authApi.exchangeCode(
            authenticateUserResponse.getCode(),
            AUTHORIZATION_CODE,
            clientId,
            clientSecret,
            redirectUri
        );

        return tokenExchangeResponse.getAccessToken();
    }

    private static String[] firstAvailableCredentials(String... envNames) {
        for (int index = 0; index < envNames.length; index += 2) {
            String username = System.getenv(envNames[index]);
            String password = System.getenv(envNames[index + 1]);
            if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
                return new String[]{username, password};
            }
        }

        throw new IllegalStateException(
            "No credentials available for JWT issuer verification. "
                + "Expected one of: CCD_CASEWORKER_AUTOTEST_EMAIL/PASSWORD or "
                + "DEFINITION_IMPORTER_USERNAME/PASSWORD"
        );
    }

    private static String decodeIssuer(String accessToken) throws Exception {
        String[] parts = accessToken.split("\\.");
        if (parts.length < 2) {
            throw new IllegalStateException("Access token is not a JWT");
        }

        byte[] decodedPayload = Base64.getUrlDecoder().decode(padBase64(parts[1]));
        JsonNode payload = OBJECT_MAPPER.readTree(new String(decodedPayload, StandardCharsets.UTF_8));
        JsonNode issuer = payload.get("iss");
        if (issuer == null || issuer.isNull() || issuer.asText().isBlank()) {
            throw new IllegalStateException("Access token does not contain an iss claim");
        }
        return issuer.asText();
    }

    private static String padBase64(String value) {
        int remainder = value.length() % 4;
        return remainder == 0 ? value : value + "=".repeat(4 - remainder);
    }

    private static String firstAvailableEnv(String... names) {
        for (String name : names) {
            String value = System.getenv(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        throw new IllegalStateException("Missing required environment variable. Checked: " + String.join(", ", names));
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }
}
