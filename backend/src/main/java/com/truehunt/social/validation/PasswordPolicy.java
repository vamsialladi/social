package com.truehunt.social.validation;

import com.truehunt.social.exception.BadRequestException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Shared server-side password policy enforcing all complexity and confirmation rules.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 20;
    public static final String ALLOWED_SPECIAL_CHARS = "@#$%!&*";
    public static final String NO_SPACES_REGEX = "^\\S*$";
    public static final String PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[@#$%!&*])\\S{8,20}$";

    private PasswordPolicy() {
    }

    public static Optional<String> validate(String password) {
        if (password == null || password.isBlank()) {
            return Optional.of("Password is required");
        }
        if (containsWhitespace(password)) {
            return Optional.of("Password must not contain spaces");
        }
        if (password.length() < MIN_LENGTH) {
            return Optional.of("Password must be at least 8 characters");
        }
        if (password.length() > MAX_LENGTH) {
            return Optional.of("Password must not exceed 20 characters");
        }
        if (!hasUppercase(password)) {
            return Optional.of("Password must contain at least one uppercase letter");
        }
        if (!hasLowercase(password)) {
            return Optional.of("Password must contain at least one lowercase letter");
        }
        if (!hasDigit(password)) {
            return Optional.of("Password must contain at least one number");
        }
        if (!hasSpecialCharacter(password)) {
            return Optional.of("Password must contain at least one special character (@, #, $, %, !, &, *)");
        }
        return Optional.empty();
    }

    public static Optional<String> validateComplexity(String password) {
        if (password == null || password.isBlank() || containsWhitespace(password)) {
            return Optional.empty();
        }
        if (password.length() < MIN_LENGTH) {
            return Optional.of("Password must be at least 8 characters");
        }
        if (password.length() > MAX_LENGTH) {
            return Optional.of("Password must not exceed 20 characters");
        }
        if (!hasUppercase(password)) {
            return Optional.of("Password must contain at least one uppercase letter");
        }
        if (!hasLowercase(password)) {
            return Optional.of("Password must contain at least one lowercase letter");
        }
        if (!hasDigit(password)) {
            return Optional.of("Password must contain at least one number");
        }
        if (!hasSpecialCharacter(password)) {
            return Optional.of("Password must contain at least one special character (@, #, $, %, !, &, *)");
        }
        return Optional.empty();
    }

    public static void requireValid(String password, String fieldName) {
        Optional<String> violation = validate(password);
        if (violation.isPresent()) {
            Map<String, String> errors = new LinkedHashMap<>();
            errors.put(fieldName, violation.get());
            throw new BadRequestException("VALIDATION_ERROR", "Password validation failed", errors);
        }
    }

    public static void requireConfirmed(String password, String confirmPassword, boolean confirmRequired) {
        if (confirmRequired && (confirmPassword == null || confirmPassword.isBlank())) {
            Map<String, String> errors = new LinkedHashMap<>();
            errors.put("confirmPassword", "Confirm password is required");
            throw new BadRequestException("VALIDATION_ERROR", "Password validation failed", errors);
        }
        if (confirmPassword != null && !Objects.equals(password, confirmPassword)) {
            Map<String, String> errors = new LinkedHashMap<>();
            errors.put("confirmPassword", "Password and confirm password do not match");
            throw new BadRequestException("VALIDATION_ERROR", "Password validation failed", errors);
        }
    }

    public static boolean containsWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasUppercase(String value) {
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch >= 'A' && ch <= 'Z') {
                return true;
            }
        }
        return false;
    }

    private static boolean hasLowercase(String value) {
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch >= 'a' && ch <= 'z') {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDigit(String value) {
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch >= '0' && ch <= '9') {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSpecialCharacter(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (ALLOWED_SPECIAL_CHARS.indexOf(value.charAt(i)) >= 0) {
                return true;
            }
        }
        return false;
    }
}

