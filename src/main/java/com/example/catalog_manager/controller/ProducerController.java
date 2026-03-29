package com.example.catalog_manager.controller;

import com.example.catalog_manager.dto.PageResponse;
import com.example.catalog_manager.dto.ResponseMessage;
import com.example.catalog_manager.dto.producer.CreateProducerRequest;
import com.example.catalog_manager.dto.producer.ProducerResponse;
import com.example.catalog_manager.dto.producer.UpdateProducerRequest;
import com.example.catalog_manager.enums.ProducerSortField;
import com.example.catalog_manager.service.ProducerService;
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
@RequestMapping("/api/producers")
@RequiredArgsConstructor
public class ProducerController {
    private final ProducerService producerService;

    @PostMapping
    public ResponseEntity<ResponseMessage> createProducer(@Valid @RequestBody CreateProducerRequest request) {
        UUID newProducerId = producerService.createProducer(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("location", "/api/producers/" + newProducerId)
                .body(new ResponseMessage("Producer created successfully."));
    }

    @PatchMapping("/{producerId}")
    public ResponseEntity<ResponseMessage> updateProducer(@PathVariable("producerId") UUID producerId,
                                                 @Valid @RequestBody UpdateProducerRequest request) {
        producerService.updateProducerById(producerId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseMessage("Producer with id: " + producerId + " was updated successfully."));
    }

    @DeleteMapping("/{producerId}")
    public ResponseEntity<ResponseMessage> deleteProducer(@PathVariable UUID producerId,
                                                 @RequestParam(defaultValue = "false") boolean force) {

        producerService.deleteProducerById(producerId, force);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseMessage("Producer with id: " + producerId + " was deleted successfully."));
    }

    @GetMapping("/{producerId}")
    public ResponseEntity<ProducerResponse> fetchProducer(@PathVariable("producerId") UUID producerId) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(producerService.getProducerById(producerId));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProducerResponse>> fetchAll(
                                                        @RequestParam(required = false) String country,
                                                        @RequestParam(defaultValue = "0") @Min(0) int page,
                                                        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                                        @RequestParam(defaultValue = "NAME") ProducerSortField sortBy,
                                                        @RequestParam(defaultValue = "ASC") Sort.Direction sortDir) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(producerService.getAllProducers(country, page, size, sortBy, sortDir));
    }
}
