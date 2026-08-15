package hanzner.zebrakapp.dto;

import hanzner.zebrakapp.entity.VoteType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Odpověď po hlasování o ověření nabídky")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponse {

    @Schema(description = "ID místa", example = "1")
    private Long placeId;

    @Schema(description = "Celkový počet kladných hlasů po započtení", example = "16")
    private Integer votesActive;

    @Schema(description = "Celkový počet záporných hlasů po započtení", example = "1")
    private Integer votesClosed;

    @Schema(description = "Aktuální hlas uživatele", example = "STILL_OPEN")
    private VoteType userVote;

    @Schema(description = "Informativní zpráva", example = "Hlas byl úspěšně zaznamenán")
    private String message;
}
