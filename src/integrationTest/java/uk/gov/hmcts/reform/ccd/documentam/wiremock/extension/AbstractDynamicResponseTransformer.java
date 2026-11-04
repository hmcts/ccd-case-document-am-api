package uk.gov.hmcts.reform.ccd.documentam.wiremock.extension;

import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

import org.springframework.http.HttpStatus;

/*
 * Customises the static stubbed response before sending it back to the client
 */
public abstract class AbstractDynamicResponseTransformer implements ResponseTransformerV2 {

    @Override
    public Response transform(Response response, ServeEvent serveEvent) {
        try {
            return Response.Builder.like(response)
                .but()
                .body(dynamicResponse(serveEvent.getRequest(), response))
                .build();

        } catch (SecurityException ex) {
            return Response.Builder.like(response)
                .but()
                .status(HttpStatus.UNAUTHORIZED.value())
                .statusMessage(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .build();
        }
    }

    @Override
    public boolean applyGlobally() {
        // This flag will ensure this transformer is used only for those request mappings that have the transformer
        // configured
        return false;
    }

    protected abstract String dynamicResponse(Request request, Response response);
}
