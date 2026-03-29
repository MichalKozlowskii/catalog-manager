package com.example.catalog_manager.service;

import com.example.catalog_manager.dto.PageResponse;
import com.example.catalog_manager.dto.producer.CreateProducerRequest;
import com.example.catalog_manager.dto.producer.ProducerResponse;
import com.example.catalog_manager.dto.producer.UpdateProducerRequest;
import com.example.catalog_manager.enums.ProducerSortField;
import org.springframework.data.domain.Sort;

import java.util.UUID;

public interface ProducerService {
    UUID createProducer(CreateProducerRequest createProducerRequest);
    void updateProducerById(UUID producerId, UpdateProducerRequest updateProducerRequest);
    void deleteProducerById(UUID producerId, boolean force);
    ProducerResponse getProducerById(UUID producerId);
    PageResponse<ProducerResponse> getAllProducers(String country, int page, int size,
                                                   ProducerSortField sortBy, Sort.Direction sortDir);
}
