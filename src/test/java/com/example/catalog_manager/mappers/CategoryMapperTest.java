package com.example.catalog_manager.mappers;

import com.example.catalog_manager.dto.category.CategoryResponse;
import com.example.catalog_manager.dto.category.CreateCategoryRequest;
import com.example.catalog_manager.dto.category.UpdateCategoryRequest;
import com.example.catalog_manager.entity.ProductCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {CategoryMapperImpl.class})
class CategoryMapperTest {

    @Autowired
    private CategoryMapper mapper;

    // ---- toResponse ----

    @Test
    void toResponse_shouldMapAllFields() {
        ProductCategory category = ProductCategory.builder()
                .id(UUID.randomUUID())
                .name("TV")
                .requiredAttributes(Set.of("screen_size", "resolution"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        CategoryResponse response = mapper.toResponse(category);

        assertThat(response.getId()).isEqualTo(category.getId());
        assertThat(response.getName()).isEqualTo(category.getName());
        assertThat(response.getRequiredAttributes())
                .containsExactlyInAnyOrderElementsOf(category.getRequiredAttributes());
    }

    @Test
    void toResponse_whenNull_shouldReturnNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    // ---- toEntity ----

    @Test
    void toEntity_shouldMapAllFields() {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("TV")
                .requiredAttributes(Set.of("screen_size", "resolution"))
                .build();

        ProductCategory category = mapper.toEntity(request);

        assertThat(category.getName()).isEqualTo(request.getName());
        assertThat(category.getRequiredAttributes())
                .containsExactlyInAnyOrderElementsOf(request.getRequiredAttributes());
    }

    @Test
    void toEntity_whenNoRequiredAttributes_shouldHaveEmptySet() {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("TV")
                .build();

        ProductCategory category = mapper.toEntity(request);

        assertThat(category.getRequiredAttributes()).isNotNull().isEmpty();
    }

    // ---- updateEntity ----

    @Test
    void updateEntity_shouldUpdateOnlyProvidedFields() {
        ProductCategory existing = ProductCategory.builder()
                .id(UUID.randomUUID())
                .name("TV")
                .requiredAttributes(new HashSet<>(Set.of("screen_size")))
                .build();

        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .requiredAttributes(Set.of("screen_size", "resolution", "weight"))
                .build();

        mapper.updateEntity(request, existing);

        assertThat(existing.getName()).isEqualTo("TV");
        assertThat(existing.getRequiredAttributes())
                .containsExactlyInAnyOrder("screen_size", "resolution", "weight");
    }

    @Test
    void updateEntity_whenAllFieldsNull_shouldChangeNothing() {
        ProductCategory existing = ProductCategory.builder()
                .id(UUID.randomUUID())
                .name("TV")
                .requiredAttributes(new HashSet<>(Set.of("screen_size")))
                .build();

        mapper.updateEntity(new UpdateCategoryRequest(), existing);

        assertThat(existing.getName()).isEqualTo("TV");
        assertThat(existing.getRequiredAttributes()).containsExactly("screen_size");
    }
}