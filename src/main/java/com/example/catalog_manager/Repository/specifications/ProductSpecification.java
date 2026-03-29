package com.example.catalog_manager.Repository.specifications;

import com.example.catalog_manager.controller.exceptions.BadRequestException;
import com.example.catalog_manager.dto.product.ProductFilter;
import com.example.catalog_manager.entity.Product;
import com.example.catalog_manager.entity.ProductAttributes;
import com.example.catalog_manager.enums.Currency;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public class ProductSpecification {
    public static Specification<Product> withFilter(ProductFilter filter) {
        return Specification
                .allOf(
                        hasProducer(filter.getProducerId()),
                        hasCategory(filter.getCategoryId()),
                        hasNameLike(filter.getName()),
                        hasPriceBetween(filter.getMinPrice(), filter.getMaxPrice()),
                        hasCurrency(filter.getCurrency()),
                        hasAttributes(filter.getAttributes())
                );
    }

    private static Specification<Product> hasProducer(UUID producerId) {
        return (root, query, cb) -> {
            if (producerId == null) return null;
            return cb.equal(root.get("producer").get("id"), producerId);
        };
    }

    private static Specification<Product> hasCategory(UUID categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) return null;
            return cb.equal(root.get("category").get("id"), categoryId);
        };
    }

    private static Specification<Product> hasNameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) return null;
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    private static Specification<Product> hasPriceBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min == null) return cb.lessThanOrEqualTo(root.get("price"), max);
            if (max == null) return cb.greaterThanOrEqualTo(root.get("price"), min);
            return cb.between(root.get("price"), min, max);
        };
    }

    private static Specification<Product> hasCurrency(Currency currency) {
        return (root, query, cb) -> {
            if (currency == null) return null;
            return cb.equal(root.get("currency"), currency);
        };
    }

    private static Specification<Product> hasAttributes(Map<String, Object> attributes) {
        return (root, query, cb) -> {
            if (attributes == null || attributes.isEmpty()) return null;

            Join<Product, ProductAttributes> attributesJoin =
                    root.join("productAttributes", JoinType.LEFT);

            try {
                String attrsJson = new ObjectMapper().writeValueAsString(attributes);
                return cb.isTrue(
                        cb.function(
                                "jsonb_contains",
                                Boolean.class,
                                attributesJoin.get("attributes"),
                                cb.literal(attrsJson)
                        )
                );
            } catch (JsonProcessingException e) {
                throw new BadRequestException("Invalid attributes format");
            }
        };
    }
}