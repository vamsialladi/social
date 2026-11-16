package com.truehunt.social.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates Indian mobile numbers (10 digits starting with 6-9, no alphabets/special characters/empty values).
 */
@Documented
@Constraint(validatedBy = PhoneNumberPolicy.JakartaValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhoneNumber {
    String message() default "Enter a valid 10-digit Indian mobile number starting with 6, 7, 8, or 9.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

