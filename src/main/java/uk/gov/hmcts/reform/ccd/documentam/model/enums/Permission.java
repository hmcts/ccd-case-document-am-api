package uk.gov.hmcts.reform.ccd.documentam.model.enums;

/**
 * Exposes a set of enum values used to set permissions for Access Management.
 * Each of the values is a power of two. The reason for that is that in Access Management
 * there might be multiple permissions that need to be assigned: ie. READ + CREATE.
 * In order to determine which individual permissions a record has
 * the binary 'AND' operation is done (the 'isGranted' method).
 */
public enum Permission {
    CREATE(),
    READ(),
    UPDATE(),
    DELETE(),
    HASHTOKEN(),
    ATTACH();
}
