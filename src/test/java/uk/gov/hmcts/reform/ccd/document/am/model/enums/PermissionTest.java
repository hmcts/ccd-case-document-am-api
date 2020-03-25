package uk.gov.hmcts.reform.ccd.document.am.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import static uk.gov.hmcts.reform.ccd.document.am.model.enums.Permission.*;

class PermissionTest {

    @Test
    void getValue() {
        assertNotNull(CREATE);
        assertEquals(1, CREATE.getValue());
        assertEquals(2, READ.getValue());
        assertEquals(4, UPDATE.getValue());
        assertEquals(8, DELETE.getValue());
    }

    @Test
    void isGranted() {

        assertTrue(CREATE.isGranted(1));
        assertTrue(READ.isGranted(2));
        assertTrue(UPDATE.isGranted(4));
        assertTrue(DELETE.isGranted(8));
    }
}
