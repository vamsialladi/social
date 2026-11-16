package com.truehunt.social.exception;

import com.truehunt.social.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleApiException(ApiException ex) {
        log.warn("API Exception [{}]: {}", ex.getCode(), ex.getMessage());
        ApiResponse<Object> body = (ex.getFields() != null && !ex.getFields().isEmpty())
                ? ApiResponse.error(ex.getCode(), ex.getMessage(), ex.getFields())
                : ApiResponse.error(ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        List<FieldError> sortedErrors = ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparingInt(this::constraintPriority))
                .toList();
        for (FieldError error : sortedErrors) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        boolean hasPasswordError = fieldErrors.keySet().stream()
                .anyMatch(field -> "password".equals(field)
                        || "newPassword".equals(field)
                        || "confirmPassword".equals(field));
        String message = hasPasswordError
                ? "Password validation failed"
                : (fieldErrors.isEmpty() ? "Validation failed" : fieldErrors.values().iterator().next());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("VALIDATION_ERROR", message, fieldErrors));
    }

    private int constraintPriority(FieldError error) {
        String code = error.getCode();
        if ("NotBlank".equals(code) || "NotNull".equals(code) || "NotEmpty".equals(code)) {
            return 0;
        }
        if ("Pattern".equals(code)) {
            return 1;
        }
        if ("ValidPassword".equals(code)) {
            return 2;
        }
        if ("Size".equals(code)) {
            return 3;
        }
        if ("PasswordMatches".equals(code)) {
            return 4;
        }
        return 5;
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("FORBIDDEN", "Access is denied: " + ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("UNAUTHORIZED", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler({
            org.hibernate.NonUniqueResultException.class,
            jakarta.persistence.NonUniqueResultException.class,
            org.springframework.dao.IncorrectResultSizeDataAccessException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleNonUniqueResult(Exception ex) {
        log.warn("Non-unique result encountered: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("DUPLICATE_ENTITY", "An account or profile record already exists for these credentials. Please sign in or use alternate details."));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrity(org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        if (isDuplicateEmailViolation(ex)) {
            String message = "This email is already registered. Please use another email address.";
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("DUPLICATE_EMAIL", message, Map.of("email", message)));
        }
        if (isDuplicatePhoneViolation(ex)) {
            String message = "This phone number is already registered. Please use another phone number.";
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("DUPLICATE_PHONE_NUMBER", message, Map.of("phoneNumber", message)));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("CONFLICT", "This request conflicts with existing data. Please check the details and try again."));
    }

    private boolean isDuplicateEmailViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase(java.util.Locale.ROOT);
                if (lower.contains("uk_user_accounts_email") || lower.contains("(email")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isDuplicatePhoneViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase(java.util.Locale.ROOT);
                if (lower.contains("uk_user_accounts_phone_number") || lower.contains("phone_number")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    @ExceptionHandler({
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.multipart.support.MissingServletRequestPartException.class,
            org.springframework.web.multipart.MultipartException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleClientBadRequestExceptions(Exception ex) {
        log.warn("Client request error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not supported: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("METHOD_NOT_ALLOWED", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        log.error("Unhandled exception occurred: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_SERVER_ERROR", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred"));
    }
}
