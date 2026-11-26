package com.truehunt.social.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates international mobile numbers using Google's libphonenumber.
 * Supports both class-level (e.g. RegisterRequest) and field-level targets.
 */
@Documented
@Constraint(validatedBy = PhoneNumberPolicy.JakartaValidator.class)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhoneNumber {
    String message() default "Please enter a valid mobile number for the selected country.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
