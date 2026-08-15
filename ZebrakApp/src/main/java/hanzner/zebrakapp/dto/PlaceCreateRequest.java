package hanzner.zebrakapp.dto;

import hanzner.zebrakapp.entity.Category;
import hanzner.zebrakapp.entity.DiscountType;
import hanzner.zebrakapp.entity.PriceLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Formulář pro vytvoření nového místa / nabídky slevy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceCreateRequest {

    @Schema(description = "Název místa / podniku", example = "Restaurace U Žebráka", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Název místa je povinný")
    private String title;

    @Schema(description = "Popis místa a nabízené slevy", example = "Každý čtvrtek od 17:00 pivo 1+1 zdarma a denní menu za polovic.")
    private String description;

    @Schema(description = "Kategorie podniku", example = "RESTAURACE", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Kategorie je povinná")
    private Category category;

    @Schema(description = "Cenová hladina", example = "CHEAP", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Cenová hladina je povinná")
    private PriceLevel priceLevel;

    @Schema(description = "Typ slevy / akce", example = "ONE_PLUS_ONE", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Typ slevy je povinný")
    private DiscountType discountType;

    @Schema(description = "Ulice a číslo popisné", example = "Nádražní 12", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Adresa je povinná")
    private String address;

    @Schema(description = "Město", example = "Praha", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Město je povinné")
    private String city;

    @Schema(description = "PSČ", example = "150 00")
    private String postalCode;

    @Schema(description = "Zeměpisná šířka (latitude)", example = "50.0755", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Zeměpisná šířka (latitude) je povinná")
    private Double latitude;

    @Schema(description = "Zeměpisná délka (longitude)", example = "14.4378", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Zeměpisná délka (longitude) je povinná")
    private Double longitude;

    @Schema(description = "Otevírací doba", example = "Po-Pá 10:00 - 22:00, So-Ne 11:00 - 23:00")
    private String openingHours;
}
