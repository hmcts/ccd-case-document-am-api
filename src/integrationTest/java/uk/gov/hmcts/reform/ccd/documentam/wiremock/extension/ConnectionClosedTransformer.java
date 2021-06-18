package uk.gov.hmcts.reform.ccd.documentam.wiremock.extension;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import org.springframework.http.HttpHeaders;

// Same issue as here https://github.com/tomakehurst/wiremock/issues/97
public class ConnectionClosedTransformer extends ResponseDefinitionTransformer {

    @Override
    public String getName() {
        return "keep-alive-disabler";
    }

    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition,
                                        FileSource files, Parameters parameters) {
        return ResponseDefinitionBuilder.like(responseDefinition)
            .withHeader(HttpHeaders.CONNECTION, "close")
            .build();
    }

}
