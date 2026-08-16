package hanzner.zebrakapp.controller;

import hanzner.zebrakapp.config.OpenApiConfig;
import hanzner.zebrakapp.dto.AuthRequest;
import hanzner.zebrakapp.dto.AuthResponse;
import hanzner.zebrakapp.dto.RegisterRequest;
import hanzner.zebrakapp.dto.UserDto;
import hanzner.zebrakapp.security.CustomUserDetails;
import hanzner.zebrakapp.security.JwtTokenProvider;
import hanzner.zebrakapp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Kontroler pro autentizaci a správu uživatelských sezení.
 * Zajišťuje registraci, přihlášení, odhlášení a zjištění profilu přihlášeného uživatele.
 */
@Tag(name = "1. Autentizace", description = "Endpointy pro registraci, přihlášení, odhlášení a získání profilu přihlášeného uživatele")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;
    private final hanzner.zebrakapp.service.TokenBlacklistService tokenBlacklistService;

    /**
     * Registrace nového uživatele do systému.
     */
    @Operation(
            summary = "Registrace nového uživatele",
            description = "Vytvoří nový uživatelský účet, nastaví HTTP-only autentizační cookie a vrátí JWT token spolu se základními informacemi o uživateli."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Uživatel byl úspěšně zaregistrován",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Neplatná vstupní data nebo e-mail již existuje",
                    content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        ResponseCookie cookie = tokenProvider.createJwtCookie(response.getToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    /**
     * Přihlášení existujícího uživatele.
     */
    @Operation(
            summary = "Přihlášení uživatele",
            description = "Ověří e-mail a heslo uživatele. Při úspěchu nastaví JWT cookie a vrátí autentizační odpověď s tokenem."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Úspěšné přihlášení",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Neplatné přihlašovací údaje (nesprávný e-mail nebo heslo)",
                    content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        ResponseCookie cookie = tokenProvider.createJwtCookie(response.getToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    /**
     * Odhlášení uživatele (zneplatnění tokenu v blacklistu a odstranění cookie).
     */
    @Operation(
            summary = "Odhlášení uživatele",
            description = "Zneplatní aktivní JWT token (zařadí jej na blacklist v Redisu do doby přirozené expirace) a smaže autentizační cookie."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Uživatel byl úspěšně odhlášen",
                    content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(jakarta.servlet.http.HttpServletRequest request) {
        String token = extractJwt(request);
        if (token != null) {
            long remainingMs = tokenProvider.getRemainingExpirationMs(token);
            if (remainingMs > 0) {
                tokenBlacklistService.blacklistToken(token, java.time.Duration.ofMillis(remainingMs));
            }
        }

        ResponseCookie cleanCookie = tokenProvider.createCleanJwtCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
                .body(Map.of("message", "Úspěšně odhlášeno"));
    }

    private String extractJwt(jakarta.servlet.http.HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("jwt_token".equals(cookie.getName()) && org.springframework.util.StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        String bearer = request.getHeader("Authorization");
        if (org.springframework.util.StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    /**
     * Získání profilu aktuálně přihlášeného uživatele.
     */
    @Operation(
            summary = "Profil aktuálně přihlášeného uživatele",
            description = "Vrátí detailní informace o přihlášeném uživateli na základě JWT tokenu.",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profil uživatele úspěšně načten",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "401", description = "Neautorizovaný přístup - uživatel není přihlášen",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(authService.getCurrentUser(userDetails.getUser()));
    }
}
