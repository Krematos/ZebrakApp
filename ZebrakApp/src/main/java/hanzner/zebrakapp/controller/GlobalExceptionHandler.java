package hanzner.zebrakapp.controller;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import hanzner.zebrakapp.exception.BaseException;
import hanzner.zebrakapp.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        Object value = ex.getValue();
        Class<?> requiredType = ex.getRequiredType();

        String message;
        if (requiredType != null && requiredType.isEnum()) {
            String allowedValues = Arrays.stream(requiredType.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            message = String.format("Neplatná hodnota '%s' pro parametr '%s'. Povolené hodnoty: [%s]", value, paramName, allowedValues);
        } else {
            message = String.format("Neplatná hodnota '%s' pro parametr '%s'. Očekávaný typ: %s",
                    value, paramName, requiredType != null ? requiredType.getSimpleName() : "neznámý");
        }

        log.warn("Chybný parametr požadavku: {}", message);

        Map<String, String> errors = new HashMap<>();
        errors.put(paramName, message);

        Map<String, Object> response = new HashMap<>();
        response.put("errorCode", ErrorCode.VALIDATION_ERROR.name());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("message", message);
        response.put("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String message = "Neplatný formát těla požadavku (JSON).";
        Map<String, String> errors = new HashMap<>();

        Throwable specificCause = ex.getMostSpecificCause();
        InvalidFormatException ife = null;
        com.fasterxml.jackson.databind.exc.MismatchedInputException mie = null;
        JsonMappingException jme = null;
        JsonParseException jpe = null;

        for (Throwable current : Arrays.asList(specificCause, ex.getCause(), ex)) {
            while (current != null) {
                if (current instanceof InvalidFormatException f && ife == null) {
                    ife = f;
                } else if (current instanceof com.fasterxml.jackson.databind.exc.MismatchedInputException m && mie == null) {
                    mie = m;
                } else if (current instanceof JsonParseException p && jpe == null) {
                    jpe = p;
                } else if (current instanceof JsonMappingException j && jme == null) {
                    jme = j;
                }
                current = current.getCause();
            }
        }

        JsonMappingException targetMappingEx = (ife != null) ? ife : (mie != null ? mie : jme);

        if (targetMappingEx != null) {
            String fieldName = targetMappingEx.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("."));

            Class<?> targetType = null;
            Object value = null;

            if (ife != null) {
                targetType = ife.getTargetType();
                value = ife.getValue();
            } else if (mie != null) {
                targetType = mie.getTargetType();
            }

            if (targetType != null && targetType.isEnum()) {
                String allowedValues = Arrays.stream(targetType.getEnumConstants())
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));
                if (value != null) {
                    message = String.format("Neplatná hodnota '%s' pro pole '%s'. Povolené hodnoty: [%s]", value, fieldName, allowedValues);
                } else {
                    message = String.format("Neplatná hodnota pro pole '%s'. Povolené hodnoty: [%s]", fieldName, allowedValues);
                }
            } else if (targetMappingEx.getMessage() != null && targetMappingEx.getMessage().contains("Enum class")) {
                message = String.format("Neplatná hodnota pro pole '%s'. Zkontrolujte povolené hodnoty.", fieldName);
            } else if (value != null && !fieldName.isBlank()) {
                message = String.format("Neplatná hodnota '%s' pro pole '%s'.", value, fieldName);
            } else if (!fieldName.isBlank()) {
                message = String.format("Neplatná hodnota pro pole '%s'.", fieldName);
            } else {
                message = "Chyba při zpracování JSON dat: neplatná hodnota nebo struktura.";
            }

            if (!fieldName.isBlank()) {
                errors.put(fieldName, message);
            }
        } else if (specificCause != null && specificCause.getMessage() != null && specificCause.getMessage().contains("Enum class")) {
            String msg = specificCause.getMessage();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("from String \"(.*?)\": not one of the values accepted for Enum class: \\[(.*?)\\]").matcher(msg);
            java.util.regex.Matcher fieldM = java.util.regex.Pattern.compile("\\[\"(.*?)\"\\]").matcher(msg);
            if (m.find()) {
                String val = m.group(1);
                String allowed = m.group(2);
                String field = fieldM.find() ? fieldM.group(1) : "";
                if (!field.isBlank()) {
                    message = String.format("Neplatná hodnota '%s' pro pole '%s'. Povolené hodnoty: [%s]", val, field, allowed);
                    errors.put(field, message);
                } else {
                    message = String.format("Neplatná hodnota '%s'. Povolené hodnoty: [%s]", val, allowed);
                }
            }
        } else if (jpe != null) {
            message = "Chyba syntaxe JSON formátu.";
        } else if (specificCause != null && specificCause != ex && specificCause.getMessage() != null) {
            message = "Neplatný požadavek: " + specificCause.getMessage();
        }

        log.warn("Nečitelné tělo požadavku: {}", message);

        Map<String, Object> response = new HashMap<>();
        response.put("errorCode", ErrorCode.VALIDATION_ERROR.name());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("message", message);
        if (!errors.isEmpty()) {
            response.put("errors", errors);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        String message = String.format("Chybí povinný parametr požadavku '%s'", ex.getParameterName());
        Map<String, Object> response = new HashMap<>();
        response.put("errorCode", ErrorCode.VALIDATION_ERROR.name());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("message", message);
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

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        log.debug("Statický prostředek nenalezen: {}", ex.getResourcePath());
        Map<String, Object> response = new HashMap<>();
        response.put("errorCode", "RESOURCE_NOT_FOUND");
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("message", "Požadovaný prostředek nebyl nalezen: " + ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(Exception ex) {
        log.warn("Překročena maximální povolená velikost nahrávaného souboru / chyba multipartu: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("errorCode", ErrorCode.PAYLOAD_TOO_LARGE.name());
        response.put("status", HttpStatus.PAYLOAD_TOO_LARGE.value());
        response.put("message", "Velikost nahrávaného souboru překročila maximální povolený limit (max. 10 MB na soubor, 30 MB na požadavek).");
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
