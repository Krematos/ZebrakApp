package hanzner.zebrakapp.controller;

import hanzner.zebrakapp.config.OpenApiConfig;
import hanzner.zebrakapp.dto.PlaceResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Kontroler pro uživatelské operace a správu vlastního obsahu.
 * Všechny endpointy vyžadují přihlášení uživatele (platný JWT token).
 */
@Tag(name = "3. Uživatelé", description = "Uživatelské operace, správa profilu a zobrazení vlastních vložených míst")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final PlaceService placeService;

    /**
     * Získání všech míst vytvořených aktuálně přihlášeným uživatelem.
     */
    @Operation(
            summary = "Seznam vlastních míst uživatele",
            description = "Vrátí všechna místa vytvořená přihlášeným uživatelem bez ohledu na jejich schvalovací stav (PENDING, APPROVED, REJECTED)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Seznam uživatelských míst úspěšně načten",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PlaceResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Neautorizovaný přístup - vyžadováno přihlášení",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/my-places")
    public ResponseEntity<List<PlaceResponse>> getMyPlaces(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(placeService.getUserPlaces(userDetails.getUser()));
    }
}
