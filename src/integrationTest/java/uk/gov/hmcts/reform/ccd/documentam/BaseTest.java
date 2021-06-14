package uk.gov.hmcts.reform.ccd.documentam;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.TextCodec;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.hmcts.reform.ccd.documentam.auditlog.AuditEntry;
import uk.gov.hmcts.reform.ccd.documentam.auditlog.AuditOperationType;
import uk.gov.hmcts.reform.ccd.documentam.auditlog.AuditRepository;
import uk.gov.hmcts.reform.ccd.documentam.configuration.AuditConfiguration;
import uk.gov.hmcts.reform.ccd.documentam.utils.KeyGenUtil;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.ACCESS_TOKEN;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static uk.gov.hmcts.reform.ccd.documentam.fixtures.WiremockFixtures.BEARER;
import static uk.gov.hmcts.reform.ccd.documentam.security.JwtGrantedAuthoritiesConverter.TOKEN_NAME;

@SpringBootTest(classes = {
    Application.class,
    TestIdamConfiguration.class
})
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0, stubs = "classpath:/wiremock-stubs")
@ActiveProfiles("itest")
public class BaseTest {

    public static final long AUTH_TOKEN_TTL = 14400000;

    public static final String IDAM_MOCK_USER_ID = "445";
    private static final String EXAMPLE_REQUEST_ID = "TEST REQUEST ID";

    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";


    @SpyBean
    @Inject
    protected AuditRepository auditRepository;

//    @Value("${wiremock.server.port}")
//    protected Integer wiremockPort;
//    @Mock
//    protected Authentication authentication;

//    @BeforeEach
//    void init() {
//        Jwt jwt = dummyJwt();
//        when(authentication.getPrincipal()).thenReturn(jwt);
//        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
//    }
//
//    private Jwt dummyJwt() {
//        return Jwt.withTokenValue("a dummy jwt token")
//                .claim("aClaim", "aClaim")
//                .header("aHeader", "aHeader")
//                .build();
//    }

    public static HttpHeaders createHttpHeaders(String serviceName) throws JOSEException {
        return createHttpHeaders(AUTH_TOKEN_TTL, serviceName, AUTH_TOKEN_TTL);
    }

    protected static HttpHeaders createHttpHeaders(long authTtlMillis,
                                            String serviceName,
                                            long s2sAuthTtlMillis)  throws JOSEException {
        HttpHeaders headers = new HttpHeaders();
        String authToken = BEARER + generateAuthToken(authTtlMillis);
        headers.add(AUTHORIZATION, authToken);
        String s2SToken = generateS2SToken(serviceName, s2sAuthTtlMillis);
        headers.add(SERVICE_AUTHORIZATION, s2SToken);
        headers.add(AuditConfiguration.REQUEST_ID, EXAMPLE_REQUEST_ID);
        return headers;
    }

    protected ResultMatcher hasGeneratedLogAudit(AuditOperationType operationType,
                                                 String invokingService,
                                                 List<String> documentIds,
                                                 String caseId) {
        return result -> verifyLogAuditValues(result,
                                              operationType,
                                              invokingService,
                                              documentIds,
                                              caseId);
    }

    protected void verifyLogAuditValues(MvcResult result,
                                        AuditOperationType operationType,
                                        String invokingService,
                                        List<String> documentIds,
                                        String caseId) {
        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        AuditEntry auditEntry = captor.getValue();

        assertNotNull("DateTime", auditEntry.getDateTime());

        assertEquals("Operation Type", operationType.getLabel(), auditEntry.getOperationType());

        assertEquals("Idam ID", IDAM_MOCK_USER_ID, auditEntry.getIdamId());
        assertEquals("Invoking Service", invokingService, auditEntry.getInvokingService());

        assertEquals("HTTP Status", result.getResponse().getStatus(), auditEntry.getHttpStatus());
        assertEquals("HTTP Method", result.getRequest().getMethod(), auditEntry.getHttpMethod());
        assertEquals("Request Path", result.getRequest().getRequestURI(), auditEntry.getRequestPath());
        assertEquals("Request ID", EXAMPLE_REQUEST_ID, auditEntry.getRequestId());

        // NB: skip validation of inputs for BAD_REQUEST as some may not have been populated
        if (result.getResponse().getStatus() != HttpStatus.BAD_REQUEST.value()) {
            if (documentIds != null && !documentIds.isEmpty()) {
                assertThat(auditEntry.getDocumentIds())
                    .isNotNull()
                    .hasSize(documentIds.size())
                    .containsAll(documentIds);
            } else {
                assertThat(auditEntry.getDocumentIds()).isNullOrEmpty();
            }

            if (caseId != null && !caseId.equals("")) {
                assertThat(auditEntry.getCaseId())
                    .isNotNull();
            } else {
                assertThat(auditEntry.getCaseId()).isNullOrEmpty();
            }
        }
    }

    private static String generateAuthToken(long ttlMillis) throws JOSEException {

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
            .subject("API_Stub")
            .issueTime(new Date())
            .claim(TOKEN_NAME, ACCESS_TOKEN)
            .expirationTime(new Date(System.currentTimeMillis() + ttlMillis));

        SignedJWT signedJWT = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(KeyGenUtil.getRsaJWK().getKeyID()).build(),
            builder.build()
        );
        signedJWT.sign(new RSASSASigner(KeyGenUtil.getRsaJWK()));

        return signedJWT.serialize();
    }

    private static String generateS2SToken(String serviceName, long ttlMillis) {
        return Jwts.builder()
            .setSubject(serviceName)
            .setExpiration(new Date(System.currentTimeMillis() + ttlMillis))
            .signWith(SignatureAlgorithm.HS256, TextCodec.BASE64.encode("AA"))
            .compact();
    }

}
