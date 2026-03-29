package com.example.catalog_manager.controller;

import com.example.catalog_manager.Repository.ProducerRepository;
import com.example.catalog_manager.Repository.ProductCategoryRepository;
import com.example.catalog_manager.Repository.ProductRepository;
import com.example.catalog_manager.entity.Producer;
import com.example.catalog_manager.entity.Product;
import com.example.catalog_manager.entity.ProductCategory;
import com.example.catalog_manager.enums.Currency;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProducerDeleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProducerRepository producerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    private UUID producerId;
    private Producer producer;
    private ProductCategory category;

    private void cleanDatabase() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("TRUNCATE TABLE product_attributes");
        jdbcTemplate.execute("TRUNCATE TABLE products");
        jdbcTemplate.execute("TRUNCATE TABLE producers");
        jdbcTemplate.execute("TRUNCATE TABLE product_categories");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }


    @BeforeEach
    void setUp() {
        cleanDatabase();

        producer = producerRepository.save(Producer.builder()
                .name("Samsung")
                .country("South Korea")
                .email("contact@samsung.com")
                .build());
        producerId = producer.getId();

        category = categoryRepository.save(ProductCategory.builder()
                .name("TV")
                .requiredAttributes(Set.of("screen_size"))
                .build());

        productRepository.save(Product.builder()
                .name("Samsung QLED 55")
                .price(new BigDecimal("2999.99"))
                .currency(Currency.PLN)
                .producer(producer)
                .category(category)
                .build());
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    // ---- force=false ----

    @Test
    void deleteProducer_whenHasActiveProducts_shouldReturn409AndNotDelete() throws Exception {
        mockMvc.perform(delete("/api/producers/{id}", producerId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("1")));

        assertThat(producerRepository.findById(producerId))
                .isPresent()
                .get()
                .extracting(Producer::getDeletedAt)
                .isNull();
    }

    @Test
    void deleteProducer_whenNoActiveProducts_shouldSoftDelete() throws Exception {
        productRepository.softDeleteByProducerId(producerId);

        mockMvc.perform(delete("/api/producers/{id}", producerId))
                .andExpect(status().isOk());

        // @SQLRestriction makes Hibernate overlook it
        assertThat(producerRepository.findById(producerId))
                .isNotPresent();
    }

    // ---- force=true ----

    @Test
    void deleteProducer_withForce_shouldSoftDeleteProducerAndProducts() throws Exception {
        mockMvc.perform(delete("/api/producers/{id}", producerId)
                        .param("force", "true"))
                .andExpect(status().isOk());

        assertThat(producerRepository.findById(producerId))
                .isNotPresent();

        long activeProducts = productRepository.countByProducer(producer);
        assertThat(activeProducts).isZero();
    }

    @Test
    void deleteProducer_withForce_productsShouldNotAppearInApi() throws Exception {
        mockMvc.perform(delete("/api/producers/{id}", producerId)
                        .param("force", "true"))
                .andExpect(status().isOk());


        mockMvc.perform(get("/api/products")
                        .param("producerId", producerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ---- not found ----

    @Test
    void deleteProducer_whenNotFound_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/producers/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ---- soft deleted producer not visible ----

    @Test
    void deletedProducer_shouldNotAppearInGetAll() throws Exception {
        productRepository.softDeleteByProducerId(producerId);

        mockMvc.perform(delete("/api/producers/{id}", producerId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/producers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void deletedProducer_shouldReturn404OnGetById() throws Exception {
        productRepository.softDeleteByProducerId(producerId);

        mockMvc.perform(delete("/api/producers/{id}", producerId))
                .andExpect(status().isOk());

        // @SQLRestriction filtruje usuniętego producenta
        mockMvc.perform(get("/api/producers/{id}", producerId))
                .andExpect(status().isNotFound());
    }
}