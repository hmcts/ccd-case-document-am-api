package uk.gov.hmcts.reform.ccd.documentam;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StartupConfigPrinter implements ApplicationListener<ApplicationReadyEvent> {

    private final ApplicationParams applicationParams;

    @Autowired
    public StartupConfigPrinter(ApplicationParams applicationParams) {
        this.applicationParams = applicationParams;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Stream configurations:\nDOWNLOAD_ENABLED: {}\nUPLOAD_ENABLED: {}", 
         applicationParams.isStreamDownloadEnabled(), 
         applicationParams.isStreamUploadEnabled());
    }
}
