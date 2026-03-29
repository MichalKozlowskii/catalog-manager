package com.example.catalog_manager.controller;

import com.example.catalog_manager.dto.PageResponse;
import com.example.catalog_manager.dto.ResponseMessage;
import com.example.catalog_manager.dto.category.CategoryResponse;
import com.example.catalog_manager.dto.category.CreateCategoryRequest;
import com.example.catalog_manager.dto.category.UpdateCategoryRequest;
import com.example.catalog_manager.enums.ProductCategorySortField;
import com.example.catalog_manager.service.ProductCategoryServiceImpl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class ProductCategoryController {
    private final ProductCategoryServiceImpl categoryService;

    @PostMapping
    public ResponseEntity<ResponseMessage> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        UUID newCategoryId = categoryService.createCategory(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("location", "/api/categories/" + newCategoryId)
                .body(new ResponseMessage("Category created successfully."));
    }

    @PatchMapping("/{categoryId}")
    public ResponseEntity<ResponseMessage> updateCategory(@PathVariable("categoryId") UUID categoryId,
                                                 @Valid @RequestBody UpdateCategoryRequest request) {
        categoryService.updateCategoryById(categoryId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseMessage("Category with id: " + categoryId + " was updated successfully."));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ResponseMessage> deleteCategory(@PathVariable("categoryId") UUID categoryId,
                                                 @RequestParam(defaultValue = "false") boolean force) {
        categoryService.deleteCategoryById(categoryId, force);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseMessage("Category with id: " + categoryId + " was deleted successfully."));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> fetchCategory(@PathVariable("categoryId") UUID categoryId) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(categoryService.getCategoryById(categoryId));
    }

    @GetMapping
    public ResponseEntity<PageResponse<CategoryResponse>> fetchAll(
                                                        @RequestParam(defaultValue = "0") @Min(0) int page,
                                                        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                                        @RequestParam(defaultValue = "NAME") ProductCategorySortField sortBy,
                                                        @RequestParam(defaultValue = "ASC") Sort.Direction sortDir) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(categoryService.getAllCategories(page, size, sortBy, sortDir));
    }
}
