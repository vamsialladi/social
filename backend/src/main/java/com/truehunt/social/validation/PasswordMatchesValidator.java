package com.truehunt.social.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;

import java.util.Objects;

/**
 * Validates matching password and confirm-password properties when confirmation is supplied.
 */
public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    private String passwordField;
    private String confirmPasswordField;
    private String message;

    @Override
    public void initialize(PasswordMatches constraintAnnotation) {
        this.passwordField = constraintAnnotation.passwordField();
        this.confirmPasswordField = constraintAnnotation.confirmPasswordField();
        this.message = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        BeanWrapperImpl wrapper = new BeanWrapperImpl(value);
        Object passwordObj = wrapper.getPropertyValue(passwordField);
        Object confirmObj = wrapper.getPropertyValue(confirmPasswordField);

        if (confirmObj == null) {
            return true;
        }
        String password = passwordObj != null ? passwordObj.toString() : null;
        String confirmPassword = confirmObj.toString();
        if (Objects.equals(password, confirmPassword)) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(confirmPasswordField)
                .addConstraintViolation();
        return false;
    }
}

