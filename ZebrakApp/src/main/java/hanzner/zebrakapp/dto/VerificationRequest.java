package hanzner.zebrakapp.dto;

import hanzner.zebrakapp.entity.VoteType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Požadavek na hlasování o platnosti nabídky")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequest {

    @Schema(description = "Hlas uživatele (STILL_OPEN = nabídka platí, CLOSED = nabídka již neplatí)", example = "STILL_OPEN", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Typ hlasu (STILL_OPEN nebo CLOSED) je povinný")
    private VoteType vote;
}
