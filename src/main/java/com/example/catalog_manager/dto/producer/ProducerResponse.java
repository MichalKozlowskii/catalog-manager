package com.example.catalog_manager.dto.producer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProducerResponse {
    private UUID id;
    private String name;
    private String country;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}