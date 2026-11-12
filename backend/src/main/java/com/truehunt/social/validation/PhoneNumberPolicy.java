package com.truehunt.social.validation;

import com.truehunt.social.exception.BadRequestException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Shared Indian mobile phone number normalization and validation rules.
 */
public final class PhoneNumberPolicy {

    private static final Pattern ALLOWED_FORMATTING_CHARS = Pattern.compile("^[0-9+\\-()\\s]+$");
    private static final Pattern CANONICAL_INDIAN_MOBILE = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern FORMATTED_SEPARATORS = Pattern.compile("[\\s\\-()]");

    private PhoneNumberPolicy() {
    }

    /**
     * Validates and normalizes an Indian mobile number to its 10-digit canonical form.
     * Returns null only when rawPhone is null (email-only API calls).
     */
    public static String validateAndNormalize(String rawPhone) {
        if (rawPhone == null) {
            return null;
        }
        String error = validate(rawPhone);
        if (error != null) {
            throw new BadRequestException("VALIDATION_ERROR", error, Map.of("phoneNumber", error));
        }
        return normalizeUnchecked(rawPhone);
    }

    /**
     * Validates a non-null phone string and returns the first human-readable error, or null when valid.
     */
    public static String validate(String rawPhone) {
        if (rawPhone == null) {
            return null;
        }
        String trimmed = rawPhone.trim();
        if (trimmed.isEmpty()) {
            return "Phone number is required.";
        }
        if (trimmed.chars().anyMatch(Character::isLetter)) {
            return "Phone number must not contain alphabets.";
        }
        if (!ALLOWED_FORMATTING_CHARS.matcher(trimmed).matches()) {
            return "Phone number must not contain special characters.";
        }
        int plusIndex = trimmed.indexOf('+');
        if (plusIndex > 0 || (plusIndex == 0 && trimmed.indexOf('+', 1) != -1)) {
            return "Phone number must not contain special characters.";
        }

        String normalized = normalizeUnchecked(trimmed);
        if (normalized == null) {
            String digitsOnly = FORMATTED_SEPARATORS.matcher(trimmed).replaceAll("");
            if (digitsOnly.startsWith("+") && !digitsOnly.startsWith("+91")) {
                return "Indian mobile number must start with 6, 7, 8, or 9.";
            }
            String strippedPrefix = stripCountryOrTrunkPrefix(digitsOnly);
            if (strippedPrefix.length() != 10) {
                return "Phone number must contain exactly 10 digits.";
            }
            return "Indian mobile number must start with 6, 7, 8, or 9.";
        }
        return null;
    }

    /**
     * Normalizes an Indian phone string to canonical 10 digits if possible, or returns null.
     */
    public static String normalizeUnchecked(String rawPhone) {
        if (rawPhone == null) {
            return null;
        }
        String compact = FORMATTED_SEPARATORS.matcher(rawPhone.trim()).replaceAll("");
        if (compact.isEmpty()) {
            return null;
        }
        if (compact.startsWith("+")) {
            if (!compact.startsWith("+91")) {
                return null;
            }
            compact = compact.substring(3);
        } else if (compact.length() == 12 && compact.startsWith("91")) {
            compact = compact.substring(2);
        } else if (compact.length() == 11 && compact.startsWith("0")) {
            compact = compact.substring(1);
        }

        return CANONICAL_INDIAN_MOBILE.matcher(compact).matches() ? compact : null;
    }

    private static String stripCountryOrTrunkPrefix(String compact) {
        if (compact.startsWith("+91")) {
            return compact.substring(3);
        }
        if (compact.length() == 12 && compact.startsWith("91")) {
            return compact.substring(2);
        }
        if (compact.length() == 11 && compact.startsWith("0")) {
            return compact.substring(1);
        }
        return compact;
    }

    /**
     * Jakarta Bean Validation adapter delegating phone checks to {@link PhoneNumberPolicy}.
     */
    public static class JakartaValidator implements ConstraintValidator<ValidPhoneNumber, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }
            String error = PhoneNumberPolicy.validate(value);
            if (error == null) {
                return true;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(error).addConstraintViolation();
            return false;
        }
    }
}

