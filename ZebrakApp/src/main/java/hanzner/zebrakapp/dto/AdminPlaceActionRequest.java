package hanzner.zebrakapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
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
    @Size(max = 2000, message = "Důvod zamítnutí může mít maximálně 2000 znaků")
    private String reason;
}
