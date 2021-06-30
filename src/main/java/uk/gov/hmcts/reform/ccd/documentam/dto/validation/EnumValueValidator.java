package uk.gov.hmcts.reform.ccd.documentam.dto.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnumValueValidator implements ConstraintValidator<EnumValue, CharSequence> {
    private Set<String> acceptedValues;

    @Override
    public void initialize(EnumValue constraintAnnotation) {
        acceptedValues = Stream.of(constraintAnnotation.enumClass().getEnumConstants())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return Optional.ofNullable(value)
            .map(x -> acceptedValues.contains(x.toString()))
            .orElse(true);
    }
}
