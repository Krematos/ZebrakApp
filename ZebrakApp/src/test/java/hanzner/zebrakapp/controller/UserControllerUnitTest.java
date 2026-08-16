package hanzner.zebrakapp.controller;

import hanzner.zebrakapp.dto.PlaceResponse;
import hanzner.zebrakapp.entity.Category;
import hanzner.zebrakapp.entity.PlaceStatus;
import hanzner.zebrakapp.entity.Role;
import hanzner.zebrakapp.entity.User;
import hanzner.zebrakapp.security.CustomUserDetails;
import hanzner.zebrakapp.service.PlaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Unit Testy")
class UserControllerUnitTest {

    @Mock
    private PlaceService placeService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private CustomUserDetails userPrincipal;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(1L)
                .email("moje@konto.cz")
                .nickname("Majitel")
                .role(Role.ROLE_USER)
                .active(true)
                .build();
        userPrincipal = new CustomUserDetails(user);

        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().equals(CustomUserDetails.class)
                                && parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return userPrincipal;
                    }
                })
                .build();
    }

    @Test
    @DisplayName("GET /api/users/my-places vrátí místa přihlášeného uživatele")
    void testGetMyPlaces_ReturnsUserPlaces() throws Exception {
        PlaceResponse myPlace = PlaceResponse.builder()
                .id(5L)
                .title("Moje oblíbené potraviny")
                .status(PlaceStatus.APPROVED)
                .category(Category.FOOD)
                .build();

        when(placeService.getUserPlaces(any(User.class))).thenReturn(List.of(myPlace));

        mockMvc.perform(get("/api/users/my-places"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].title").value("Moje oblíbené potraviny"));
    }
}
