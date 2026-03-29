package com.example.catalog_manager.service;

import com.example.catalog_manager.dto.PageResponse;
import com.example.catalog_manager.dto.category.CategoryResponse;
import com.example.catalog_manager.dto.category.CreateCategoryRequest;
import com.example.catalog_manager.dto.category.UpdateCategoryRequest;
import com.example.catalog_manager.enums.ProductCategorySortField;
import org.springframework.data.domain.Sort;

import java.util.UUID;

public interface ProductCategoryService {
    UUID createCategory(CreateCategoryRequest request);
    void updateCategoryById(UUID categoryId, UpdateCategoryRequest request);
    void deleteCategoryById(UUID categoryId, boolean force);
    CategoryResponse getCategoryById(UUID categoryId);
    PageResponse<CategoryResponse> getAllCategories(int page, int size,
                                                    ProductCategorySortField sortBy, Sort.Direction sortDir);
}
