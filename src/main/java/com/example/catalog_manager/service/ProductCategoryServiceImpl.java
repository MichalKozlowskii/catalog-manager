package com.example.catalog_manager.service;

import com.example.catalog_manager.Repository.ProductCategoryRepository;
import com.example.catalog_manager.Repository.ProductRepository;
import com.example.catalog_manager.controller.exceptions.BusinessException;
import com.example.catalog_manager.controller.exceptions.ResourceNotFoundException;
import com.example.catalog_manager.dto.PageResponse;
import com.example.catalog_manager.dto.category.CategoryResponse;
import com.example.catalog_manager.dto.category.CreateCategoryRequest;
import com.example.catalog_manager.dto.category.UpdateCategoryRequest;
import com.example.catalog_manager.dto.producer.ProducerResponse;
import com.example.catalog_manager.entity.ProductCategory;
import com.example.catalog_manager.enums.ProducerSortField;
import com.example.catalog_manager.enums.ProductCategorySortField;
import com.example.catalog_manager.mappers.CategoryMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl implements ProductCategoryService {
    private final ProductCategoryRepository repository;
    private final ProductRepository productRepository;
    private final CategoryMapper mapper;

    @Override
    public UUID createCategory(CreateCategoryRequest request) {
        return repository.save(mapper.toEntity(request)).getId();
    }

    @Override
    @Transactional
    public void updateCategoryById(UUID categoryId, UpdateCategoryRequest request) {
        ProductCategory existing = repository.findById(categoryId).orElseThrow(
                () -> new ResourceNotFoundException("Category not found with id: " + categoryId)
        );

        mapper.updateEntity(request, existing);
    }

    @Override
    @Transactional
    public void deleteCategoryById(UUID categoryId, boolean force) {
        ProductCategory category = repository.findById(categoryId).orElseThrow(
                () -> new ResourceNotFoundException("Category not found with id: " + categoryId)
        );

        if (force) {
            productRepository.softDeleteByCategoryId(categoryId);
        } else {
            long activeProducts = productRepository.countByCategory(category);
            if (activeProducts > 0) {
                throw new BusinessException("Cannot delete category with " + activeProducts + " active products. " +
                                "Use force=true to delete all products as well.");
            }
        }

        category.setDeletedAt(LocalDateTime.now());
    }
    @Override
    public CategoryResponse getCategoryById(UUID categoryId) {
        ProductCategory existing = repository.findById(categoryId).orElseThrow(
                () -> new ResourceNotFoundException("Category not found with id: " + categoryId)
        );

        return mapper.toResponse(existing);
    }

    @Override
    public PageResponse<CategoryResponse> getAllCategories(int page, int size, ProductCategorySortField sortBy,
                                                           Sort.Direction sortDir) {
        String sortField = switch (sortBy) {
            case NAME -> "name";
            case CREATED_AT -> "createdAt";
            case UPDATED_AT -> "updatedAt";
        };

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortField));

        Page<ProductCategory> result = repository.findAll(pageable);

        return PageResponse.<CategoryResponse>builder()
                .content(result.getContent().stream()
                        .map(mapper::toResponse)
                        .toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }
}
