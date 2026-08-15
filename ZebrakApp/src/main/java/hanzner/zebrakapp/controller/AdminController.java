package hanzner.zebrakapp.controller;

import hanzner.zebrakapp.config.OpenApiConfig;
import hanzner.zebrakapp.dto.AdminPlaceActionRequest;
import hanzner.zebrakapp.dto.PlaceResponse;
import hanzner.zebrakapp.entity.PlaceStatus;
import hanzner.zebrakapp.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Kontroler pro administrátorskou správu a moderaci míst.
 * Všechny endpointy vyžadují roli ROLE_ADMIN a platný JWT token.
 */
@Tag(name = "4. Administrace", description = "Endpointy pro administrátory – schvalování, zamítání a mazání vložených nabídek a míst")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * Získání všech míst čekajících na schválení (stav PENDING).
     */
    @Operation(
            summary = "Seznam míst ke schválení",
            description = "Vrátí seznam všech míst se stavem PENDING, která čekají na posouzení administrátorem."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Seznam čekajících míst úspěšně načten",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PlaceResponse.class)))),
            @ApiResponse(responseCode = "403", description = "Přístup odepřen - vyžadována role ROLE_ADMIN",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/places/pending")
    public ResponseEntity<List<PlaceResponse>> getPendingPlaces() {
        return ResponseEntity.ok(adminService.getPendingPlaces());
    }

    /**
     * Získání všech míst s volitelným filtrováním podle stavu.
     */
    @Operation(
            summary = "Seznam všech míst se stavem",
            description = "Vrátí všechna místa v systému s možností filtrování podle jejich stavu (PENDING, APPROVED, REJECTED)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Seznam míst úspěšně načten",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PlaceResponse.class)))),
            @ApiResponse(responseCode = "403", description = "Přístup odepřen - vyžadována role ROLE_ADMIN",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/places")
    public ResponseEntity<List<PlaceResponse>> getAllPlaces(
            @Parameter(description = "Stav místa (PENDING, APPROVED, REJECTED)")
            @RequestParam(required = false) PlaceStatus status
    ) {
        return ResponseEntity.ok(adminService.getAllPlaces(status));
    }

    /**
     * Schválení zadaného místa.
     */
    @Operation(
            summary = "Schválení místa",
            description = "Změní stav místa na APPROVED. Schválené místo se začne zobrazovat ve veřejném vyhledávání."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Místo bylo úspěšně schváleno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlaceResponse.class))),
            @ApiResponse(responseCode = "403", description = "Přístup odepřen - vyžadována role ROLE_ADMIN",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Místo nebylo nalezeno",
                    content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/places/{id}/approve")
    public ResponseEntity<PlaceResponse> approvePlace(
            @Parameter(description = "ID schvalovaného místa", example = "1")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(adminService.approvePlace(id));
    }

    /**
     * Zamítnutí zadaného místa s volitelným uvedením důvodu.
     */
    @Operation(
            summary = "Zamítnutí místa",
            description = "Změní stav místa na REJECTED a zaznamená důvod zamítnutí, který se zobrazí autorovi."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Místo bylo úspěšně zamítnuto",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlaceResponse.class))),
            @ApiResponse(responseCode = "403", description = "Přístup odepřen - vyžadována role ROLE_ADMIN",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Místo nebylo nalezeno",
                    content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/places/{id}/reject")
    public ResponseEntity<PlaceResponse> rejectPlace(
            @Parameter(description = "ID zamítaného místa", example = "1")
            @PathVariable Long id,

            @RequestBody(required = false) AdminPlaceActionRequest request
    ) {
        String reason = request != null ? request.getReason() : "Nespecifikováno";
        return ResponseEntity.ok(adminService.rejectPlace(id, reason));
    }

    /**
     * Trvalé smazání místa administrátorem.
     */
    @Operation(
            summary = "Trvalé smazání místa",
            description = "Trvale odstraní místo včetně všech jeho souborů a historie ověření z databáze i disku."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Místo bylo trvale odstraněno"),
            @ApiResponse(responseCode = "403", description = "Přístup odepřen - vyžadována role ROLE_ADMIN",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Místo nebylo nalezeno",
                    content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/places/{id}")
    public ResponseEntity<Void> deletePlace(
            @Parameter(description = "ID mazaného místa", example = "1")
            @PathVariable Long id
    ) {
        adminService.deletePlace(id);
        return ResponseEntity.noContent().build();
    }
}
