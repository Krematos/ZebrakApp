package hanzner.zebrakapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Požadavek na přihlášení uživatele")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    @Schema(description = "Uživatelský e-mail", example = "jan.novak@example.cz", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "E-mail je povinný")
    @Email(message = "Zadejte platný formát e-mailu")
    private String email;

    @Schema(description = "Heslo uživatele", example = "TajneHeslo123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Heslo je povinné")
    private String password;
}
