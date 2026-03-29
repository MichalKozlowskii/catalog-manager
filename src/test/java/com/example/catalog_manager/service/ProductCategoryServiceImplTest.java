package com.example.catalog_manager.service;

import com.example.catalog_manager.Repository.ProductCategoryRepository;
import com.example.catalog_manager.Repository.ProductRepository;
import com.example.catalog_manager.controller.exceptions.BusinessException;
import com.example.catalog_manager.controller.exceptions.ResourceNotFoundException;
import com.example.catalog_manager.dto.PageResponse;
import com.example.catalog_manager.dto.category.CategoryResponse;
import com.example.catalog_manager.dto.category.CreateCategoryRequest;
import com.example.catalog_manager.dto.category.UpdateCategoryRequest;
import com.example.catalog_manager.entity.ProductCategory;
import com.example.catalog_manager.enums.ProductCategorySortField;
import com.example.catalog_manager.mappers.CategoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductCategoryServiceImplTest {

    @Mock
    private ProductCategoryRepository repository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryMapper mapper;

    @InjectMocks
    private ProductCategoryServiceImpl categoryService;

    private UUID categoryId;
    private ProductCategory category;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        category = ProductCategory.builder()
                .id(categoryId)
                .name("TV")
                .requiredAttributes(Set.of("screen_size", "resolution"))
                .build();
    }

    // ---- createCategory ----

    @Test
    void createCategory_shouldSaveAndReturnId() {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("TV")
                .requiredAttributes(Set.of("screen_size"))
                .build();

        when(mapper.toEntity(request)).thenReturn(category);
        when(repository.save(category)).thenReturn(category);

        UUID result = categoryService.createCategory(request);

        assertThat(result).isEqualTo(categoryId);
        verify(mapper).toEntity(request);
        verify(repository).save(category);
    }

    // ---- updateCategoryById ----

    @Test
    void updateCategoryById_shouldUpdateExistingCategory() {
        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("Smart TV")
                .build();

        when(repository.findById(categoryId)).thenReturn(Optional.of(category));

        categoryService.updateCategoryById(categoryId, request);

        verify(repository).findById(categoryId);
        verify(mapper).updateEntity(request, category);
    }

    @Test
    void updateCategoryById_whenNotFound_shouldThrowResourceNotFoundException() {
        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("Smart TV")
                .build();

        when(repository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategoryById(categoryId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(categoryId.toString());

        verify(mapper, never()).updateEntity(any(), any());
    }

    // ---- deleteCategoryById force=false ----

    @Test
    void deleteCategory_whenNoActiveProducts_shouldSoftDelete() {
        when(repository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.countByCategory(category)).thenReturn(0L);

        categoryService.deleteCategoryById(categoryId, false);

        assertThat(category.getDeletedAt()).isNotNull();
        verify(productRepository, never()).softDeleteByCategoryId(any());
    }

    @Test
    void deleteCategory_whenHasActiveProducts_shouldThrowBusinessException() {
        when(repository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.countByCategory(category)).thenReturn(3L);

        assertThatThrownBy(() -> categoryService.deleteCategoryById(categoryId, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("3");

        assertThat(category.getDeletedAt()).isNull();
        verify(productRepository, never()).softDeleteByCategoryId(any());
    }

    @Test
    void deleteCategory_whenNotFound_shouldThrowResourceNotFoundException() {
        when(repository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategoryById(categoryId, false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(categoryId.toString());

        verify(productRepository, never()).countByCategory(any());
        verify(productRepository, never()).softDeleteByCategoryId(any());
    }

    @Test
    void deleteCategory_shouldSetDeletedAtToNow() {
        when(repository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.countByCategory(category)).thenReturn(0L);

        LocalDateTime before = LocalDateTime.now();
        categoryService.deleteCategoryById(categoryId, false);
        LocalDateTime after = LocalDateTime.now();

        assertThat(category.getDeletedAt())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }

    // ---- deleteCategoryById force=true ----

    @Test
    void deleteCategory_withForce_shouldSoftDeleteProductsAndCategory() {
        when(repository.findById(categoryId)).thenReturn(Optional.of(category));

        categoryService.deleteCategoryById(categoryId, true);

        verify(productRepository).softDeleteByCategoryId(categoryId);
        assertThat(category.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteCategory_withForce_shouldNotCheckActiveProducts() {
        when(repository.findById(categoryId)).thenReturn(Optional.of(category));

        categoryService.deleteCategoryById(categoryId, true);

        verify(productRepository, never()).countByCategory(any());
    }

    @Test
    void deleteCategory_withForce_whenNotFound_shouldThrowResourceNotFoundException() {
        when(repository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategoryById(categoryId, true))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(categoryId.toString());

        verify(productRepository, never()).softDeleteByCategoryId(any());
    }

    // ---- getCategoryById ----

    @Test
    void getCategoryById_shouldReturnMappedResponse() {
        CategoryResponse response = CategoryResponse.builder()
                .id(categoryId)
                .name("TV")
                .requiredAttributes(Set.of("screen_size", "resolution"))
                .build();

        when(repository.findById(categoryId)).thenReturn(Optional.of(category));
        when(mapper.toResponse(category)).thenReturn(response);

        CategoryResponse result = categoryService.getCategoryById(categoryId);

        assertThat(result.getId()).isEqualTo(categoryId);
        assertThat(result.getName()).isEqualTo("TV");
        assertThat(result.getRequiredAttributes())
                .containsExactlyInAnyOrder("screen_size", "resolution");
    }

    @Test
    void getCategoryById_whenNotFound_shouldThrowResourceNotFoundException() {
        when(repository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(categoryId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(categoryId.toString());

        verify(mapper, never()).toResponse(any());
    }

    // ---- getAllCategories ----

    @Test
    void getAllCategories_shouldReturnPageResponse() {
        ProductCategory category2 = ProductCategory.builder()
                .id(UUID.randomUUID())
                .name("Smartphones")
                .build();

        CategoryResponse response1 = CategoryResponse.builder().id(categoryId).name("TV").build();
        CategoryResponse response2 = CategoryResponse.builder().id(category2.getId()).name("Smartphones").build();

        Page<ProductCategory> page = new PageImpl<>(
                List.of(category, category2),
                PageRequest.of(0, 20),
                2
        );

        when(repository.findAll(any(Pageable.class))).thenReturn(page);
        when(mapper.toResponse(category)).thenReturn(response1);
        when(mapper.toResponse(category2)).thenReturn(response2);

        PageResponse<CategoryResponse> result = categoryService.getAllCategories(
                0, 20, ProductCategorySortField.NAME, Sort.Direction.ASC);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.isLast()).isTrue();
    }

    @Test
    void getAllCategories_shouldPassCorrectSortToRepository() {
        Page<ProductCategory> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(repository.findAll(any(Pageable.class))).thenReturn(page);

        categoryService.getAllCategories(0, 20, ProductCategorySortField.CREATED_AT, Sort.Direction.DESC);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(pageableCaptor.capture());

        Pageable captured = pageableCaptor.getValue();
        assertThat(captured.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(captured.getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getAllCategories_shouldReturnCorrectPageMetadata() {
        Page<ProductCategory> page = new PageImpl<>(
                List.of(category),
                PageRequest.of(1, 5),
                25
        );

        when(repository.findAll(any(Pageable.class))).thenReturn(page);
        when(mapper.toResponse(any())).thenReturn(new CategoryResponse());

        PageResponse<CategoryResponse> result = categoryService.getAllCategories(
                1, 5, ProductCategorySortField.NAME, Sort.Direction.ASC);

        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(25);
        assertThat(result.getTotalPages()).isEqualTo(5);
        assertThat(result.isLast()).isFalse();
    }
}