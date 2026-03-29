package com.example.catalog_manager.controller;

import com.example.catalog_manager.Repository.ProducerRepository;
import com.example.catalog_manager.Repository.ProductCategoryRepository;
import com.example.catalog_manager.Repository.ProductRepository;
import com.example.catalog_manager.entity.Producer;
import com.example.catalog_manager.entity.Product;
import com.example.catalog_manager.entity.ProductAttributes;
import com.example.catalog_manager.entity.ProductCategory;
import com.example.catalog_manager.enums.Currency;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProducerRepository producerRepository;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Producer samsung;
    private Producer lg;
    private ProductCategory tvCategory;
    private ProductCategory smartphoneCategory;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        samsung = producerRepository.save(Producer.builder()
                .name("Samsung")
                .country("South Korea")
                .email("contact@samsung.com")
                .build());

        lg = producerRepository.save(Producer.builder()
                .name("LG")
                .country("South Korea")
                .email("contact@lg.com")
                .build());

        tvCategory = categoryRepository.save(ProductCategory.builder()
                .name("TV")
                .requiredAttributes(Set.of("screen_size", "resolution"))
                .build());

        smartphoneCategory = categoryRepository.save(ProductCategory.builder()
                .name("Smartphone")
                .requiredAttributes(Set.of("battery_capacity"))
                .build());

        // Samsung TVs
        saveProduct("Samsung QLED 55", new BigDecimal("2999.99"), Currency.PLN, samsung, tvCategory,
                Map.of("screen_size", "55", "resolution", "4K", "color", "black"));

        saveProduct("Samsung QLED 65", new BigDecimal("4999.99"), Currency.PLN, samsung, tvCategory,
                Map.of("screen_size", "65", "resolution", "8K", "color", "silver"));

        // Samsung Smartphone
        saveProduct("Samsung Galaxy S24", new BigDecimal("3499.99"), Currency.PLN, samsung, smartphoneCategory,
                Map.of("battery_capacity", "5000", "color", "black"));

        // LG TV
        saveProduct("LG OLED 55", new BigDecimal("3499.99"), Currency.EUR, lg, tvCategory,
                Map.of("screen_size", "55", "resolution", "4K", "color", "white"));

        // LG Smartphone
        saveProduct("LG Velvet", new BigDecimal("1999.99"), Currency.EUR, lg, smartphoneCategory,
                Map.of("battery_capacity", "4300", "color", "pink"));
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    // ---- filter by producer ----

    @Test
    void getAllProducts_filterByProducerId_shouldReturnOnlySamsungProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("producerId", samsung.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].producer.name",
                        everyItem(equalTo("Samsung"))));
    }

    @Test
    void getAllProducts_filterByProducerId_whenNoProducts_shouldReturnEmpty() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("producerId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    // ---- filter by category ----

    @Test
    void getAllProducts_filterByCategoryId_shouldReturnOnlyTVs() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("categoryId", tvCategory.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].category.name",
                        everyItem(equalTo("TV"))));
    }

    // ---- filter by name ----

    @Test
    void getAllProducts_filterByName_shouldReturnMatchingProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("name", "QLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].name",
                        everyItem(containsString("QLED"))));
    }

    @Test
    void getAllProducts_filterByNameCaseInsensitive_shouldReturnResults() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("name", "qled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllProducts_filterByName_whenNoMatch_shouldReturnEmpty() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("name", "Sony"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ---- filter by price ----

    @Test
    void getAllProducts_filterByMinPrice_shouldReturnExpensiveProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("minPrice", "4000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Samsung QLED 65"));
    }

    @Test
    void getAllProducts_filterByMaxPrice_shouldReturnCheapProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("maxPrice", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("LG Velvet"));
    }

    @Test
    void getAllProducts_filterByPriceRange_shouldReturnProductsInRange() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("minPrice", "2500")
                        .param("maxPrice", "3500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    // ---- filter by currency ----

    @Test
    void getAllProducts_filterByCurrency_shouldReturnOnlyPLNProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("currency", "PLN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].currency",
                        everyItem(equalTo("PLN"))));
    }

    @Test
    void getAllProducts_filterByCurrencyEUR_shouldReturnOnlyEURProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("currency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].currency",
                        everyItem(equalTo("EUR"))));
    }


    // ---- filter combinations ----

    @Test
    void getAllProducts_filterByProducerAndCategory_shouldReturnCorrectProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("producerId", samsung.getId().toString())
                        .param("categoryId", tvCategory.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].producer.name",
                        everyItem(equalTo("Samsung"))))
                .andExpect(jsonPath("$.content[*].category.name",
                        everyItem(equalTo("TV"))));
    }

    @Test
    void getAllProducts_filterByProducerAndPriceRange_shouldReturnCorrectProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("producerId", samsung.getId().toString())
                        .param("minPrice", "3000")
                        .param("maxPrice", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2)); // QLED 65 i Galaxy S24
    }

    @Test
    void getAllProducts_allFilters_shouldNarrowResultsCorrectly() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("producerId", samsung.getId().toString())
                        .param("categoryId", tvCategory.getId().toString())
                        .param("minPrice", "4000")
                        .param("currency", "PLN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Samsung QLED 65"));
    }

    // ---- sorting ----

    @Test
    void getAllProducts_sortByPriceAsc_shouldReturnCheapestFirst() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("sortBy", "PRICE")
                        .param("sortDir", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("LG Velvet"))
                .andExpect(jsonPath("$.content[4].name").value("Samsung QLED 65"));
    }

    @Test
    void getAllProducts_sortByPriceDesc_shouldReturnMostExpensiveFirst() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("sortBy", "PRICE")
                        .param("sortDir", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Samsung QLED 65"));
    }

    @Test
    void getAllProducts_sortByNameAsc_shouldReturnAlphabetically() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("sortBy", "NAME")
                        .param("sortDir", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("LG OLED 55"))
                .andExpect(jsonPath("$.content[1].name").value("LG Velvet"));
    }

    // ---- pagination ----

    @Test
    void getAllProducts_withPagination_shouldReturnCorrectPage() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sortBy", "PRICE")
                        .param("sortDir", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void getAllProducts_lastPage_shouldHaveLastTrue() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("page", "2")
                        .param("size", "2")
                        .param("sortBy", "PRICE")
                        .param("sortDir", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    // ---- soft delete ----

    @Test
    void getAllProducts_shouldNotReturnDeletedProducts() throws Exception {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.update(
                "UPDATE products SET deleted_at = NOW() WHERE name = 'Samsung QLED 55'");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[*].name",
                        not(hasItem("Samsung QLED 55"))));
    }

    // ---- helpers ----

    private void saveProduct(String name, BigDecimal price, Currency currency,
                             Producer producer, ProductCategory category,
                             Map<String, Object> attributes) {
        Product product = Product.builder()
                .name(name)
                .price(price)
                .currency(currency)
                .producer(producer)
                .category(category)
                .build();

        ProductAttributes productAttributes = ProductAttributes.builder()
                .attributes(attributes)
                .product(product)
                .build();

        product.setProductAttributes(productAttributes);
        productRepository.save(product);
    }

    private void cleanDatabase() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("TRUNCATE TABLE product_attributes");
        jdbcTemplate.execute("TRUNCATE TABLE products");
        jdbcTemplate.execute("TRUNCATE TABLE producers");
        jdbcTemplate.execute("TRUNCATE TABLE product_categories");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}