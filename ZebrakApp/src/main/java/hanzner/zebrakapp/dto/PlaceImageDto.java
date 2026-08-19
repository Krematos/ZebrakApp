package hanzner.zebrakapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Schema(description = "Informace o fotografii místa")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceImageDto {

    @Schema(description = "ID fotografie", example = "1")
    private Long id;

    @Schema(description = "Název souboru na disku", example = "a1b2c3d4-foto.jpg")
    private String filename;

    @Schema(description = "Veřejná URL adresa pro zobrazení fotografie", example = "/uploads/a1b2c3d4-foto.jpg")
    private String url;

    @Schema(description = "Příznak hlavní/úvodní fotografie místa", example = "true")
    private boolean isPrimary;

    @Schema(description = "Datum nahrání fotografie")
    private Instant createdAt;
}
