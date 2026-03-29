package com.example.catalog_manager.service;

import com.example.catalog_manager.Repository.ProducerRepository;
import com.example.catalog_manager.Repository.ProductCategoryRepository;
import com.example.catalog_manager.Repository.ProductRepository;
import com.example.catalog_manager.controller.exceptions.BadRequestException;
import com.example.catalog_manager.controller.exceptions.ResourceNotFoundException;
import com.example.catalog_manager.dto.PageResponse;
import com.example.catalog_manager.dto.product.CreateProductRequest;
import com.example.catalog_manager.dto.product.ProductFilter;
import com.example.catalog_manager.dto.product.ProductResponse;
import com.example.catalog_manager.dto.product.UpdateProductRequest;
import com.example.catalog_manager.entity.Producer;
import com.example.catalog_manager.entity.Product;
import com.example.catalog_manager.entity.ProductAttributes;
import com.example.catalog_manager.entity.ProductCategory;
import com.example.catalog_manager.enums.Currency;
import com.example.catalog_manager.enums.ProductSortField;
import com.example.catalog_manager.mappers.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProducerRepository producerRepository;

    @Mock
    private ProductCategoryRepository categoryRepository;

    @Mock
    private ProductMapper mapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private UUID productId;
    private UUID producerId;
    private UUID categoryId;
    private Product product;
    private Producer producer;
    private ProductCategory category;
    private ProductAttributes productAttributes;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        producerId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        producer = Producer.builder()
                .id(producerId)
                .name("Samsung")
                .country("South Korea")
                .email("contact@samsung.com")
                .build();

        category = ProductCategory.builder()
                .id(categoryId)
                .name("TV")
                .requiredAttributes(new HashSet<>(Set.of("screen_size", "resolution")))
                .build();

        productAttributes = ProductAttributes.builder()
                .id(UUID.randomUUID())
                .attributes(new HashMap<>(Map.of("screen_size", "55", "resolution", "4K")))
                .build();

        product = Product.builder()
                .id(productId)
                .name("Samsung QLED 55")
                .price(new BigDecimal("2999.99"))
                .currency(com.example.catalog_manager.enums.Currency.PLN)
                .producer(producer)
                .category(category)
                .productAttributes(productAttributes)
                .build();

        productAttributes.setProduct(product);
    }

    // ---- createProduct ----

    @Test
    void createProduct_shouldSaveAndReturnId() {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Samsung QLED 55")
                .price(new BigDecimal("2999.99"))
                .currency(Currency.PLN)
                .producerId(producerId)
                .categoryId(categoryId)
                .attributes(Map.of("screen_size", "55", "resolution", "4K"))
                .build();

        when(producerRepository.findById(producerId)).thenReturn(Optional.of(producer));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(mapper.toEntity(request)).thenReturn(product);
        when(productRepository.save(any())).thenReturn(product);

        UUID result = productService.createProduct(request);

        assertThat(result).isEqualTo(productId);
        verify(productRepository).save(any());
    }

    @Test
    void createProduct_whenProducerNotFound_shouldThrowResourceNotFoundException() {
        CreateProductRequest request = CreateProductRequest.builder()
                .producerId(producerId)
                .categoryId(categoryId)
                .build();

        when(producerRepository.findById(producerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(producerId.toString());

        verify(categoryRepository, never()).findById(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_whenCategoryNotFound_shouldThrowResourceNotFoundException() {
        CreateProductRequest request = CreateProductRequest.builder()
                .producerId(producerId)
                .categoryId(categoryId)
                .build();

        when(producerRepository.findById(producerId)).thenReturn(Optional.of(producer));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(categoryId.toString());

        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_whenMissingRequiredAttributes_shouldThrowBadRequestException() {
        CreateProductRequest request = CreateProductRequest.builder()
                .producerId(producerId)
                .categoryId(categoryId)
                .attributes(Map.of("screen_size", "55")) // "resolution" missing
                .build();

        when(producerRepository.findById(producerId)).thenReturn(Optional.of(producer));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("resolution");

        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_whenAttributeValueIsNull_shouldThrowBadRequestException() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("screen_size", "55");
        attributes.put("resolution", null); // null val

        CreateProductRequest request = CreateProductRequest.builder()
                .producerId(producerId)
                .categoryId(categoryId)
                .attributes(attributes)
                .build();

        when(producerRepository.findById(producerId)).thenReturn(Optional.of(producer));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("resolution");

        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_whenCategoryHasNoRequiredAttributes_shouldSaveWithoutValidation() {
        category.setRequiredAttributes(new HashSet<>());

        CreateProductRequest request = CreateProductRequest.builder()
                .producerId(producerId)
                .categoryId(categoryId)
                .attributes(Map.of())
                .build();

        when(producerRepository.findById(producerId)).thenReturn(Optional.of(producer));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(mapper.toEntity(request)).thenReturn(product);
        when(productRepository.save(any())).thenReturn(product);

        assertThatNoException().isThrownBy(() -> productService.createProduct(request));
        verify(productRepository).save(any());
    }

    // ---- updateProductById ----

    @Test
    void updateProduct_shouldUpdateBasicFields() {
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("Samsung QLED 65")
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        productService.updateProductById(productId, request);

        verify(mapper).updateEntity(request, product);
    }

    @Test
    void updateProduct_whenNotFound_shouldThrowResourceNotFoundException() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProductById(productId, new UpdateProductRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(productId.toString());

        verify(mapper, never()).updateEntity(any(), any());
    }

    @Test
    void updateProduct_whenAttributesChanged_shouldValidateAndUpdate() {
        Map<String, Object> newAttributes = Map.of("screen_size", "65", "resolution", "8K");

        UpdateProductRequest request = UpdateProductRequest.builder()
                .attributes(newAttributes)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        productService.updateProductById(productId, request);

        assertThat(product.getProductAttributes().getAttributes()).isEqualTo(newAttributes);
        verify(mapper).updateEntity(request, product);
    }

    @Test
    void updateProduct_whenAttributesMissingRequired_shouldThrowBadRequestException() {
        UpdateProductRequest request = UpdateProductRequest.builder()
                .attributes(Map.of("screen_size", "65")) // brakuje "resolution"
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.updateProductById(productId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("resolution");

        verify(mapper, never()).updateEntity(any(), any());
    }

    @Test
    void updateProduct_whenCategoryChanged_shouldValidateAttributesAgainstNewCategory() {
        UUID newCategoryId = UUID.randomUUID();
        ProductCategory newCategory = ProductCategory.builder()
                .id(newCategoryId)
                .name("SMARTPHONE")
                .requiredAttributes(Set.of("battery_capacity"))
                .build();

        UpdateProductRequest request = UpdateProductRequest.builder()
                .categoryId(newCategoryId)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(newCategoryId)).thenReturn(Optional.of(newCategory));

        // current attributes {"screen_size", "resolution"} dont meet requirements of new category
        assertThatThrownBy(() -> productService.updateProductById(productId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("battery_capacity");
    }

    @Test
    void updateProduct_whenCategoryChangedWithCorrectAttributes_shouldUpdate() {
        UUID newCategoryId = UUID.randomUUID();
        ProductCategory newCategory = ProductCategory.builder()
                .id(newCategoryId)
                .name("SMARTPHONE")
                .requiredAttributes(Set.of("battery_capacity"))
                .build();

        UpdateProductRequest request = UpdateProductRequest.builder()
                .categoryId(newCategoryId)
                .attributes(Map.of("battery_capacity", "5000"))
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(newCategoryId)).thenReturn(Optional.of(newCategory));

        assertThatNoException().isThrownBy(() -> productService.updateProductById(productId, request));
        assertThat(product.getCategory()).isEqualTo(newCategory);
        verify(mapper).updateEntity(request, product);
    }

    @Test
    void updateProduct_whenAttributesSameAsExisting_shouldNotRevalidate() {
        UpdateProductRequest request = UpdateProductRequest.builder()
                .attributes(Map.of("screen_size", "55", "resolution", "4K"))
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThatNoException().isThrownBy(() -> productService.updateProductById(productId, request));
        verify(mapper).updateEntity(request, product);
    }

    // ---- deleteProductById ----

    @Test
    void deleteProduct_shouldSetDeletedAt() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        LocalDateTime before = LocalDateTime.now();
        productService.deleteProductById(productId);
        LocalDateTime after = LocalDateTime.now();

        assertThat(product.getDeletedAt())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }

    @Test
    void deleteProduct_whenNotFound_shouldThrowResourceNotFoundException() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProductById(productId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(productId.toString());
    }

    // ---- getProductById ----

    @Test
    void getProductById_shouldReturnMappedResponse() {
        ProductResponse response = ProductResponse.builder()
                .id(productId)
                .name("Samsung QLED 55")
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(mapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.getProductById(productId);

        assertThat(result.getId()).isEqualTo(productId);
        assertThat(result.getName()).isEqualTo("Samsung QLED 55");
    }

    @Test
    void getProductById_whenNotFound_shouldThrowResourceNotFoundException() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(productId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(productId.toString());

        verify(mapper, never()).toResponse(any());
    }

    // ---- getAllProducts ----

    @Test
    void getAllProducts_shouldReturnPageResponse() {
        ProductResponse response = ProductResponse.builder().id(productId).name("Samsung QLED 55").build();
        Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(mapper.toResponse(product)).thenReturn(response);

        PageResponse<ProductResponse> result = productService.getAllProducts(
                new ProductFilter(), 1, 20, ProductSortField.NAME, Sort.Direction.ASC);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getPage()).isEqualTo(1);
    }

    @Test
    void getAllProducts_shouldConvertPageNumberFromOneBased() {
        Page<Product> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        productService.getAllProducts(new ProductFilter(), 1, 20, ProductSortField.NAME, Sort.Direction.ASC);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(any(Specification.class), pageableCaptor.capture());

        // klient wysyła page=1, do bazy trafia page=0
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
    }

    @Test
    void getAllProducts_shouldPassCorrectSortToRepository() {
        Page<Product> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        productService.getAllProducts(new ProductFilter(), 1, 20, ProductSortField.PRICE, Sort.Direction.DESC);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(any(Specification.class), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getSort().getOrderFor("price")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("price").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }
}