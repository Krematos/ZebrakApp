package hanzner.zebrakapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Odpověď po úspěšné autentizaci")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    @Schema(description = "JWT Bearer autentizační token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "Typ tokenu", example = "Bearer")
    private String tokenType;

    @Schema(description = "Informace o přihlášeném uživateli")
    private UserDto user;
}
