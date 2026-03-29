package com.example.catalog_manager.service;

import com.example.catalog_manager.Repository.ProducerRepository;
import com.example.catalog_manager.Repository.ProductRepository;
import com.example.catalog_manager.controller.exceptions.BusinessException;
import com.example.catalog_manager.controller.exceptions.ResourceNotFoundException;
import com.example.catalog_manager.dto.PageResponse;
import com.example.catalog_manager.dto.producer.CreateProducerRequest;
import com.example.catalog_manager.dto.producer.ProducerResponse;
import com.example.catalog_manager.dto.producer.UpdateProducerRequest;
import com.example.catalog_manager.entity.Producer;
import com.example.catalog_manager.enums.ProducerSortField;
import com.example.catalog_manager.mappers.ProducerMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProducerServiceImpl implements ProducerService {
    private final ProducerRepository producerRepository;
    private final ProductRepository productRepository;
    private final ProducerMapper producerMapper;

    @Override
    public UUID createProducer(CreateProducerRequest createProducerRequest) {
        return producerRepository.save(producerMapper.toEntity(createProducerRequest)).getId();
    }

    @Override
    @Transactional
    public void updateProducerById(UUID producerId, UpdateProducerRequest updateProducerRequest) {
        Producer existing = producerRepository.findById(producerId).orElseThrow(
                () -> new ResourceNotFoundException("Producer not found with id: " + producerId));

        producerMapper.updateEntity(updateProducerRequest, existing);
    }

    @Override
    @Transactional
    public void deleteProducerById(UUID producerId, boolean force) {
        Producer producer = producerRepository.findById(producerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producer not found with id: " + producerId));

        if (force) {
            productRepository.softDeleteByProducerId(producerId);
        } else {
            long activeProducts = productRepository.countByProducer(producer);
            if (activeProducts > 0) {
                throw new BusinessException("Cannot delete producer with " + activeProducts + " active products. " +
                                "Use force=true to delete all products as well.");
            }
        }

        producer.setDeletedAt(LocalDateTime.now());
    }
    @Override
    public ProducerResponse getProducerById(UUID producerId) {
        return producerMapper.toResponse(producerRepository.findById(producerId).orElseThrow(
                () -> new ResourceNotFoundException("Producer not found with id: " + producerId))
        );
    }

    @Override
    public PageResponse<ProducerResponse> getAllProducers(String country, int page, int size,
                                                          ProducerSortField sortBy, Sort.Direction sortDir) {
        String sortField = switch (sortBy) {
            case NAME -> "name";
            case COUNTRY -> "country";
            case CREATED_AT -> "createdAt";
            case UPDATED_AT -> "updatedAt";
        };

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortField));

        Page<Producer> result = (country != null && !country.isBlank())
                ? producerRepository.findByCountryIgnoreCase(country, pageable)
                : producerRepository.findAll(pageable);

        return PageResponse.<ProducerResponse>builder()
                .content(result.getContent().stream()
                        .map(producerMapper::toResponse)
                        .toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }
}
