package hanzner.zebrakapp.controller;

import hanzner.zebrakapp.entity.Category;
import hanzner.zebrakapp.entity.DiscountType;
import hanzner.zebrakapp.entity.PriceLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Kontroler pro poskytování metadat a číselníků (kategorie, cenové hladiny, typy slev).
 * Slouží především pro frontend formuláře a filtry. Všechny endpointy jsou veřejné.
 */
@Tag(name = "5. Metadata a číselníky", description = "Veřejné číselníky a výčtové hodnoty pro filtry a editační formuláře")
@RestController
@RequestMapping("/api/metadata")
public class CategoryController {

    /**
     * Získání všech kategorií míst včetně jejich lokalizovaných názvů a popisů.
     */
    @Operation(
            summary = "Seznam kategorií míst",
            description = "Vrátí všechny podporované kategorie (např. RESTAURACE, PUB, KAVARNA, FAST_FOOD, PEKARNA, ATD.) s českým názvem a popisem."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Seznam kategorií úspěšně načten",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(example = "{\"name\":\"RESTAURACE\",\"label\":\"Restaurace\",\"description\":\"Klasické i moderní restaurace a bistra\"}"))))
    })
    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, String>>> getCategories() {
        List<Map<String, String>> categories = Arrays.stream(Category.values())
                .map(c -> Map.of(
                        "name", c.name(),
                        "label", c.getLabel(),
                        "description", c.getDescription()
                ))
                .toList();
        return ResponseEntity.ok(categories);
    }

    /**
     * Získání všech cenových hladin.
     */
    @Operation(
            summary = "Seznam cenových hladin",
            description = "Vrátí dostupné cenové úrovně (CHEAP, MEDIUM, EXPENSIVE) včetně jejich zobrazení (např. 'Levné (€)')."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Seznam cenových hladin úspěšně načten",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(example = "{\"name\":\"CHEAP\",\"label\":\"Levné (€)\"}"))))
    })
    @GetMapping("/price-levels")
    public ResponseEntity<List<Map<String, String>>> getPriceLevels() {
        List<Map<String, String>> priceLevels = Arrays.stream(PriceLevel.values())
                .map(p -> Map.of(
                        "name", p.name(),
                        "label", p.getLabel()
                ))
                .toList();
        return ResponseEntity.ok(priceLevels);
    }

    /**
     * Získání všech typů slev a akčních nabídek.
     */
    @Operation(
            summary = "Seznam typů slev",
            description = "Vrátí podporované typy slev a akcí (PERCENT_OFF, FIXED_AMOUNT, ONE_PLUS_ONE, HAPPY_HOUR, STUDENT, LOYALTY, OTHER) s českými popisky."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Seznam typů slev úspěšně načten",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(example = "{\"name\":\"PERCENT_OFF\",\"label\":\"Procentuální sleva\"}"))))
    })
    @GetMapping("/discount-types")
    public ResponseEntity<List<Map<String, String>>> getDiscountTypes() {
        List<Map<String, String>> discountTypes = Arrays.stream(DiscountType.values())
                .map(d -> Map.of(
                        "name", d.name(),
                        "label", d.getLabel()
                ))
                .toList();
        return ResponseEntity.ok(discountTypes);
    }
}
