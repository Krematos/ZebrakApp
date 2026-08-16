package hanzner.zebrakapp.security;

import hanzner.zebrakapp.entity.Role;
import hanzner.zebrakapp.entity.User;
import hanzner.zebrakapp.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Bezpečnostní filtry a Handlery Unit Testy")
class SecurityFiltersUnitTest {

    @Nested
    @DisplayName("CsrfCookieFilter Testy")
    class CsrfCookieFilterTests {

        @Test
        @DisplayName("Zavolá getToken() na odloženém CSRF tokenu a předá řízení dalšímu filtru")
        void testCsrfCookieFilter_CallsGetToken() throws ServletException, IOException {
            CsrfCookieFilter filter = new CsrfCookieFilter();
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain filterChain = mock(FilterChain.class);

            CsrfToken csrfToken = mock(CsrfToken.class);
            when(csrfToken.getToken()).thenReturn("mock-csrf-token");
            request.setAttribute(CsrfToken.class.getName(), csrfToken);

            filter.doFilter(request, response, filterChain);

            verify(csrfToken, times(1)).getToken();
            verify(filterChain, times(1)).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("CustomAccessDeniedHandler Testy")
    class CustomAccessDeniedHandlerTests {

        @Test
        @DisplayName("Vrátí HTTP 403 a standardní JSON chybovou odpověď")
        void testAccessDeniedHandler() throws ServletException, IOException {
            CustomAccessDeniedHandler handler = new CustomAccessDeniedHandler();
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            AccessDeniedException ex = new AccessDeniedException("Forbidden");

            handler.handle(request, response, ex);

            assertEquals(403, response.getStatus());
            assertTrue(response.getContentType().contains("application/json"));
            assertTrue(response.getContentAsString().contains("UNAUTHORIZED_ACCESS"));
        }
    }

    @Nested
    @DisplayName("JwtAuthenticationEntryPoint Testy")
    class JwtAuthenticationEntryPointTests {

        @Test
        @DisplayName("Vrátí HTTP 401 a standardní JSON chybovou odpověď")
        void testAuthenticationEntryPoint() throws ServletException, IOException {
            JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint();
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            AuthenticationException ex = new AuthenticationException("Unauthorized") {};

            entryPoint.commence(request, response, ex);

            assertEquals(401, response.getStatus());
            assertTrue(response.getContentType().contains("application/json"));
            assertTrue(response.getContentAsString().contains("UNAUTHORIZED"));
        }
    }

    @Nested
    @DisplayName("JwtAuthenticationFilter Testy")
    class JwtAuthenticationFilterTests {

        @Mock
        private JwtTokenProvider tokenProvider;

        @Mock
        private CustomUserDetailsService userDetailsService;

        @Mock
        private TokenBlacklistService tokenBlacklistService;

        private JwtAuthenticationFilter jwtFilter;

        private CustomUserDetails userDetails;

        @BeforeEach
        void setUp() {
            SecurityContextHolder.clearContext();
            jwtFilter = new JwtAuthenticationFilter(tokenProvider, userDetailsService, tokenBlacklistService);

            User user = User.builder()
                    .id(1L)
                    .email("filter.user@test.cz")
                    .password("secret")
                    .nickname("FilterUser")
                    .role(Role.ROLE_USER)
                    .active(true)
                    .build();
            userDetails = new CustomUserDetails(user);
        }

        @AfterEach
        void tearDown() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Autentizuje uživatele z cookie 'jwt_token'")
        void testFilter_AuthenticatesFromCookie() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie("jwt_token", "valid.jwt.cookie"));
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain filterChain = mock(FilterChain.class);

            when(tokenProvider.validateToken("valid.jwt.cookie")).thenReturn(true);
            when(tokenBlacklistService.isBlacklisted("valid.jwt.cookie")).thenReturn(false);
            when(tokenProvider.getEmailFromToken("valid.jwt.cookie")).thenReturn("filter.user@test.cz");
            when(userDetailsService.loadUserByUsername("filter.user@test.cz")).thenReturn(userDetails);

            jwtFilter.doFilter(request, response, filterChain);

            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
            assertEquals("filter.user@test.cz", SecurityContextHolder.getContext().getAuthentication().getName());
            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("Autentizuje uživatele z hlavičky Authorization Bearer")
        void testFilter_AuthenticatesFromBearerHeader() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer valid.jwt.header");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain filterChain = mock(FilterChain.class);

            when(tokenProvider.validateToken("valid.jwt.header")).thenReturn(true);
            when(tokenBlacklistService.isBlacklisted("valid.jwt.header")).thenReturn(false);
            when(tokenProvider.getEmailFromToken("valid.jwt.header")).thenReturn("filter.user@test.cz");
            when(userDetailsService.loadUserByUsername("filter.user@test.cz")).thenReturn(userDetails);

            jwtFilter.doFilter(request, response, filterChain);

            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("Neplatný token nenastaví autentizaci do SecurityContextu")
        void testFilter_InvalidToken_DoesNotAuthenticate() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer bad.token");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain filterChain = mock(FilterChain.class);

            when(tokenProvider.validateToken("bad.token")).thenReturn(false);

            jwtFilter.doFilter(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());
            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("Zneplatněný (blacklisted) token nenastaví autentizaci")
        void testFilter_BlacklistedToken_DoesNotAuthenticate() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer blacklisted.token");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain filterChain = mock(FilterChain.class);

            when(tokenProvider.validateToken("blacklisted.token")).thenReturn(true);
            when(tokenBlacklistService.isBlacklisted("blacklisted.token")).thenReturn(true);

            jwtFilter.doFilter(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());
            verify(filterChain, times(1)).doFilter(request, response);
        }
    }
}
