package uk.gov.hmcts.reform.ccd.documentam;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

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

    public String getDocumentURL() {
        return documentURL;
    }

    public int getDocumentTtlInDays() {
        return documentTtlInDays;
    }

    public String getSalt() {
        return salt;
    }

    public boolean isHashCheckEnabled() {
        return hashCheckEnabled;
    }

    public List<String> getMovingCaseTypes() {
        return Optional.ofNullable(movingCaseTypes).orElse(emptyList());
    }

}
