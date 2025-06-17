package uk.gov.hmcts.reform.ccd.documentam;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

@Getter
@Component
public class ApplicationParams {

    @Value("${documentStoreUrl}")
    private String documentURL;

    @Value("${documentTtlInDays}")
    private int documentTtlInDays;

    @Value("${idam.s2s-auth.totp_secret}")
    private String salt;

    @Value("${hash.check.enabled}")
    private boolean hashCheckEnabled;

    @Value("${moving.case.types}")
    private List<String> movingCaseTypes;

    @Value("#{'${request.forwarded_headers.from_client}'.split(',')}")
    private List<String> clientRequestHeadersToForward;

    @Value("${stream.download.enabled}")
    private boolean isStreamDownloadEnabled;

    @Value("${stream.upload.enabled}")
    private boolean isStreamUploadEnabled;

    public List<String> getMovingCaseTypes() {
        return Optional.ofNullable(movingCaseTypes).orElse(emptyList());
    }
}
