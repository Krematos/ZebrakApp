package hanzner.zebrakapp.controller;

import hanzner.zebrakapp.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("MaxUploadSizeExceededException vrátí HTTP 413 PAYLOAD_TOO_LARGE s odpovídajícím JSON")
    void testHandleMaxUploadSizeExceeded() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(10 * 1024 * 1024);

        ResponseEntity<Map<String, Object>> response = handler.handleMaxUploadSizeExceeded(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("errorCode")).isEqualTo(ErrorCode.PAYLOAD_TOO_LARGE.name());
        assertThat(response.getBody().get("status")).isEqualTo(413);
        assertThat(response.getBody().get("message")).toString().contains("překročila maximální povolený limit");
    }

    @Test
    @DisplayName("NoResourceFoundException vrátí HTTP 404 NOT_FOUND s RESOURCE_NOT_FOUND")
    void testHandleNoResourceFound() {
        NoResourceFoundException ex = new NoResourceFoundException(GET, "/favicon.ico", "No static resource favicon.ico");

        ResponseEntity<Map<String, Object>> response = handler.handleNoResourceFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("errorCode")).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().get("status")).isEqualTo(404);
    }
}
