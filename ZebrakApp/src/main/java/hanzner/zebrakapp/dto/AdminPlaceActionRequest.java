package hanzner.zebrakapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Požadavek administrátora s důvodem zamítnutí místa")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPlaceActionRequest {

    @Schema(description = "Důvod zamítnutí místa", example = "Neplatná adresa nebo duplicitní záznam.")
    private String reason;
}
