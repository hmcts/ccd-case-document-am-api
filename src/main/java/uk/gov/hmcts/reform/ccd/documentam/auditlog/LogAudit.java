package uk.gov.hmcts.reform.ccd.documentam.auditlog;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation on the endpoint method to create the audit log entry and send to stdout.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface LogAudit {

    AuditOperationType operationType();

    /**
     * Expression to parse when loading the Document ID from a string value.
     */
    String documentId() default "";

    /**
     * Expression to parse when loading a list of Document IDs.
     */
    String documentIds() default "";

    /**
     * Expression to parse when loading the Case ID from a string value.
     */
    String caseId() default "";

    /**
     * Expression to parse when loading a list of Case IDs.
     */
    String caseIds() default "";


    /**
     * Expression to parse when loading the Jurisdiction ID from a string value.
     */
    String jurisdiction() default "";


    /**
     * Expression to parse when loading the Case Type ID from a string value.
     */
    String caseType() default "";

}
