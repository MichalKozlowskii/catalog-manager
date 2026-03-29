package com.example.catalog_manager.controller;

import com.example.catalog_manager.controller.ProductController;
import com.example.catalog_manager.controller.exceptions.BadRequestException;
import com.example.catalog_manager.controller.exceptions.ResourceNotFoundException;
import com.example.catalog_manager.dto.PageResponse;
import com.example.catalog_manager.dto.product.CreateProductRequest;
import com.example.catalog_manager.dto.product.ProductFilter;
import com.example.catalog_manager.dto.product.ProductResponse;
import com.example.catalog_manager.dto.product.UpdateProductRequest;
import com.example.catalog_manager.enums.Currency;
import com.example.catalog_manager.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private UUID productId;
    private UUID producerId;
    private UUID categoryId;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        producerId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        productResponse = ProductResponse.builder()
                .id(productId)
                .name("Samsung QLED 55")
                .price(new BigDecimal("2999.99"))
                .currency(Currency.PLN)
                .producer(ProductResponse.ProducerInfo.builder()
                        .id(producerId)
                        .name("Samsung")
                        .country("South Korea")
                        .build())
                .category(ProductResponse.CategoryInfo.builder()
                        .id(categoryId)
                        .name("TV")
                        .build())
                .attributes(Map.of("screen_size", "55", "resolution", "4K"))
                .build();
    }

    // ---- POST /api/products ----

    @Test
    void createProduct_shouldReturn201WithLocation() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Samsung QLED 55")
                .price(new BigDecimal("2999.99"))
                .currency(Currency.PLN)
                .producerId(producerId)
                .categoryId(categoryId)
                .attributes(Map.of("screen_size", "55", "resolution", "4K"))
                .build();

        when(productService.createProduct(any())).thenReturn(productId);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("location", "/api/products/" + productId));
    }

    @Test
    void createProduct_whenNameBlank_shouldReturn400() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("")
                .price(new BigDecimal("2999.99"))
                .currency(Currency.PLN)
                .producerId(producerId)
                .categoryId(categoryId)
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(productService, never()).createProduct(any());
    }

    @Test
    void createProduct_whenPriceNull_shouldReturn400() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Samsung QLED 55")
                .price(null)
                .currency(Currency.PLN)
                .producerId(producerId)
                .categoryId(categoryId)
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(productService, never()).createProduct(any());
    }

    @Test
    void createProduct_whenPriceNegative_shouldReturn400() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Samsung QLED 55")
                .price(new BigDecimal("-1.00"))
                .currency(Currency.PLN)
                .producerId(producerId)
                .categoryId(categoryId)
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(productService, never()).createProduct(any());
    }

    @Test
    void createProduct_whenProducerNotFound_shouldReturn404() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Samsung QLED 55")
                .price(new BigDecimal("2999.99"))
                .currency(Currency.PLN)
                .producerId(producerId)
                .categoryId(categoryId)
                .build();

        when(productService.createProduct(any()))
                .thenThrow(new ResourceNotFoundException("Producer not found with id: " + producerId));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString(producerId.toString())));
    }

    @Test
    void createProduct_whenMissingRequiredAttributes_shouldReturn400() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Samsung QLED 55")
                .price(new BigDecimal("2999.99"))
                .currency(Currency.PLN)
                .producerId(producerId)
                .categoryId(categoryId)
                .attributes(Map.of("screen_size", "55"))
                .build();

        when(productService.createProduct(any()))
                .thenThrow(new BadRequestException("Missing required attributes: [resolution]"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("resolution")));
    }

    // ---- PATCH /api/products/{id} ----

    @Test
    void updateProduct_shouldReturn200() throws Exception {
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("Samsung QLED 65")
                .build();

        mockMvc.perform(patch("/api/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(productService).updateProductById(eq(productId), any());
    }

    @Test
    void updateProduct_whenNotFound_shouldReturn404() throws Exception {
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("Samsung QLED 65")
                .build();

        doThrow(new ResourceNotFoundException("Product not found with id: " + productId))
                .when(productService).updateProductById(eq(productId), any());

        mockMvc.perform(patch("/api/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString(productId.toString())));
    }

    @Test
    void updateProduct_whenInvalidAttributes_shouldReturn400() throws Exception {
        UpdateProductRequest request = UpdateProductRequest.builder()
                .attributes(Map.of("screen_size", "55"))
                .build();

        doThrow(new BadRequestException("Missing required attributes: [resolution]"))
                .when(productService).updateProductById(eq(productId), any());

        mockMvc.perform(patch("/api/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("resolution")));
    }

    // ---- DELETE /api/products/{id} ----

    @Test
    void deleteProduct_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/products/{id}", productId))
                .andExpect(status().isOk());

        verify(productService).deleteProductById(productId);
    }

    @Test
    void deleteProduct_whenNotFound_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Product not found with id: " + productId))
                .when(productService).deleteProductById(productId);

        mockMvc.perform(delete("/api/products/{id}", productId))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/products/{id} ----

    @Test
    void fetchProduct_shouldReturn200WithBody() throws Exception {
        when(productService.getProductById(productId)).thenReturn(productResponse);

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId.toString()))
                .andExpect(jsonPath("$.name").value("Samsung QLED 55"))
                .andExpect(jsonPath("$.price").value(2999.99))
                .andExpect(jsonPath("$.currency").value("PLN"))
                .andExpect(jsonPath("$.producer.name").value("Samsung"))
                .andExpect(jsonPath("$.category.name").value("TV"))
                .andExpect(jsonPath("$.attributes.screen_size").value("55"))
                .andExpect(jsonPath("$.attributes.resolution").value("4K"));
    }

    @Test
    void fetchProduct_whenNotFound_shouldReturn404() throws Exception {
        when(productService.getProductById(productId))
                .thenThrow(new ResourceNotFoundException("Product not found with id: " + productId));

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString(productId.toString())));
    }

    @Test
    void fetchProduct_whenInvalidUUID_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/products/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    // ---- GET /api/products ----

    @Test
    void getAllProducts_shouldReturn200WithPageResponse() throws Exception {
        PageResponse<ProductResponse> pageResponse = PageResponse.<ProductResponse>builder()
                .content(List.of(productResponse))
                .page(1)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(productService.getAllProducts(any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("Samsung QLED 55"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void getAllProducts_withFilters_shouldPassFiltersToService() throws Exception {
        when(productService.getAllProducts(any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(PageResponse.<ProductResponse>builder()
                        .content(List.of())
                        .page(1).size(20).totalElements(0).totalPages(0).last(true)
                        .build());

        mockMvc.perform(get("/api/products")
                        .param("name", "samsung")
                        .param("producerId", producerId.toString())
                        .param("categoryId", categoryId.toString())
                        .param("minPrice", "100")
                        .param("maxPrice", "5000")
                        .param("currency", "PLN"))
                .andExpect(status().isOk());

        ArgumentCaptor<ProductFilter> filterCaptor = ArgumentCaptor.forClass(ProductFilter.class);
        verify(productService).getAllProducts(filterCaptor.capture(), anyInt(), anyInt(), any(), any());

        ProductFilter captured = filterCaptor.getValue();
        assertThat(captured.getName()).isEqualTo("samsung");
        assertThat(captured.getProducerId()).isEqualTo(producerId);
        assertThat(captured.getCategoryId()).isEqualTo(categoryId);
        assertThat(captured.getMinPrice()).isEqualTo(new BigDecimal("100"));
        assertThat(captured.getMaxPrice()).isEqualTo(new BigDecimal("5000"));
        assertThat(captured.getCurrency()).isEqualTo(Currency.PLN);
    }

    @Test
    void getAllProducts_withValidAttributesJson_shouldParseAndPassToService() throws Exception {
        when(productService.getAllProducts(any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(PageResponse.<ProductResponse>builder()
                        .content(List.of()).page(1).size(20).totalElements(0).totalPages(0).last(true)
                        .build());

        mockMvc.perform(get("/api/products")
                        .param("attributes", "{\"color\":\"red\",\"screen_size\":\"55\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<ProductFilter> filterCaptor = ArgumentCaptor.forClass(ProductFilter.class);
        verify(productService).getAllProducts(filterCaptor.capture(), anyInt(), anyInt(), any(), any());

        assertThat(filterCaptor.getValue().getAttributes())
                .containsEntry("color", "red")
                .containsEntry("screen_size", "55");
    }

    @Test
    void getAllProducts_withInvalidAttributesJson_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("attributes", "not-valid-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllProducts_whenSizeExceedsMax_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("size", "999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllProducts_whenPageZero_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("page", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllProducts_whenInvalidSortField_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("sortBy", "INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllProducts_whenInvalidCurrency_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("currency", "INVALID"))
                .andExpect(status().isBadRequest());
    }
}