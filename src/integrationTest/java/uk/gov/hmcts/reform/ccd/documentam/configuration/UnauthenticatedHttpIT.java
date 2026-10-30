package uk.gov.hmcts.reform.ccd.documentam.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import uk.gov.hmcts.reform.ccd.documentam.Application;
import uk.gov.hmcts.reform.ccd.documentam.BaseTest;

import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {Application.class, UnauthenticatedHttpIT.ProbeConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UnauthenticatedHttpIT extends BaseTest {

    private static final String DIAGNOSTIC = "PRIVATE_HEALTH_DIAGNOSTIC";
    private static final String QUERIES = "?trace=true&message=true&errors=true";

    @LocalServerPort
    private int port;

    @Test
    void protectedRoutesRejectAnonymousJsonAndHtmlRequestsWithoutDiagnostics() throws Exception {
        for (String accept : new String[] {"application/json", "text/html"}) {
            HttpResponse<String> response = request("/cases/documents/private-document-id" + QUERIES, accept);
            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(response.body()).isEmpty();
            assertThat(response.headers().firstValue("WWW-Authenticate")).isEmpty();
        }
    }

    @Test
    void publicHealthHidesFailureDetailsWithoutAuthentication() throws Exception {
        HttpResponse<String> response = request("/health" + QUERIES, "application/json");
        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.body()).contains("DOWN")
            .doesNotContain(DIAGNOSTIC, "exception", "details", "components", "trace", "localhost");
    }

    @Test
    void publicUnknownResourceErrorsHideDiagnosticsForJsonAndHtml() throws Exception {
        for (String accept : new String[] {"application/json", "text/html"}) {
            HttpResponse<String> response = request("/favicon.ico" + QUERIES, accept);
            // HTML cannot render the advice body; the original secured error dispatch returns 401.
            assertThat(response.statusCode()).isEqualTo(accept.equals("application/json") ? 500 : 401);
            if (accept.equals("text/html")) {
                assertThat(response.body()).isEmpty();
            }
            assertThat(response.body()).doesNotContain("Exception", "trace", "stack", "localhost", "Tomcat");
        }
    }

    @Test
    void containerRejectsMalformedAnonymousRequestsWithoutReportingInternals() throws Exception {
        try (Socket socket = new Socket("localhost", port)) {
            socket.setSoTimeout(10000);
            socket.getOutputStream().write(("GET /<PRIVATE_IDENTIFIER HTTP/1.1\r\nHost: localhost\r\n"
                + "Connection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            String response = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(response).startsWith("HTTP/1.1 400");
            assertThat(response).doesNotContain("PRIVATE_IDENTIFIER", "Exception", "org.apache", "Tomcat",
                "stacktrace", "java.lang");
        }
    }

    private HttpResponse<String> request(String path, String accept) throws Exception {
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10)).header("Accept", accept).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        }
    }

    @TestConfiguration
    static class ProbeConfiguration {
        @Bean
        HealthIndicator anonymousFailureProbe() {
            return () -> Health.down(new IllegalStateException(DIAGNOSTIC)).build();
        }
    }
}
