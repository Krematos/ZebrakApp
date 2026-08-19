package hanzner.zebrakapp.controller;

import hanzner.zebrakapp.config.OpenApiConfig;
import hanzner.zebrakapp.dto.DeleteAccountRequest;
import hanzner.zebrakapp.dto.PagedResponse;
import hanzner.zebrakapp.dto.PlaceResponse;
import hanzner.zebrakapp.security.CustomUserDetails;
import hanzner.zebrakapp.security.JwtTokenProvider;
import hanzner.zebrakapp.service.PlaceService;
import hanzner.zebrakapp.service.UserService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

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
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Získání všech míst vytvořených aktuálně přihlášeným uživatelem.
     */
    @Operation(
            summary = "Seznam vlastních míst uživatele",
            description = "Vrátí stránkovaný seznam všech míst vytvořených přihlášeným uživatelem bez ohledu na jejich schvalovací stav (PENDING, APPROVED, REJECTED)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stránkovaný seznam uživatelských míst úspěšně načten",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Neautorizovaný přístup - vyžadováno přihlášení",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/my-places")
    public ResponseEntity<PagedResponse<PlaceResponse>> getMyPlaces(
            @Parameter(description = "Číslo stránky (od 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Počet položek na stránku (1-100)", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        int validatedPage = Math.max(0, page);
        int validatedSize = Math.max(1, Math.min(size, 100));
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                validatedPage,
                validatedSize,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
        );

        return ResponseEntity.ok(placeService.getUserPlaces(userDetails.getUser(), pageable));
    }

    /**
     * Smazání vlastního uživatelského účtu (Soft Delete).
     */
    @Operation(
            summary = "Smazání vlastního účtu (Soft Delete)",
            description = "Označí účet jako smazaný po ověření zadaného stávajícího hesla a okamžitě zneplatní JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Účet byl úspěšně označen jako smazaný a token zneplatněn"),
            @ApiResponse(responseCode = "400", description = "Neplatné heslo nebo chyba validace",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Neautorizovaný přístup - vyžadováno přihlášení",
                    content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody DeleteAccountRequest request,
            HttpServletRequest httpRequest
    ) {
        String token = extractToken(httpRequest);
        userService.deleteMyAccount(userDetails.getUser(), request, token);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, jwtTokenProvider.createCleanJwtCookie().toString())
                .build();
    }

    private String extractToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("jwt_token".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
