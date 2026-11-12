package com.truehunt.social.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Optional;

/**
 * Bean Validation constraint validator delegating password complexity checks to {@link PasswordPolicy}.
 */
public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        Optional<String> violation = PasswordPolicy.validateComplexity(value);
        if (violation.isEmpty()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(violation.get()).addConstraintViolation();
        return false;
    }
}

