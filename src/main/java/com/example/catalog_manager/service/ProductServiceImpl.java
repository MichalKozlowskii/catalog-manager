package com.example.catalog_manager.service;

import com.example.catalog_manager.Repository.ProducerRepository;
import com.example.catalog_manager.Repository.ProductCategoryRepository;
import com.example.catalog_manager.Repository.ProductRepository;
import com.example.catalog_manager.Repository.specifications.ProductSpecification;
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
import com.example.catalog_manager.enums.ProductSortField;
import com.example.catalog_manager.mappers.ProductMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ProducerRepository producerRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductMapper mapper;

    private void validateAttributes(ProductCategory category, Map<String, Object> attributes) {
        Set<String> required = category.getRequiredAttributes();

        if (required == null || required.isEmpty()) return;

        Set<String> provided = attributes != null ? attributes.keySet() : Set.of();

        Set<String> missing = required.stream()
                .filter(attr -> !provided.contains(attr))
                .collect(Collectors.toSet());

        Set<String> blank = required.stream()
                .filter(provided::contains)
                .filter(attr -> {
                    Object value = attributes.get(attr);
                    return value == null || value.toString().isBlank();
                })
                .collect(Collectors.toSet());

        if (!missing.isEmpty() || !blank.isEmpty()) {
            throw new BadRequestException(
                    "Invalid attributes for category '" + category.getName() + "'" +
                            (missing.isEmpty() ? "" : ", missing: " + missing) +
                            (blank.isEmpty() ? "" : ", blank: " + blank));
        }
    }

    @Override
    public UUID createProduct(CreateProductRequest request) {
        Producer producer = producerRepository.findById(request.getProducerId()).orElseThrow(
                () -> new ResourceNotFoundException("Producer not found with id: " + request.getProducerId())
        );
        ProductCategory category = categoryRepository.findById(request.getCategoryId()).orElseThrow(
                () -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId())
        );

        validateAttributes(category, request.getAttributes());

        Product product = mapper.toEntity(request);
        product.setProducer(producer);
        product.setCategory(category);

        ProductAttributes productAttributes = ProductAttributes.builder()
                .attributes(request.getAttributes())
                .product(product)
                .build();

        product.setProductAttributes(productAttributes);

        return productRepository.save(product).getId();
    }

    @Override
    @Transactional
    public void updateProductById(UUID productId, UpdateProductRequest request) {
        Product existing = productRepository.findById(productId).orElseThrow(
                () -> new ResourceNotFoundException("Product not found with id: " + productId)
        );

        Map<String, Object> attributes = Optional.ofNullable(existing.getProductAttributes())
                .map(ProductAttributes::getAttributes)
                .orElse(new HashMap<>());

        boolean shouldValidate = false;

        if (request.getAttributes() != null && !attributes.equals(request.getAttributes())) {
            attributes = request.getAttributes();
            shouldValidate = true;
        }

        ProductCategory category = existing.getCategory();

        if (request.getCategoryId() != null && !existing.getCategory().getId().equals(request.getCategoryId())) {
            category = categoryRepository.findById(request.getCategoryId()).orElseThrow(
                    () -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId())
            );

            shouldValidate = true;
        }

        if (shouldValidate) {
            validateAttributes(category, attributes);
        }

        existing.getProductAttributes().setAttributes(attributes);
        existing.setCategory(category);
        mapper.updateEntity(request, existing);
    }

    @Override
    @Transactional
    public void deleteProductById(UUID productId) {
        Product existing = productRepository.findById(productId).orElseThrow(
                () -> new ResourceNotFoundException("Product not found with id: " + productId)
        );

        existing.setDeletedAt(LocalDateTime.now());
    }

    @Override
    public ProductResponse getProductById(UUID productId) {
        Product existing = productRepository.findById(productId).orElseThrow(
                () -> new ResourceNotFoundException("Product not found with id: " + productId)
        );

        return mapper.toResponse(existing);
    }

    @Override
    public PageResponse<ProductResponse> getAllProducts(ProductFilter filter, int page, int size,
                                                        ProductSortField sortBy, Sort.Direction sortDir) {

        String sortField = switch (sortBy) {
            case NAME -> "name";
            case PRICE -> "price";
            case CREATED_AT -> "createdAt";
            case UPDATED_AT -> "updatedAt";
        };

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(sortDir, sortField));

        Page<Product> result = productRepository.findAll(
                ProductSpecification.withFilter(filter), pageable);

        return PageResponse.<ProductResponse>builder()
                .content(result.getContent().stream()
                        .map(mapper::toResponse)
                        .toList())
                .page(result.getNumber() + 1)
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }
}
