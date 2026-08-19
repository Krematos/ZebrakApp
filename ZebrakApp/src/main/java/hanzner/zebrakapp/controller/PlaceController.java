package hanzner.zebrakapp.controller;

import hanzner.zebrakapp.config.OpenApiConfig;
import hanzner.zebrakapp.dto.*;
import hanzner.zebrakapp.entity.Category;
import hanzner.zebrakapp.entity.DiscountType;
import hanzner.zebrakapp.entity.PriceLevel;
import hanzner.zebrakapp.entity.User;
import hanzner.zebrakapp.security.CustomUserDetails;
import hanzner.zebrakapp.service.PlaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Kontroler pro správu, vyhledávání a komunitní ověřování míst a slev.
 */
@Tag(name = "2. Místa a slevy", description = "Endpointy pro vyhledávání, vytváření, editaci, nahrávání obrázků a hlasování o platnosti nabídek")
@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    /**
     * Vyhledávání schválených míst a slev podle různých kritérií.
     */
    @Operation(
            summary = "Vyhledávání schválených míst a slev",
            description = "Vrátí stránkovaný seznam schválených míst (APPROVED) s možností filtrování podle kategorie, cenové hladiny, typu slevy, souřadnicového obdélníku (bounding box pro mapu), fulltextového vyhledávání a stránkování."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stránkovaný seznam míst odpovídajících filtrům",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagedResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PagedResponse<PlaceResponse>> searchPlaces(
            @Parameter(description = "Kategorie podniku (např. FOOD, SECOND_HAND, OUTLET, ATD.)")
            @RequestParam(required = false) Category category,

            @Parameter(description = "Cenová hladina (LOW, MEDIUM, HIGH)")
            @RequestParam(required = false) PriceLevel priceLevel,

            @Parameter(description = "Typ slevy / akce (PERMANENT, ONE_TIME, ATD.)")
            @RequestParam(required = false) DiscountType discountType,

            @Parameter(description = "Minimální zeměpisná šířka pro výřez mapy (bounding box)")
            @RequestParam(required = false) Double minLat,

            @Parameter(description = "Maximální zeměpisná šířka pro výřez mapy (bounding box)")
            @RequestParam(required = false) Double maxLat,

            @Parameter(description = "Minimální zeměpisná délka pro výřez mapy (bounding box)")
            @RequestParam(required = false) Double minLng,

            @Parameter(description = "Maximální zeměpisná délka pro výřez mapy (bounding box)")
            @RequestParam(required = false) Double maxLng,

            @Parameter(description = "Vyhledávací textový dotaz (název, popis, město apod.)")
            @RequestParam(required = false) String q,

            @Parameter(description = "Číslo stránky (od 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Počet položek na stránku (1-100)", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Pole pro řazení", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Směr řazení (ASC, DESC)", example = "DESC")
            @RequestParam(defaultValue = "DESC") String sortDir,

            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(hidden = true)
            HttpServletRequest request
    ) {
        User currentUser = userDetails != null ? userDetails.getUser() : null;
        String ipAddress = request.getRemoteAddr();

        int validatedPage = Math.max(0, page);
        int validatedSize = Math.max(1, Math.min(size, 100));
        org.springframework.data.domain.Sort.Direction direction = "ASC".equalsIgnoreCase(sortDir)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                validatedPage,
                validatedSize,
                org.springframework.data.domain.Sort.by(direction, sortBy)
        );

        PagedResponse<PlaceResponse> places = placeService.searchApprovedPlaces(
                category, priceLevel, discountType, minLat, maxLat, minLng, maxLng, q, currentUser, ipAddress, pageable
        );
        return ResponseEntity.ok(places);
    }

    /**
     * Získání detailu místa podle jeho ID.
     */
    @Operation(
            summary = "Detail místa podle ID",
            description = "Získá detailní informace o konkrétním místě včetně obrázků a stavu hlasování pro aktuálního uživatele nebo IP adresu."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Místo nalezeno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlaceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Místo s daným ID neexistuje",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{id}")
    public ResponseEntity<PlaceResponse> getPlaceById(
            @Parameter(description = "ID místa", example = "1")
            @PathVariable Long id,

            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(hidden = true)
            HttpServletRequest request
    ) {
        User currentUser = userDetails != null ? userDetails.getUser() : null;
        String ipAddress = request.getRemoteAddr();

        return ResponseEntity.ok(placeService.getPlaceById(id, currentUser, ipAddress));
    }

    /**
     * Vytvoření nového místa / slevy přihlášeným uživatelem.
     */
    @Operation(
            summary = "Vytvoření nového místa",
            description = "Vytvoří nové místo a zařadí jej do stavu PENDING ke schválení administrátorem.",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Místo úspěšně vytvořeno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlaceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Neplatná vstupní data",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Neautorizovaný přístup - vyžadováno přihlášení",
                    content = @Content(mediaType = "application/json"))
    })
    @PostMapping
    public ResponseEntity<PlaceResponse> createPlace(
            @Valid @RequestBody PlaceCreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(placeService.createPlace(request, userDetails.getUser()));
    }

    /**
     * Úprava existujícího místa.
     */
    @Operation(
            summary = "Úprava existujícího místa",
            description = "Upraví údaje místa. Povoleno pouze autorovi místa nebo administrátorovi.",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Místo úspěšně upraveno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlaceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Neplatná vstupní data",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Nemáte oprávnění upravovat toto místo",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Místo nebylo nalezeno",
                    content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}")
    public ResponseEntity<PlaceResponse> updatePlace(
            @Parameter(description = "ID upravovaného místa", example = "1")
            @PathVariable Long id,

            @Valid @RequestBody PlaceUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(placeService.updatePlace(id, request, userDetails.getUser()));
    }

    /**
     * Nahrání fotografií k místu.
     */
    @Operation(
            summary = "Nahrání fotografií k místu",
            description = "Nahraje jeden nebo více obrázků k danému místu (formát multipart/form-data).",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fotografie úspěšně nahrány",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PlaceImageDto.class)))),
            @ApiResponse(responseCode = "400", description = "Neplatný soubor nebo překročena velikost",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Nemáte oprávnění nahrávat fotky k tomuto místu",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Místo nebylo nalezeno",
                    content = @Content(mediaType = "application/json"))
    })
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<PlaceImageDto>> uploadImages(
            @Parameter(description = "ID místa", example = "1")
            @PathVariable Long id,

            @Parameter(description = "Seznam obrázků k nahrání")
            @RequestParam("files") List<MultipartFile> files,

            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(placeService.uploadImages(id, files, userDetails.getUser()));
    }

    /**
     * Smazání konkrétní fotografie místa.
     */
    @Operation(
            summary = "Smazání fotografie",
            description = "Smaže zadanou fotografii patřící k danému místu.",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Fotografie byla úspěšně smazána"),
            @ApiResponse(responseCode = "403", description = "Nemáte oprávnění smazat tuto fotografii",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Místo nebo fotografie nenalezeny",
                    content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/{placeId}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @Parameter(description = "ID místa", example = "1")
            @PathVariable Long placeId,

            @Parameter(description = "ID mazané fotografie", example = "5")
            @PathVariable Long imageId,

            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        placeService.deleteImage(placeId, imageId, userDetails.getUser());
        return ResponseEntity.noContent().build();
    }

    /**
     * Hlasování o platnosti nabídky (komunitní ověřování).
     */
    @Operation(
            summary = "Hlasování o platnosti nabídky",
            description = "Zahlasuje, zda je sleva/nabídka stále platná (UP) nebo již neplatná (DOWN). Lze provést i nepřihlášeným uživatelem (eviduje se IP)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hlas byl úspěšně zaznamenán",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = VerificationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Neplatný požadavek nebo nepovolená hodnota hlasu",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Místo nebylo nalezeno",
                    content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/{id}/verify")
    public ResponseEntity<VerificationResponse> verifyPlace(
            @Parameter(description = "ID místa", example = "1")
            @PathVariable Long id,

            @Valid @RequestBody VerificationRequest request,

            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(hidden = true)
            HttpServletRequest servletRequest
    ) {
        User currentUser = userDetails != null ? userDetails.getUser() : null;
        String ipAddress = servletRequest.getRemoteAddr();

        return ResponseEntity.ok(placeService.verifyPlace(id, request.getVote(), currentUser, ipAddress));
    }
}
