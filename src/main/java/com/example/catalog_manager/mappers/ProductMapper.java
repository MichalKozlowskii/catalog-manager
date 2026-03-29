package com.example.catalog_manager.mappers;

import com.example.catalog_manager.dto.product.CreateProductRequest;
import com.example.catalog_manager.dto.product.ProductResponse;
import com.example.catalog_manager.dto.product.UpdateProductRequest;
import com.example.catalog_manager.entity.Producer;
import com.example.catalog_manager.entity.Product;
import com.example.catalog_manager.entity.ProductCategory;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {ProducerMapper.class, CategoryMapper.class})
public interface ProductMapper {

    @Mapping(source = "productAttributes.attributes", target = "attributes")
    @Mapping(source = "producer", target = "producer")
    @Mapping(source = "category", target = "category")
    ProductResponse toResponse(Product product);

    @Mapping(target = "producer", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "productAttributes", ignore = true)
    Product toEntity(CreateProductRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "producer", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "productAttributes", ignore = true)
    void updateEntity(UpdateProductRequest request, @MappingTarget Product product);

    ProductResponse.ProducerInfo toProducerSummary(Producer producer);
    ProductResponse.CategoryInfo toCategorySummary(ProductCategory category);
}