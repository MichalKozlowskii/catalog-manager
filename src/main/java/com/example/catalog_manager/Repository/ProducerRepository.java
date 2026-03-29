package com.example.catalog_manager.Repository;

import com.example.catalog_manager.entity.Producer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProducerRepository extends JpaRepository<Producer, UUID> {
    Page<Producer> findByCountryIgnoreCase(String country, Pageable pageable);
}
