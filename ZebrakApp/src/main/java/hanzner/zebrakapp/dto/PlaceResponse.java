package hanzner.zebrakapp.dto;

import hanzner.zebrakapp.entity.Category;
import hanzner.zebrakapp.entity.DiscountType;
import hanzner.zebrakapp.entity.PlaceStatus;
import hanzner.zebrakapp.entity.PriceLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Detailní informace o místě / slevové nabídce")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceResponse {

    @Schema(description = "Unikátní ID místa", example = "1")
    private Long id;

    @Schema(description = "Název podniku / akce", example = "Restaurace U Žebráka")
    private String title;

    @Schema(description = "Popis místa a akce", example = "Pivo 1+1 zdarma každý čtvrtek")
    private String description;

    @Schema(description = "Kód kategorie", example = "RESTAURACE")
    private Category category;

    @Schema(description = "Lokalizovaný název kategorie", example = "Restaurace")
    private String categoryLabel;

    @Schema(description = "Kód cenové hladiny", example = "CHEAP")
    private PriceLevel priceLevel;

    @Schema(description = "Lokalizovaný název cenové hladiny", example = "Levné (€)")
    private String priceLevelLabel;

    @Schema(description = "Kód typu slevy", example = "ONE_PLUS_ONE")
    private DiscountType discountType;

    @Schema(description = "Lokalizovaný název typu slevy", example = "1+1 zdarma")
    private String discountTypeLabel;

    @Schema(description = "Ulice a číslo popisné", example = "Nádražní 12")
    private String address;

    @Schema(description = "Město", example = "Praha")
    private String city;

    @Schema(description = "PSČ", example = "150 00")
    private String postalCode;

    @Schema(description = "Zeměpisná šířka (latitude)", example = "50.0755")
    private Double latitude;

    @Schema(description = "Zeměpisná délka (longitude)", example = "14.4378")
    private Double longitude;

    @Schema(description = "Otevírací doba", example = "Po-Pá 10:00 - 22:00")
    private String openingHours;

    @Schema(description = "Stav schválení administrátorem", example = "APPROVED")
    private PlaceStatus status;

    @Schema(description = "Počet kladných hlasů pro platnost nabídky", example = "15")
    private Integer votesActive;

    @Schema(description = "Počet záporných hlasů (neplatná / ukončená nabídka)", example = "1")
    private Integer votesClosed;

    @Schema(description = "Hlas aktuálně dotazujícího uživatele/IP (UP, DOWN nebo null)", example = "UP")
    private String userVote;

    @Schema(description = "Důvod zamítnutí (pokud je status REJECTED)", example = "Neúplné informace o podniku")
    private String rejectionReason;

    @Schema(description = "Autor záznamu")
    private UserDto author;

    @Schema(description = "Seznam fotografií místa")
    private List<PlaceImageDto> images;

    @Schema(description = "Datum a čas vytvoření záznamu")
    private LocalDateTime createdAt;

    @Schema(description = "Datum a čas poslední úpravy záznamu")
    private LocalDateTime updatedAt;
}
