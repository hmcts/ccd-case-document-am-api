package uk.gov.hmcts.reform.ccd.document.am.configuration;

import feign.Response;
import feign.codec.ErrorDecoder;

public class CustomErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {

        switch (response.status()){
            case 400:
                return new Exception();
            case 404:
                return new Exception();
            default:
                return new Exception("Generic error");
        }
    }
}
