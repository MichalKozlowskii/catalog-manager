package com.example.catalog_manager.controller;

import com.example.catalog_manager.controller.exceptions.BusinessException;
import com.example.catalog_manager.controller.exceptions.ResourceNotFoundException;
import com.example.catalog_manager.dto.PageResponse;
import com.example.catalog_manager.dto.producer.CreateProducerRequest;
import com.example.catalog_manager.dto.producer.ProducerResponse;
import com.example.catalog_manager.dto.producer.UpdateProducerRequest;
import com.example.catalog_manager.service.ProducerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProducerController.class)
class ProducerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProducerService producerService;

    private UUID producerId;
    private ProducerResponse producerResponse;

    @BeforeEach
    void setUp() {
        producerId = UUID.randomUUID();
        producerResponse = ProducerResponse.builder()
                .id(producerId)
                .name("Samsung")
                .country("South Korea")
                .email("contact@samsung.com")
                .build();
    }

    // ---- POST /api/producers ----

    @Test
    void createProducer_shouldReturn201WithLocation() throws Exception {
        CreateProducerRequest request = CreateProducerRequest.builder()
                .name("Samsung")
                .country("South Korea")
                .email("contact@samsung.com")
                .build();

        when(producerService.createProducer(any())).thenReturn(producerId);

        mockMvc.perform(post("/api/producers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("location", "/api/producers/" + producerId));
    }

    @Test
    void createProducer_whenNameBlank_shouldReturn400() throws Exception {
        CreateProducerRequest request = CreateProducerRequest.builder()
                .name("")
                .country("South Korea")
                .email("contact@samsung.com")
                .build();

        mockMvc.perform(post("/api/producers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(producerService, never()).createProducer(any());
    }

    @Test
    void createProducer_whenEmailInvalid_shouldReturn400() throws Exception {
        CreateProducerRequest request = CreateProducerRequest.builder()
                .name("Samsung")
                .country("South Korea")
                .email("not-an-email")
                .build();

        mockMvc.perform(post("/api/producers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(producerService, never()).createProducer(any());
    }

    @Test
    void createProducer_whenBodyMissing_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/producers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ---- PATCH /api/producers/{id} ----

    @Test
    void updateProducer_shouldReturn200() throws Exception {
        UpdateProducerRequest request = UpdateProducerRequest.builder()
                .name("Samsung Electronics")
                .build();

        mockMvc.perform(patch("/api/producers/{id}", producerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(producerService).updateProducerById(eq(producerId), any());
    }

    @Test
    void updateProducer_whenNotFound_shouldReturn404() throws Exception {
        UpdateProducerRequest request = UpdateProducerRequest.builder()
                .name("Samsung Electronics")
                .build();

        doThrow(new ResourceNotFoundException("Producer not found with id: " + producerId))
                .when(producerService).updateProducerById(eq(producerId), any());

        mockMvc.perform(patch("/api/producers/{id}", producerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ---- DELETE /api/producers/{id} ----

    @Test
    void deleteProducer_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/producers/{id}", producerId))
                .andExpect(status().isOk());

        verify(producerService).deleteProducerById(producerId, false); // ← domyślnie false
    }

    @Test
    void deleteProducer_withForceTrue_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/producers/{id}", producerId)
                        .param("force", "true"))
                .andExpect(status().isOk());

        verify(producerService).deleteProducerById(producerId, true);
    }

    @Test
    void deleteProducer_whenNotFound_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Producer not found with id: " + producerId))
                .when(producerService).deleteProducerById(producerId, false);

        mockMvc.perform(delete("/api/producers/{id}", producerId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProducer_whenHasActiveProducts_shouldReturn409() throws Exception {
        doThrow(new BusinessException("Cannot delete producer with 5 active products"))
                .when(producerService).deleteProducerById(producerId, false);

        mockMvc.perform(delete("/api/producers/{id}", producerId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Cannot delete producer with 5 active products"));
    }

    @Test
    void deleteProducer_withForceTrueWhenHasProducts_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/producers/{id}", producerId)
                        .param("force", "true"))
                .andExpect(status().isOk());

        verify(producerService).deleteProducerById(producerId, true);
    }
    // ---- GET /api/producers/{id} ----

    @Test
    void fetchProducer_shouldReturn200WithBody() throws Exception {
        when(producerService.getProducerById(producerId)).thenReturn(producerResponse);

        mockMvc.perform(get("/api/producers/{id}", producerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(producerId.toString()))
                .andExpect(jsonPath("$.name").value("Samsung"))
                .andExpect(jsonPath("$.country").value("South Korea"))
                .andExpect(jsonPath("$.email").value("contact@samsung.com"));
    }

    @Test
    void fetchProducer_whenNotFound_shouldReturn404() throws Exception {
        when(producerService.getProducerById(producerId))
                .thenThrow(new ResourceNotFoundException("Producer not found with id: " + producerId));

        mockMvc.perform(get("/api/producers/{id}", producerId))
                .andExpect(status().isNotFound());
    }

    @Test
    void fetchProducer_whenInvalidUUID_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/producers/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    // ---- GET /api/producers ----

    @Test
    void fetchAll_shouldReturn200WithPageResponse() throws Exception {
        PageResponse<ProducerResponse> pageResponse = PageResponse.<ProducerResponse>builder()
                .content(List.of(producerResponse))
                .page(1)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(producerService.getAllProducers(any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/producers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("Samsung"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void fetchAll_withCountryFilter_shouldPassCountryToService() throws Exception {
        when(producerService.getAllProducers(eq("South Korea"), anyInt(), anyInt(), any(), any()))
                .thenReturn(PageResponse.<ProducerResponse>builder()
                        .content(List.of(producerResponse))
                        .page(1).size(20).totalElements(1).totalPages(1).last(true)
                        .build());

        mockMvc.perform(get("/api/producers").param("country", "South Korea"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].country").value("South Korea"));

        verify(producerService).getAllProducers(
                eq("South Korea"), anyInt(), anyInt(), any(), any());
    }

    @Test
    void fetchAll_whenSizeExceedsMax_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/producers")
                        .param("size", "999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fetchAll_whenPageNegative_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/producers")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }
}