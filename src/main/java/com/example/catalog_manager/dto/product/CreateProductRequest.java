package com.example.catalog_manager.dto.product;

import com.example.catalog_manager.enums.Currency;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name cannot exceed 255 characters")
    private String name;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 15, fraction = 2, message = "Invalid price format")
    private BigDecimal price;

    @NotNull(message = "Currency is required")
    private Currency currency;

    @NotNull(message = "Producer ID is required")
    private UUID producerId;

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();
}