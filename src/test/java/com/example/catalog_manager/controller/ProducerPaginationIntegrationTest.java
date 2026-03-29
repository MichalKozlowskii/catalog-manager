package com.example.catalog_manager.controller;

import com.example.catalog_manager.Repository.ProducerRepository;
import com.example.catalog_manager.entity.Producer;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Rollback
class ProducerPaginationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProducerRepository producerRepository;

    @BeforeEach
    void setUp() {
        producerRepository.deleteAll();

        producerRepository.saveAll(List.of(
                Producer.builder()
                        .name("Samsung")
                        .country("South Korea")
                        .email("contact@samsung.com")
                        .build(),
                Producer.builder()
                        .name("LG")
                        .country("South Korea")
                        .email("contact@lg.com")
                        .build(),
                Producer.builder()
                        .name("Bosch")
                        .country("Germany")
                        .email("contact@bosch.com")
                        .build(),
                Producer.builder()
                        .name("Siemens")
                        .country("Germany")
                        .email("contact@siemens.com")
                        .build(),
                Producer.builder()
                        .name("Apple")
                        .country("USA")
                        .email("contact@apple.com")
                        .build()
        ));
    }

    // ---- filter ----

    @Test
    void fetchAll_withoutFilter_shouldReturnAllProducers() throws Exception {
        mockMvc.perform(get("/api/producers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(5));
    }

    @Test
    void fetchAll_withCountryFilter_shouldReturnOnlyMatchingProducers() throws Exception {
        mockMvc.perform(get("/api/producers")
                        .param("country", "Germany"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[*].country",
                        everyItem(equalTo("Germany"))));
    }

    @Test
    void fetchAll_withCountryFilterCaseInsensitive_shouldReturnResults() throws Exception {
        mockMvc.perform(get("/api/producers")
                        .param("country", "germany")) // małe litery
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void fetchAll_withNonExistentCountry_shouldReturnEmptyPage() throws Exception {
        mockMvc.perform(get("/api/producers")
                        .param("country", "France"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    // ---- sorting ----

    @Test
    void fetchAll_sortByNameAsc_shouldReturnAlphabeticalOrder() throws Exception {
        mockMvc.perform(get("/api/producers")
                        .param("sortBy", "NAME")
                        .param("sortDir", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Apple"))
                .andExpect(jsonPath("$.content[1].name").value("Bosch"))
                .andExpect(jsonPath("$.content[2].name").value("LG"));
    }

    @Test
    void fetchAll_sortByNameDesc_shouldReturnReverseAlphabeticalOrder() throws Exception {
        mockMvc.perform(get("/api/producers")
                        .param("sortBy", "NAME")
                        .param("sortDir", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Siemens"))
                .andExpect(jsonPath("$.content[1].name").value("Samsung"));
    }

    @Test
    void fetchAll_sortByCountryAsc_shouldGroupByCountry() throws Exception {
        mockMvc.perform(get("/api/producers")
                        .param("sortBy", "COUNTRY")
                        .param("sortDir", "ASC"))
                .andExpect(status().isOk())
                // Germany (2) → South Korea (2) → USA (1) alfabetycznie
                .andExpect(jsonPath("$.content[0].country").value("Germany"))
                .andExpect(jsonPath("$.content[1].country").value("Germany"))
                .andExpect(jsonPath("$.content[4].country").value("USA"));
    }

    // ---- combination ----

    @Test
    void fetchAll_filterAndSorting_shouldWorkTogether() throws Exception {
        mockMvc.perform(get("/api/producers")
                        .param("country", "South Korea")
                        .param("sortBy", "NAME")
                        .param("sortDir", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].name").value("LG"))
                .andExpect(jsonPath("$.content[1].name").value("Samsung"));
    }

    // ---- validation ----

    @Test
    void fetchAll_whenInvalidSortField_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/producers")
                        .param("sortBy", "INVALID_FIELD"))
                .andExpect(status().isBadRequest());
    }
}