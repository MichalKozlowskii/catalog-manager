package com.example.catalog_manager.service;

import com.example.catalog_manager.dto.PageResponse;
import com.example.catalog_manager.dto.product.CreateProductRequest;
import com.example.catalog_manager.dto.product.ProductFilter;
import com.example.catalog_manager.dto.product.ProductResponse;
import com.example.catalog_manager.dto.product.UpdateProductRequest;
import com.example.catalog_manager.enums.ProductSortField;
import org.springframework.data.domain.Sort;

import java.util.UUID;

public interface ProductService {
     UUID createProduct(CreateProductRequest request);
     void updateProductById(UUID productId, UpdateProductRequest request);
     void deleteProductById(UUID productId);
     ProductResponse getProductById(UUID productId);
     PageResponse<ProductResponse> getAllProducts(ProductFilter filter, int page, int size,
                                                  ProductSortField sortBy, Sort.Direction sortDir);
}
