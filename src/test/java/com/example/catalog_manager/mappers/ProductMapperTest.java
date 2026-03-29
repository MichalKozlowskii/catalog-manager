package com.example.catalog_manager.mappers;

import com.example.catalog_manager.dto.product.CreateProductRequest;
import com.example.catalog_manager.dto.product.ProductResponse;
import com.example.catalog_manager.dto.product.UpdateProductRequest;
import com.example.catalog_manager.entity.Producer;
import com.example.catalog_manager.entity.Product;
import com.example.catalog_manager.entity.ProductAttributes;
import com.example.catalog_manager.entity.ProductCategory;
import com.example.catalog_manager.enums.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ProductMapperImpl.class, ProducerMapperImpl.class, CategoryMapperImpl.class})
class ProductMapperTest {

    @Autowired
    private ProductMapper mapper;

    private Producer producer;
    private ProductCategory category;
    private ProductAttributes attributes;
    private Product product;

    @BeforeEach
    void setUp() {
        producer = Producer.builder()
                .id(UUID.randomUUID())
                .name("Samsung")
                .country("South Korea")
                .email("contact@samsung.com")
                .build();

        category = ProductCategory.builder()
                .id(UUID.randomUUID())
                .name("TV")
                .requiredAttributes(Set.of("screen_size"))
                .build();

        attributes = ProductAttributes.builder()
                .id(UUID.randomUUID())
                .attributes(Map.of("screen_size", "55", "resolution", "4K"))
                .build();

        product = Product.builder()
                .id(UUID.randomUUID())
                .name("Samsung QLED 55")
                .description("Great TV")
                .price(new BigDecimal("2999.99"))
                .currency(Currency.PLN)
                .producer(producer)
                .category(category)
                .productAttributes(attributes)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ---- toResponse ----

    @Test
    void toResponse_shouldMapAllFields() {
        ProductResponse response = mapper.toResponse(product);

        assertThat(response.getId()).isEqualTo(product.getId());
        assertThat(response.getName()).isEqualTo(product.getName());
        assertThat(response.getDescription()).isEqualTo(product.getDescription());
        assertThat(response.getPrice()).isEqualTo(product.getPrice());
        assertThat(response.getCurrency()).isEqualTo(product.getCurrency());
        assertThat(response.getCreatedAt()).isEqualTo(product.getCreatedAt());
    }

    @Test
    void toResponse_shouldMapProducerSummary() {
        ProductResponse response = mapper.toResponse(product);

        assertThat(response.getProducer().getId()).isEqualTo(producer.getId());
        assertThat(response.getProducer().getName()).isEqualTo(producer.getName());
        assertThat(response.getProducer().getCountry()).isEqualTo(producer.getCountry());
    }

    @Test
    void toResponse_shouldMapCategorySummary() {
        ProductResponse response = mapper.toResponse(product);

        assertThat(response.getCategory().getId()).isEqualTo(category.getId());
        assertThat(response.getCategory().getName()).isEqualTo(category.getName());
    }

    @Test
    void toResponse_shouldMapAttributesFromProductAttributes() {
        ProductResponse response = mapper.toResponse(product);

        assertThat(response.getAttributes())
                .containsEntry("screen_size", "55")
                .containsEntry("resolution", "4K");
    }

    @Test
    void toResponse_whenAttributesNull_shouldReturnNullAttributes() {
        product.setProductAttributes(null);

        ProductResponse response = mapper.toResponse(product);

        assertThat(response.getAttributes()).isNull();
    }

    // ---- toEntity ----

    @Test
    void toEntity_shouldMapBasicFields() {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Samsung QLED 55")
                .description("Great TV")
                .price(new BigDecimal("2999.99"))
                .currency(Currency.PLN)
                .producerId(UUID.randomUUID())
                .categoryId(UUID.randomUUID())
                .attributes(Map.of("screen_size", "55"))
                .build();

        Product entity = mapper.toEntity(request);

        assertThat(entity.getName()).isEqualTo(request.getName());
        assertThat(entity.getDescription()).isEqualTo(request.getDescription());
        assertThat(entity.getPrice()).isEqualTo(request.getPrice());
        assertThat(entity.getCurrency()).isEqualTo(request.getCurrency());
    }

    @Test
    void toEntity_shouldIgnoreProducerCategoryAndAttributes() {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Samsung QLED 55")
                .price(new BigDecimal("2999.99"))
                .currency(Currency.PLN)
                .producerId(UUID.randomUUID())
                .categoryId(UUID.randomUUID())
                .build();

        Product entity = mapper.toEntity(request);

        // te pola ustawia serwis po pobraniu z repo
        assertThat(entity.getProducer()).isNull();
        assertThat(entity.getCategory()).isNull();
        assertThat(entity.getProductAttributes()).isNull();
    }

    // ---- updateEntity ----

    @Test
    void updateEntity_shouldUpdateOnlyProvidedFields() {
        Product existing = Product.builder()
                .id(UUID.randomUUID())
                .name("Stara nazwa")
                .description("Stary opis")
                .price(new BigDecimal("1999.99"))
                .currency(Currency.PLN)
                .build();

        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("Nowa nazwa")
                .build();

        mapper.updateEntity(request, existing);

        assertThat(existing.getName()).isEqualTo("Nowa nazwa");
        assertThat(existing.getDescription()).isEqualTo("Stary opis");
        assertThat(existing.getPrice()).isEqualTo(new BigDecimal("1999.99"));
    }

    @Test
    void updateEntity_whenAllFieldsNull_shouldChangeNothing() {
        Product existing = Product.builder()
                .id(UUID.randomUUID())
                .name("Samsung QLED 55")
                .price(new BigDecimal("2999.99"))
                .currency(Currency.PLN)
                .build();

        mapper.updateEntity(new UpdateProductRequest(), existing);

        assertThat(existing.getName()).isEqualTo("Samsung QLED 55");
        assertThat(existing.getPrice()).isEqualTo(new BigDecimal("2999.99"));
        assertThat(existing.getCurrency()).isEqualTo(Currency.PLN);
    }
}