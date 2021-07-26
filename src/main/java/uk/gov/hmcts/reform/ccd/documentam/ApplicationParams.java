package uk.gov.hmcts.reform.ccd.documentam;

import org.springframework.beans.factory.annotation.Value;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

@Named
@Singleton
public class ApplicationParams {

    @Value("${documentStoreUrl}")
    private String documentURL;

    @Value("${documentTtlInDays}")
    private int documentTtlInDays;

    @Value("${idam.s2s-auth.totp_secret}")
    private String salt;

    @Value("${hash.check.enabled}")
    private boolean hashCheckEnabled;

    @Value("${bulkscan.exception.record.types}")
    private List<String> bulkScanExceptionRecordTypes;

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

    public List<String> getBulkScanExceptionRecordTypes() {
        return Optional.ofNullable(bulkScanExceptionRecordTypes).orElse(emptyList());
    }

}
