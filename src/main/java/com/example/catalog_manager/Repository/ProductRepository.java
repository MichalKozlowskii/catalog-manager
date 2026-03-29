package com.example.catalog_manager.Repository;

import com.example.catalog_manager.entity.Producer;
import com.example.catalog_manager.entity.Product;
import com.example.catalog_manager.entity.ProductCategory;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.deletedAt = CURRENT_TIMESTAMP WHERE p.producer.id = :producerId")
    void softDeleteByProducerId(@Param("producerId") UUID producerId);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.deletedAt = CURRENT_TIMESTAMP WHERE p.category.id = :categoryId")
    void softDeleteByCategoryId(@Param("categoryId") UUID categoryId);

    long countByProducer(Producer producer);

    long countByCategory(ProductCategory productCategory);

    Page<Product> findAll(Specification<Product> productSpecification, Pageable pageable);
}
