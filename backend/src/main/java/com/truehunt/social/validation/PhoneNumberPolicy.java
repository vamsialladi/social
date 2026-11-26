package com.truehunt.social.validation;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.PhoneNumberUtil.ValidationResult;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.truehunt.social.dto.RegisterRequest;
import com.truehunt.social.exception.BadRequestException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * International mobile phone number validation and normalization rules using
 * Google's libphonenumber.
 */
public final class PhoneNumberPolicy {

    public static final String MSG_REQUIRED = "Phone number is required.";
    public static final String MSG_INVALID_COUNTRY = "Please select a valid country.";
    public static final String MSG_INVALID_PHONE = "Please enter a valid mobile number for the selected country.";
    public static final String MSG_INCORRECT_LENGTH = "Please enter the correct number of digits for the selected country.";
    public static final String MSG_DIGITS_ONLY = "Phone number must contain digits only.";
    public static final String MSG_DUPLICATE = "This phone number is already registered. Please use another phone number.";

    public static final String DEFAULT_REGION = "IN";
    public static final String DEFAULT_CALLING_CODE = "+91";

    private static final Pattern ALLOWED_FORMATTING_CHARS = Pattern.compile("^[0-9+\\-()\\s]+$");
    private static final Pattern FORMATTED_SEPARATORS = Pattern.compile("[\\s\\-()]");

    private PhoneNumberPolicy() {
    }

    /**
     * Resolves the ISO 2-letter uppercase region code (e.g. "IN", "US", "GB")
     * from given country string, country calling code, and raw phone input.
     * Returns null if the country is specified but invalid.
     */
    public static String resolveRegion(String country, String countryCode, String rawPhone) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Set<String> supportedRegions = phoneUtil.getSupportedRegions();

        if (country != null && !country.isBlank()) {
            String c = country.trim().toUpperCase(Locale.ROOT);
            if (c.startsWith("+") || c.matches("^\\d+$")) {
                try {
                    int code = Integer.parseInt(c.replace("+", ""));
                    String region = phoneUtil.getRegionCodeForCountryCode(code);
                    if (!"ZZ".equals(region) && !region.isEmpty()) {
                        return region;
                    }
                } catch (NumberFormatException ignored) {
                }
                return null;
            }
            if (c.length() == 2 && supportedRegions.contains(c)) {
                return c;
            }
            // Common country name / alias mappings
            if ("USA".equals(c) || "UNITED STATES".equals(c))
                return "US";
            if ("UK".equals(c) || "UNITED KINGDOM".equals(c))
                return "GB";
            if ("INDIA".equals(c))
                return "IN";
            if ("CANADA".equals(c))
                return "CA";
            if ("AUSTRALIA".equals(c))
                return "AU";
            if ("GERMANY".equals(c))
                return "DE";
            if ("UAE".equals(c) || "UNITED ARAB EMIRATES".equals(c))
                return "AE";
            if ("SINGAPORE".equals(c))
                return "SG";

            // Fallback: check locale display countries
            for (String iso : Locale.getISOCountries()) {
                Locale loc = new Locale("", iso);
                if (loc.getDisplayCountry(Locale.ENGLISH).equalsIgnoreCase(c)
                        || loc.getCountry().equalsIgnoreCase(c)) {
                    if (supportedRegions.contains(iso)) {
                        return iso;
                    }
                }
            }
            return null; // Invalid country explicitly specified
        }

        if (countryCode != null && !countryCode.isBlank()) {
            String cleanCc = countryCode.trim().replace("+", "");
            if (cleanCc.matches("^\\d+$")) {
                try {
                    int code = Integer.parseInt(cleanCc);
                    String region = phoneUtil.getRegionCodeForCountryCode(code);
                    if (!"ZZ".equals(region) && !region.isEmpty()) {
                        return region;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            return null; // Invalid country calling code explicitly specified
        }

        return DEFAULT_REGION;
    }

    /**
     * Returns the 2-letter country code or default "IN".
     */
    public static String resolveCountry(String rawPhone, String country, String countryCode) {
        String region = resolveRegion(country, countryCode, rawPhone);
        if (region == null || "ZZ".equals(region)) {
            if (rawPhone != null && rawPhone.trim().startsWith("+")) {
                try {
                    PhoneNumber parsed = PhoneNumberUtil.getInstance().parse(rawPhone.trim(), "ZZ");
                    String inferred = PhoneNumberUtil.getInstance().getRegionCodeForNumber(parsed);
                    if (inferred != null && !"ZZ".equals(inferred)) {
                        return inferred;
                    }
                } catch (Exception ignored) {
                }
            }
            return DEFAULT_REGION;
        }
        return region;
    }

    /**
     * Returns the country calling code with "+" (e.g. "+91", "+1").
     */
    public static String resolveCountryCode(String rawPhone, String country, String countryCode) {
        if (countryCode != null && !countryCode.isBlank()) {
            String clean = countryCode.trim();
            return clean.startsWith("+") ? clean : "+" + clean;
        }
        String region = resolveCountry(rawPhone, country, countryCode);
        int code = PhoneNumberUtil.getInstance().getCountryCodeForRegion(region);
        return code > 0 ? "+" + code : DEFAULT_CALLING_CODE;
    }

    /**
     * Validates raw phone against selected country and countryCode.
     * Returns null if valid, or a specific user-facing error message.
     */
    public static String validate(String rawPhone, String country, String countryCode) {
        if (rawPhone == null || rawPhone.trim().isEmpty()) {
            return MSG_REQUIRED;
        }
        String trimmed = rawPhone.trim();

        // 1. Digits only validation: check for letters and unauthorized characters
        if (trimmed.chars().anyMatch(Character::isLetter)) {
            return MSG_DIGITS_ONLY;
        }
        if (!ALLOWED_FORMATTING_CHARS.matcher(trimmed).matches()) {
            return MSG_DIGITS_ONLY;
        }
        int plusIndex = trimmed.indexOf('+');
        if (plusIndex > 0 || (plusIndex == 0 && trimmed.indexOf('+', 1) != -1)) {
            return MSG_DIGITS_ONLY;
        }

        // 2. Resolve country region
        String region = resolveRegion(country, countryCode, trimmed);
        if (region == null) {
            return MSG_INVALID_COUNTRY;
        }

        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

        // 3. Prepare parse target
        String parseTarget = trimmed;
        String compactDigits = FORMATTED_SEPARATORS.matcher(trimmed).replaceAll("");
        if (DEFAULT_REGION.equals(region)) {
            if (compactDigits.length() == 12 && compactDigits.startsWith("91") && !trimmed.startsWith("+")) {
                parseTarget = "+" + compactDigits;
            }
        }

        PhoneNumber phoneNumber = null;
        try {
            phoneNumber = phoneUtil.parse(parseTarget, "ZZ".equals(region) ? null : region);
        } catch (NumberParseException e) {
            if (e.getErrorType() == NumberParseException.ErrorType.TOO_SHORT_NSN
                    || e.getErrorType() == NumberParseException.ErrorType.TOO_SHORT_AFTER_IDD) {
                return MSG_INCORRECT_LENGTH;
            }
        }

        if (phoneNumber == null
                || (!phoneUtil.isValidNumber(phoneNumber) && !"ZZ".equals(region) && !trimmed.startsWith("+"))) {
            int countryCallingCode = phoneUtil.getCountryCodeForRegion(region);
            if (countryCallingCode > 0) {
                try {
                    PhoneNumber alt = phoneUtil.parse("+" + countryCallingCode + compactDigits, region);
                    if (phoneUtil.isValidNumber(alt) || phoneUtil.isPossibleNumber(alt)) {
                        phoneNumber = alt;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (phoneNumber == null) {
            return MSG_INVALID_PHONE;
        }

        // 4. Country calling code matching
        if (!"ZZ".equals(region)) {
            int expectedCallingCode = phoneUtil.getCountryCodeForRegion(region);
            if (phoneNumber.getCountryCode() != expectedCallingCode) {
                return MSG_INVALID_PHONE;
            }
        }

        // 5. Length validation
        ValidationResult lenResult = phoneUtil.isPossibleNumberWithReason(phoneNumber);
        if (lenResult == ValidationResult.TOO_SHORT
                || lenResult == ValidationResult.TOO_LONG
                || lenResult == ValidationResult.INVALID_LENGTH) {
            return MSG_INCORRECT_LENGTH;
        }

        String national = phoneUtil.getNationalSignificantNumber(phoneNumber);
        if ("IN".equals(region) && national.length() != 10) {
            return MSG_INCORRECT_LENGTH;
        }
        if (("US".equals(region) || "CA".equals(region)) && national.length() != 10) {
            return MSG_INCORRECT_LENGTH;
        }
        if ("GB".equals(region) && national.length() != 10) {
            return MSG_INCORRECT_LENGTH;
        }

        // 6. Country-specific mobile rules
        if ("IN".equals(region) || phoneNumber.getCountryCode() == 91) {
            char firstDigit = national.charAt(0);
            if (firstDigit != '6' && firstDigit != '7' && firstDigit != '8' && firstDigit != '9') {
                return MSG_INVALID_PHONE;
            }
            if (national.length() != 10) {
                return MSG_INCORRECT_LENGTH;
            }
            return null;
        }

        // Validity check for other countries
        if (!phoneUtil.isValidNumber(phoneNumber) && !phoneUtil.isPossibleNumber(phoneNumber)) {
            return MSG_INVALID_PHONE;
        }

        PhoneNumberType type = phoneUtil.getNumberType(phoneNumber);
        if (type != PhoneNumberType.MOBILE && type != PhoneNumberType.FIXED_LINE_OR_MOBILE && type != PhoneNumberType.UNKNOWN) {
            return MSG_INVALID_PHONE;
        }

        return null;
    }

    /**
     * Backward-compatible validator using default region "IN".
     */
    public static String validate(String rawPhone) {
        return validate(rawPhone, null, null);
    }

    /**
     * Validates and normalizes phone number to E.164 format
     * (+<countryCode><nationalNumber>).
     * Throws BadRequestException on validation failure.
     */
    public static String validateAndNormalize(String rawPhone, String country, String countryCode) {
        if (rawPhone == null) {
            return null;
        }
        String error = validate(rawPhone, country, countryCode);
        if (error != null) {
            throw new BadRequestException("VALIDATION_ERROR", error, Map.of("phoneNumber", error));
        }
        return normalize(rawPhone, country, countryCode);
    }

    public static String validateAndNormalize(String rawPhone) {
        return validateAndNormalize(rawPhone, null, null);
    }

    /**
     * Normalizes a valid phone number into E.164 format.
     */
    public static String normalize(String rawPhone, String country, String countryCode) {
        if (rawPhone == null) {
            return null;
        }
        String trimmed = rawPhone.trim();
        String region = resolveRegion(country, countryCode, trimmed);
        if (region == null) {
            region = DEFAULT_REGION;
        }
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

        String parseTarget = trimmed;
        String compactDigits = FORMATTED_SEPARATORS.matcher(trimmed).replaceAll("");
        if (DEFAULT_REGION.equals(region)) {
            if (compactDigits.length() == 12 && compactDigits.startsWith("91") && !trimmed.startsWith("+")) {
                parseTarget = "+" + compactDigits;
            }
        }

        PhoneNumber phoneNumber = null;
        try {
            phoneNumber = phoneUtil.parse(parseTarget, "ZZ".equals(region) ? null : region);
        } catch (NumberParseException ignored) {
        }

        if (phoneNumber == null
                || (!phoneUtil.isValidNumber(phoneNumber) && !"ZZ".equals(region) && !trimmed.startsWith("+"))) {
            int countryCallingCode = phoneUtil.getCountryCodeForRegion(region);
            if (countryCallingCode > 0) {
                try {
                    PhoneNumber alt = phoneUtil.parse("+" + countryCallingCode + compactDigits, region);
                    if (phoneUtil.isValidNumber(alt)) {
                        phoneNumber = alt;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (phoneNumber == null) {
            return null;
        }
        return phoneUtil.format(phoneNumber, PhoneNumberFormat.E164);
    }

    /**
     * Normalizes without throwing, returning E.164 if valid or null.
     */
    public static String normalizeUnchecked(String rawPhone, String country, String countryCode) {
        if (rawPhone == null) {
            return null;
        }
        String error = validate(rawPhone, country, countryCode);
        if (error != null) {
            return null;
        }
        return normalize(rawPhone, country, countryCode);
    }

    public static String normalizeUnchecked(String rawPhone) {
        return normalizeUnchecked(rawPhone, null, null);
    }

    /**
     * Extracts national significant number digits (e.g. "+919876543210" ->
     * "9876543210").
     */
    public static String extractNationalNumber(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try {
            PhoneNumber parsed = phoneUtil.parse(phone.trim(), DEFAULT_REGION);
            return phoneUtil.getNationalSignificantNumber(parsed);
        } catch (Exception ex) {
            String digits = phone.replaceAll("[^0-9]", "");
            if (phone.startsWith("+91") && digits.length() >= 12) {
                return digits.substring(2);
            }
            return digits;
        }
    }

    /**
     * Jakarta Bean Validation adapter. Supports class-level RegisterRequest and
     * String fields.
     */
    public static class JakartaValidator implements ConstraintValidator<ValidPhoneNumber, Object> {
        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }

            if (value instanceof RegisterRequest req) {
                String phone = req.getPhone();
                if (phone == null || phone.isBlank()) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(MSG_REQUIRED)
                            .addPropertyNode("phoneNumber")
                            .addConstraintViolation();
                    return false;
                }
                String error = PhoneNumberPolicy.validate(phone, req.getCountry(), req.getCountryCode());
                if (error != null) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(error)
                            .addPropertyNode("phoneNumber")
                            .addConstraintViolation();
                    return false;
                }
                return true;
            }

            if (value instanceof String phoneStr) {
                String error = PhoneNumberPolicy.validate(phoneStr, null, null);
                if (error != null) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(error).addConstraintViolation();
                    return false;
                }
                return true;
            }

            return true;
        }
    }
}
