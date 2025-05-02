package uk.gov.hmcts.reform.ccd.documentam.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassificationValueValidator implements ConstraintValidator<ClassificationValue, CharSequence> {
    private Set<String> acceptedValues;

    @Override
    public void initialize(ClassificationValue constraintAnnotation) {
        acceptedValues = Stream.of(constraintAnnotation.enumClass().getEnumConstants())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean isValid(final CharSequence value, final ConstraintValidatorContext context) {
        return Optional.ofNullable(value)
            .map(input -> acceptedValues.contains(input.toString()))
            .orElse(true);
    }
}
