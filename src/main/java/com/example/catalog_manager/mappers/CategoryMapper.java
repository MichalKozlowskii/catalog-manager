package com.example.catalog_manager.mappers;

import com.example.catalog_manager.dto.category.CategoryResponse;
import com.example.catalog_manager.dto.category.CreateCategoryRequest;
import com.example.catalog_manager.dto.category.UpdateCategoryRequest;
import com.example.catalog_manager.entity.ProductCategory;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toResponse(ProductCategory category);

    ProductCategory toEntity(CreateCategoryRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UpdateCategoryRequest request, @MappingTarget ProductCategory category);
}