package uk.gov.hmcts.reform.ccd.documentam.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.HibernateValidator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import uk.gov.hmcts.reform.ccd.documentam.model.enums.Permission;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorisedServiceValidationTest {

    private static final String JURISDICTION_ID_PROPERTY = "jurisdictionId";
    private static final String WILDCARD_ALONE_PROPERTY = "jurisdictionIdWildcardAlone";
    private static final String WILDCARD_MESSAGE = "jurisdictionId: \"*\" must be the only element when present";
    private static final String NOT_EMPTY_MESSAGE = "must not be empty";

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.byProvider(HibernateValidator.class)
            .configure()
            .defaultLocale(Locale.ENGLISH)
            .buildValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    static Stream<List<String>> validJurisdictionIds() {
        return Stream.of(
            List.of("*"),
            List.of("A"),
            List.of("A", "B")
        );
    }

    static Stream<List<String>> wildcardCombinedWithOtherIds() {
        return Stream.of(
            List.of("*", "A"),
            List.of("A", "*"),
            List.of("*", "*")
        );
    }

    @ParameterizedTest
    @MethodSource("validJurisdictionIds")
    void shouldBeValidWhenJurisdictionIdIsAllowed(List<String> jurisdictionId) {
        assertThat(validator.validate(serviceWith(jurisdictionId))).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("wildcardCombinedWithOtherIds")
    void shouldBeInvalidWhenWildcardIsNotTheOnlyElement(List<String> jurisdictionId) {
        assertSingleViolation(validator.validate(serviceWith(jurisdictionId)),
                              WILDCARD_ALONE_PROPERTY, WILDCARD_MESSAGE);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldBeInvalidWhenJurisdictionIdIsNullOrEmpty(List<String> jurisdictionId) {
        assertSingleViolation(validator.validate(serviceWith(jurisdictionId)),
                              JURISDICTION_ID_PROPERTY, NOT_EMPTY_MESSAGE);
    }

    private static AuthorisedService serviceWith(List<String> jurisdictionId) {
        return AuthorisedService.builder()
            .id("test_service")
            .caseTypeId(List.of("*"))
            .jurisdictionId(jurisdictionId)
            .permissions(List.of(Permission.READ))
            .build();
    }

    private static void assertSingleViolation(Set<ConstraintViolation<AuthorisedService>> violations,
                                              String expectedProperty,
                                              String expectedMessage) {
        assertThat(violations)
            .singleElement()
            .satisfies(violation -> {
                assertThat(violation.getPropertyPath()).hasToString(expectedProperty);
                assertThat(violation.getMessage()).isEqualTo(expectedMessage);
            });
    }
}
