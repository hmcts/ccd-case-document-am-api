package uk.gov.hmcts.reform.ccd.documentam.dto.validation;

import uk.gov.hmcts.reform.ccd.documentam.model.enums.Classification;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static uk.gov.hmcts.reform.ccd.documentam.apihelper.Constants.CLASSIFICATION_ID_INVALID;

@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = ClassificationValueValidator.class)
public @interface ClassificationValue {
    Class<Classification> enumClass() default Classification.class;
    String message() default CLASSIFICATION_ID_INVALID;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
