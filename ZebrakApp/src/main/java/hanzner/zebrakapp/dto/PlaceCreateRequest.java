package hanzner.zebrakapp.dto;

import hanzner.zebrakapp.entity.Category;
import hanzner.zebrakapp.entity.DiscountType;
import hanzner.zebrakapp.entity.PriceLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @Size(min = 2, max = 255, message = "Název místa musí mít 2 až 255 znaků")
    private String title;

    @Schema(description = "Popis místa a nabízené slevy", example = "Každý čtvrtek od 17:00 pivo 1+1 zdarma a denní menu za polovic.")
    @Size(max = 5000, message = "Popis místa může mít maximálně 5000 znaků")
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
    @Size(min = 2, max = 255, message = "Adresa musí mít 2 až 255 znaků")
    private String address;

    @Schema(description = "Město", example = "Praha", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Město je povinné")
    @Size(min = 2, max = 100, message = "Název města musí mít 2 až 100 znaků")
    private String city;

    @Schema(description = "PSČ", example = "150 00")
    @Size(max = 20, message = "PSČ může mít maximálně 20 znaků")
    private String postalCode;

    @Schema(description = "Zeměpisná šířka (latitude)", example = "50.0755", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Zeměpisná šířka (latitude) je povinná")
    @DecimalMin(value = "-90.0", message = "Zeměpisná šířka musí být v rozmezí -90.0 až 90.0")
    @DecimalMax(value = "90.0", message = "Zeměpisná šířka musí být v rozmezí -90.0 až 90.0")
    private Double latitude;

    @Schema(description = "Zeměpisná délka (longitude)", example = "14.4378", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Zeměpisná délka (longitude) je povinná")
    @DecimalMin(value = "-180.0", message = "Zeměpisná délka musí být v rozmezí -180.0 až 180.0")
    @DecimalMax(value = "180.0", message = "Zeměpisná délka musí být v rozmezí -180.0 až 180.0")
    private Double longitude;

    @Schema(description = "Otevírací doba", example = "Po-Pá 10:00 - 22:00, So-Ne 11:00 - 23:00")
    @Size(max = 255, message = "Otevírací doba může mít maximálně 255 znaků")
    private String openingHours;
}
