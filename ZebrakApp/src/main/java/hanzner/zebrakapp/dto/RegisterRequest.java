package hanzner.zebrakapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Požadavek na registraci nového uživatele")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @Schema(description = "Uživatelský e-mail", example = "jan.novak@example.cz", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "E-mail je povinný")
    @Email(message = "Zadejte platný formát e-mailu")
    private String email;

    @Schema(description = "Heslo uživatele (min. 6 znaků)", example = "MojeBezpecneHeslo123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Heslo je povinné")
    @Size(min = 6, message = "Heslo musí mít minimálně 6 znaků")
    private String password;

    @Schema(description = "Uživatelské jméno / přezdívka", example = "JanN", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Přezdívka / jméno je povinné")
    @Size(min = 2, max = 50, message = "Přezdívka musí mít 2 až 50 znaků")
    private String nickname;
}
