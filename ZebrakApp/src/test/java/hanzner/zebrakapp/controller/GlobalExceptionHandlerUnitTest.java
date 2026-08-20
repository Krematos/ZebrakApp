package hanzner.zebrakapp.controller;

import hanzner.zebrakapp.entity.Category;
import hanzner.zebrakapp.entity.PriceLevel;
import hanzner.zebrakapp.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GlobalExceptionHandler Unit Testy")
class GlobalExceptionHandlerUnitTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleBaseException zpracuje PlaceNotFoundException (404)")
    void testHandleBaseException_PlaceNotFound() {
        PlaceNotFoundException ex = new PlaceNotFoundException(10L);

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleBaseException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("PLACE_NOT_FOUND", response.getBody().get("errorCode"));
        assertEquals(404, response.getBody().get("status"));
        assertTrue(response.getBody().get("message").toString().contains("10"));
    }

    @Test
    @DisplayName("handleBaseException zpracuje UserAlreadyExistException (409)")
    void testHandleBaseException_UserAlreadyExists() {
        UserAlreadyExistException ex = new UserAlreadyExistException("E-mail již existuje");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleBaseException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("USER_ALREADY_EXISTS", response.getBody().get("errorCode"));
        assertEquals(409, response.getBody().get("status"));
    }

    @Test
    @DisplayName("handleBaseException zpracuje UnauthorizedActionException (403)")
    void testHandleBaseException_UnauthorizedAction() {
        UnauthorizedActionException ex = new UnauthorizedActionException("Nemáte práva");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleBaseException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("UNAUTHORIZED_ACCESS", response.getBody().get("errorCode"));
        assertEquals(403, response.getBody().get("status"));
    }

    @Test
    @DisplayName("handleValidationExceptions zpracuje chyby validačních anotací (400)")
    void testHandleValidationExceptions() throws NoSuchMethodException {
        Method method = this.getClass().getDeclaredMethod("setUp");
        MethodParameter parameter = new MethodParameter(method, -1);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "email", "Neplatný formát e-mailu"));
        bindingResult.addError(new FieldError("target", "password", "Heslo je příliš krátké"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleValidationExceptions(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", response.getBody().get("errorCode"));
        assertEquals(400, response.getBody().get("status"));

        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) response.getBody().get("errors");
        assertNotNull(errors);
        assertEquals("Neplatný formát e-mailu", errors.get("email"));
        assertEquals("Heslo je příliš krátké", errors.get("password"));
    }

    @Test
    @DisplayName("handleBadCredentials vrátí 401 UNAUTHORIZED")
    void testHandleBadCredentials() {
        BadCredentialsException ex = new BadCredentialsException("Špatné heslo");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleBadCredentials(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("BAD_CREDENTIALS", response.getBody().get("errorCode"));
        assertEquals(401, response.getBody().get("status"));
    }

    @Test
    @DisplayName("handleAccessDenied vrátí 403 FORBIDDEN")
    void testHandleAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("Přístup odepřen");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleAccessDenied(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("UNAUTHORIZED_ACCESS", response.getBody().get("errorCode"));
        assertEquals(403, response.getBody().get("status"));
    }

    @Test
    @DisplayName("handleIllegalArgument vrátí 400 BAD_REQUEST")
    void testHandleIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Neplatný argument");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleIllegalArgument(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", response.getBody().get("errorCode"));
        assertEquals(400, response.getBody().get("status"));
    }

    @Test
    @DisplayName("handleSecurity vrátí 403 FORBIDDEN")
    void testHandleSecurity() {
        SecurityException ex = new SecurityException("Bezpečnostní porušení");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleSecurity(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("UNAUTHORIZED_ACCESS", response.getBody().get("errorCode"));
        assertEquals(403, response.getBody().get("status"));
    }

    @Test
    @DisplayName("handleMethodArgumentTypeMismatch pro enum vrátí 400 s popisem povolených hodnot")
    void testHandleMethodArgumentTypeMismatch_Enum() {
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex =
                new org.springframework.web.method.annotation.MethodArgumentTypeMismatchException(
                        "SPATNA_KATEGORIE",
                        Category.class,
                        "category",
                        null,
                        null
                );

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleMethodArgumentTypeMismatch(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", response.getBody().get("errorCode"));
        assertEquals(400, response.getBody().get("status"));
        assertTrue(response.getBody().get("message").toString().contains("Neplatná hodnota 'SPATNA_KATEGORIE' pro parametr 'category'"));
        assertTrue(response.getBody().get("message").toString().contains("FOOD"));
    }

    @Test
    @DisplayName("handleMethodArgumentTypeMismatch pro non-enum vrátí 400 s popisem očekávaného typu")
    void testHandleMethodArgumentTypeMismatch_NonEnum() {
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex =
                new org.springframework.web.method.annotation.MethodArgumentTypeMismatchException(
                        "neni_cislo",
                        Long.class,
                        "id",
                        null,
                        null
                );

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleMethodArgumentTypeMismatch(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", response.getBody().get("errorCode"));
        assertTrue(response.getBody().get("message").toString().contains("Neplatná hodnota 'neni_cislo' pro parametr 'id'"));
    }

    @Test
    @DisplayName("handleHttpMessageNotReadable s InvalidFormatException vrátí 400 s popisem pole")
    void testHandleHttpMessageNotReadable_InvalidFormatException() {
        com.fasterxml.jackson.databind.exc.InvalidFormatException ife =
                new com.fasterxml.jackson.databind.exc.InvalidFormatException(
                        null,
                        "Cannot deserialize value",
                        "NEPLATNA_CENA",
                        PriceLevel.class
                );
        ife.prependPath(new com.fasterxml.jackson.databind.JsonMappingException.Reference(null, "priceLevel"));

        org.springframework.http.converter.HttpMessageNotReadableException ex =
                new org.springframework.http.converter.HttpMessageNotReadableException("JSON parse error", ife, null);

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleHttpMessageNotReadable(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", response.getBody().get("errorCode"));
        assertTrue(response.getBody().get("message").toString().contains("Neplatná hodnota 'NEPLATNA_CENA' pro pole 'priceLevel'"));
        assertTrue(response.getBody().get("message").toString().contains("LOW"));
    }

    @Test
    @DisplayName("handleMissingServletRequestParameter vrátí 400 s názvem chybějícího parametru")
    void testHandleMissingServletRequestParameter() {
        org.springframework.web.bind.MissingServletRequestParameterException ex =
                new org.springframework.web.bind.MissingServletRequestParameterException("q", "String");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleMissingServletRequestParameter(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", response.getBody().get("errorCode"));
        assertTrue(response.getBody().get("message").toString().contains("q"));
    }

    @Test
    @DisplayName("handleMaxUploadSizeExceeded vrátí 413 PAYLOAD_TOO_LARGE")
    void testHandleMaxUploadSizeExceeded() {
        org.springframework.web.multipart.MaxUploadSizeExceededException ex =
                new org.springframework.web.multipart.MaxUploadSizeExceededException(10 * 1024 * 1024);

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleMaxUploadSizeExceeded(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertEquals("PAYLOAD_TOO_LARGE", response.getBody().get("errorCode"));
        assertEquals(413, response.getBody().get("status"));
        assertTrue(response.getBody().get("message").toString().contains("překročila maximální povolený limit"));
    }

    @Test
    @DisplayName("handleGeneralException vrátí 500 INTERNAL_SERVER_ERROR")
    void testHandleGeneralException() {
        RuntimeException ex = new RuntimeException("Neočekávaná chyba");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGeneralException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().get("errorCode"));
        assertEquals(500, response.getBody().get("status"));
        assertTrue(response.getBody().get("message").toString().contains("Neočekávaná chyba"));
    }
}
