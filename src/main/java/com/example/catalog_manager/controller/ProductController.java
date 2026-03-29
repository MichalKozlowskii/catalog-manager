package com.example.catalog_manager.controller;

import com.example.catalog_manager.controller.exceptions.BadRequestException;
import com.example.catalog_manager.dto.PageResponse;
import com.example.catalog_manager.dto.ResponseMessage;
import com.example.catalog_manager.dto.product.CreateProductRequest;
import com.example.catalog_manager.dto.product.ProductFilter;
import com.example.catalog_manager.dto.product.ProductResponse;
import com.example.catalog_manager.dto.product.UpdateProductRequest;
import com.example.catalog_manager.enums.Currency;
import com.example.catalog_manager.enums.ProductSortField;
import com.example.catalog_manager.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ResponseMessage> createProduct(@Valid @RequestBody CreateProductRequest request) {
        UUID newProductId = productService.createProduct(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("location", "/api/products/" + newProductId)
                .body(new ResponseMessage("Product created successfully."));
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<ResponseMessage> updateProduct(@PathVariable("productId") UUID productId,
                                                         @Valid @RequestBody UpdateProductRequest request) {
        productService.updateProductById(productId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseMessage("Product with id: " + productId + " was updated successfully."));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ResponseMessage> deleteProduct(@PathVariable("productId") UUID productId) {
        productService.deleteProductById(productId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseMessage("Product with id: " + productId + " was deleted successfully."));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> fetchProduct(@PathVariable("productId") UUID productId) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(productService.getProductById(productId));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> getAllProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) UUID producerId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Currency currency,
            @RequestParam(required = false) String attributes,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "NAME") ProductSortField sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction sortDir) {

        Map<String, Object> attributesMap = parseAttributes(attributes);

        ProductFilter filter = ProductFilter.builder()
                .name(name)
                .producerId(producerId)
                .categoryId(categoryId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .currency(currency)
                .attributes(attributesMap)
                .build();

        return ResponseEntity.ok(productService.getAllProducts(filter, page, size, sortBy, sortDir));
    }

    private Map<String, Object> parseAttributes(String attributes) {
        if (attributes == null || attributes.isBlank()) return null;
        try {
            return new ObjectMapper().readValue(attributes,
                    new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid attributes JSON: " + attributes);
        }
    }
}
