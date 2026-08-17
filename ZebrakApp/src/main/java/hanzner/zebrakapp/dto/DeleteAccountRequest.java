package hanzner.zebrakapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Požadavek na potvrzení smazání uživatelského účtu")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAccountRequest {

    @NotBlank(message = "Pro potvrzení smazání účtu je nutné zadat stávající heslo")
    @Schema(description = "Aktuální heslo uživatele pro ověření totožnosti", example = "MojeHeslo123")
    private String password;
}
