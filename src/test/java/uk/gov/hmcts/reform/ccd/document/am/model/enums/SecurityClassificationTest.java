package uk.gov.hmcts.reform.ccd.document.am.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SecurityClassificationTest {

    @Test
    void shoudGetFromHierarchy() {
        assertNotNull(SecurityClassification.fromHierarchy(1));
    }
}
