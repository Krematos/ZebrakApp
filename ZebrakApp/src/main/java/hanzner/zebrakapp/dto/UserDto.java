package hanzner.zebrakapp.dto;

import hanzner.zebrakapp.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Schema(description = "Uživatelský profil")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    @Schema(description = "Unikátní ID uživatele", example = "1")
    private Long id;

    @Schema(description = "E-mailová adresa", example = "jan.novak@example.cz")
    private String email;

    @Schema(description = "Uživatelská přezdívka", example = "JanN")
    private String nickname;

    @Schema(description = "Role uživatele v systému", example = "ROLE_USER")
    private Role role;

    @Schema(description = "Datum a čas vytvoření účtu")
    private Instant createdAt;
}
