package hanzner.zebrakapp.controller;

import hanzner.zebrakapp.exception.BaseException;
import hanzner.zebrakapp.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Map<String, Object>> handleBaseException(BaseException ex) {
        log.warn("Business exception: [{}] {}", ex.getErrorCode(), ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("errorCode", ex.getErrorCode().name());
        response.put("status", ex.getErrorCode().getHttpStatus().value());
        response.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("errorCode", ErrorCode.VALIDATION_ERROR.name());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("message", "Chyba validace vstupních dat");
        response.put("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("errorCode", "BAD_CREDENTIALS");
        response.put("status", HttpStatus.UNAUTHORIZED.value());
        response.put("message", "Nesprávný e-mail nebo heslo");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("errorCode", ErrorCode.UNAUTHORIZED_ACCESS.name());
        response.put("status", HttpStatus.FORBIDDEN.value());
        response.put("message", "Nemáte oprávnění k provedení této akce");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("errorCode", ErrorCode.VALIDATION_ERROR.name());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurity(SecurityException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("errorCode", ErrorCode.UNAUTHORIZED_ACCESS.name());
        response.put("status", HttpStatus.FORBIDDEN.value());
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        log.debug("Statický prostředek nenalezen: {}", ex.getResourcePath());
        Map<String, Object> response = new HashMap<>();
        response.put("errorCode", "RESOURCE_NOT_FOUND");
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("message", "Požadovaný prostředek nebyl nalezen: " + ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        log.warn("Překročena maximální povolená velikost nahrávaného souboru: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("errorCode", ErrorCode.PAYLOAD_TOO_LARGE.name());
        response.put("status", HttpStatus.PAYLOAD_TOO_LARGE.value());
        response.put("message", "Velikost nahrávaného souboru překročila maximální povolený limit.");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("errorCode", "METHOD_NOT_ALLOWED");
        response.put("status", HttpStatus.METHOD_NOT_ALLOWED.value());
        response.put("message", "HTTP metoda není podporována: " + ex.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("Neočekávaná chyba serveru: ", ex);
        Map<String, Object> response = new HashMap<>();
        response.put("errorCode", ErrorCode.INTERNAL_SERVER_ERROR.name());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("message", "Došlo k neočekávané chybě serveru: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
