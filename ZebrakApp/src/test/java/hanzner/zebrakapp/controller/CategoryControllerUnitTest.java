package hanzner.zebrakapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("CategoryController Unit Testy")
class CategoryControllerUnitTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CategoryController controller = new CategoryController();
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    @DisplayName("GET /api/metadata/categories vrátí všechny kategorie s name, label a description")
    void testGetCategories() throws Exception {
        mockMvc.perform(get("/api/metadata/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].label").exists())
                .andExpect(jsonPath("$[0].description").exists());
    }

    @Test
    @DisplayName("GET /api/metadata/price-levels vrátí všechny cenové hladiny s name a label")
    void testGetPriceLevels() throws Exception {
        mockMvc.perform(get("/api/metadata/price-levels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].label").exists());
    }

    @Test
    @DisplayName("GET /api/metadata/discount-types vrátí všechny typy slev s name a label")
    void testGetDiscountTypes() throws Exception {
        mockMvc.perform(get("/api/metadata/discount-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].label").exists());
    }
}
