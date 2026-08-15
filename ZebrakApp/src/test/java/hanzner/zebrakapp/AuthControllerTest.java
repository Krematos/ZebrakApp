package hanzner.zebrakapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import hanzner.zebrakapp.dto.AuthRequest;
import hanzner.zebrakapp.dto.RegisterRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("Test")
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void testRegisterLoginAndCookieAuth() throws Exception {
        String testEmail = "testuser" + System.currentTimeMillis() + "@test.cz";
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(testEmail)
                .password("password123")
                .nickname("Testovac")
                .build();

        // 1. Registrace -> zkontroluje HttpOnly cookie
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("jwt_token"))
                .andExpect(cookie().httpOnly("jwt_token", true))
                .andExpect(jsonPath("$.user.email").value(testEmail))
                .andExpect(jsonPath("$.user.role").value("ROLE_USER"))
                .andReturn();

        Cookie jwtCookie = registerResult.getResponse().getCookie("jwt_token");

        // 2. Ověření autentizace přes samotnou cookie na /api/auth/me
        mockMvc.perform(get("/api/auth/me")
                        .cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testEmail));

        // 3. Přihlášení
        AuthRequest authRequest = AuthRequest.builder()
                .email(testEmail)
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("jwt_token"))
                .andExpect(cookie().httpOnly("jwt_token", true))
                .andExpect(jsonPath("$.user.email").value(testEmail));

        // 4. Logout -> smazání cookie (maxAge = 0)
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("jwt_token", 0));
    }

    @Test
    void testMetadataEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/api/metadata/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/metadata/price-levels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
