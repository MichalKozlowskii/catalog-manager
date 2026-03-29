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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProducerServiceImplTest {

    @Mock
    private ProducerRepository producerRepository;

    @Mock
    private ProducerMapper producerMapper;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProducerServiceImpl producerService;

    private Producer producer;
    private UUID producerId;

    @BeforeEach
    void setUp() {
        producerId = UUID.randomUUID();
        producer = Producer.builder()
                .id(producerId)
                .name("Samsung")
                .country("South Korea")
                .email("contact@samsung.com")
                .build();
    }

    // ---- createProducer ----

    @Test
    void createProducer_shouldSaveAndReturnId() {
        CreateProducerRequest request = CreateProducerRequest.builder()
                .name("Samsung")
                .country("South Korea")
                .email("contact@samsung.com")
                .build();

        when(producerMapper.toEntity(request)).thenReturn(producer);
        when(producerRepository.save(producer)).thenReturn(producer);

        UUID result = producerService.createProducer(request);

        assertThat(result).isEqualTo(producerId);
        verify(producerMapper).toEntity(request);
        verify(producerRepository).save(producer);
    }

    // ---- updateProducerById ----

    @Test
    void updateProducerById_shouldUpdateExistingProducer() {
        UpdateProducerRequest request = UpdateProducerRequest.builder()
                .name("Samsung Electronics")
                .build();

        when(producerRepository.findById(producerId)).thenReturn(Optional.of(producer));

        producerService.updateProducerById(producerId, request);

        verify(producerRepository).findById(producerId);
        verify(producerMapper).updateEntity(request, producer);
    }

    @Test
    void updateProducerById_whenNotFound_shouldThrowResourceNotFoundException() {
        UpdateProducerRequest request = UpdateProducerRequest.builder()
                .name("Samsung Electronics")
                .build();

        when(producerRepository.findById(producerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> producerService.updateProducerById(producerId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(producerId.toString());

        verify(producerMapper, never()).updateEntity(any(), any());
    }

    // ---- deleteProducerById ----

    // ---- force=false ----

    @Test
    void deleteProducer_whenNoActiveProducts_shouldSoftDelete() {
        when(producerRepository.findById(producerId)).thenReturn(Optional.of(producer));
        when(productRepository.countByProducer(producer)).thenReturn(0L);

        producerService.deleteProducerById(producerId, false);

        assertThat(producer.getDeletedAt()).isNotNull();
        verify(productRepository, never()).softDeleteByProducerId(any());
    }

    @Test
    void deleteProducer_whenHasActiveProducts_shouldThrowBusinessException() {
        when(producerRepository.findById(producerId)).thenReturn(Optional.of(producer));
        when(productRepository.countByProducer(producer)).thenReturn(5L);

        assertThatThrownBy(() -> producerService.deleteProducerById(producerId, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("5");

        assertThat(producer.getDeletedAt()).isNull();
        verify(productRepository, never()).softDeleteByProducerId(any());
    }

    @Test
    void deleteProducer_whenNotFound_shouldThrowResourceNotFoundException() {
        when(producerRepository.findById(producerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> producerService.deleteProducerById(producerId, false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(producerId.toString());

        verify(productRepository, never()).countByProducer(any());
        verify(productRepository, never()).softDeleteByProducerId(any());
    }

    // ---- force=true ----

    @Test
    void deleteProducer_withForce_shouldSoftDeleteProductsAndProducer() {
        when(producerRepository.findById(producerId)).thenReturn(Optional.of(producer));

        producerService.deleteProducerById(producerId, true);

        verify(productRepository).softDeleteByProducerId(producerId);
        assertThat(producer.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteProducer_withForce_shouldNotCheckActiveProducts() {
        when(producerRepository.findById(producerId)).thenReturn(Optional.of(producer));

        producerService.deleteProducerById(producerId, true);

        verify(productRepository, never()).countByProducer(any());
    }

    @Test
    void deleteProducer_withForce_whenNotFound_shouldThrowResourceNotFoundException() {
        when(producerRepository.findById(producerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> producerService.deleteProducerById(producerId, true))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(producerId.toString());

        verify(productRepository, never()).softDeleteByProducerId(any());
    }

    // ---- soft delete weryfikacja ----

    @Test
    void deleteProducer_shouldSetDeletedAtToNow() {
        when(producerRepository.findById(producerId)).thenReturn(Optional.of(producer));
        when(productRepository.countByProducer(producer)).thenReturn(0L);

        LocalDateTime before = LocalDateTime.now();
        producerService.deleteProducerById(producerId, false);
        LocalDateTime after = LocalDateTime.now();

        assertThat(producer.getDeletedAt())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }

    // ---- getProducerById ----

    @Test
    void getProducerById_shouldReturnMappedResponse() {
        ProducerResponse response = ProducerResponse.builder()
                .id(producerId)
                .name("Samsung")
                .country("South Korea")
                .email("contact@samsung.com")
                .build();

        when(producerRepository.findById(producerId)).thenReturn(Optional.of(producer));
        when(producerMapper.toResponse(producer)).thenReturn(response);

        ProducerResponse result = producerService.getProducerById(producerId);

        assertThat(result).isEqualTo(response);
        assertThat(result.getId()).isEqualTo(producerId);
        assertThat(result.getName()).isEqualTo("Samsung");
    }

    @Test
    void getProducerById_whenNotFound_shouldThrowResourceNotFoundException() {
        when(producerRepository.findById(producerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> producerService.getProducerById(producerId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(producerId.toString());

        verify(producerMapper, never()).toResponse(any());
    }

    // ---- getAllProducers ----

    @Test
    void getAllProducers_withoutCountry_shouldReturnAllProducers() {
        Producer producer2 = Producer.builder()
                .id(UUID.randomUUID())
                .name("LG")
                .country("South Korea")
                .email("contact@lg.com")
                .build();

        ProducerResponse response1 = ProducerResponse.builder().id(producerId).name("Samsung").build();
        ProducerResponse response2 = ProducerResponse.builder().id(producer2.getId()).name("LG").build();

        Page<Producer> producerPage = new PageImpl<>(
                List.of(producer, producer2),
                PageRequest.of(0, 20),
                2
        );

        when(producerRepository.findAll(any(Pageable.class))).thenReturn(producerPage);
        when(producerMapper.toResponse(producer)).thenReturn(response1);
        when(producerMapper.toResponse(producer2)).thenReturn(response2);

        PageResponse<ProducerResponse> result = producerService.getAllProducers(
                null, 0, 20, ProducerSortField.NAME, Sort.Direction.ASC);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.isLast()).isTrue();
        verify(producerRepository).findAll(any(Pageable.class));
        verify(producerRepository, never()).findByCountryIgnoreCase(any(), any());
    }

    @Test
    void getAllProducers_withCountry_shouldFilterByCountry() {
        ProducerResponse response = ProducerResponse.builder()
                .id(producerId)
                .name("Samsung")
                .country("South Korea")
                .build();

        Page<Producer> producerPage = new PageImpl<>(
                List.of(producer),
                PageRequest.of(0, 20),
                1
        );

        when(producerRepository.findByCountryIgnoreCase(eq("South Korea"), any(Pageable.class)))
                .thenReturn(producerPage);
        when(producerMapper.toResponse(producer)).thenReturn(response);

        PageResponse<ProducerResponse> result = producerService.getAllProducers(
                "South Korea", 0, 20, ProducerSortField.NAME, Sort.Direction.ASC);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCountry()).isEqualTo("South Korea");
        verify(producerRepository).findByCountryIgnoreCase(eq("South Korea"), any(Pageable.class));
        verify(producerRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getAllProducers_withBlankCountry_shouldReturnAllProducers() {
        Page<Producer> producerPage = new PageImpl<>(List.of(producer), PageRequest.of(0, 20), 1);

        when(producerRepository.findAll(any(Pageable.class))).thenReturn(producerPage);
        when(producerMapper.toResponse(producer)).thenReturn(new ProducerResponse());

        producerService.getAllProducers("   ", 0, 20, ProducerSortField.NAME, Sort.Direction.ASC);

        verify(producerRepository).findAll(any(Pageable.class));
        verify(producerRepository, never()).findByCountryIgnoreCase(any(), any());
    }

    @Test
    void getAllProducers_shouldReturnCorrectPageMetadata() {
        List<Producer> producers = List.of(producer);
        Page<Producer> producerPage = new PageImpl<>(producers, PageRequest.of(1, 5), 15);

        when(producerRepository.findAll(any(Pageable.class))).thenReturn(producerPage);
        when(producerMapper.toResponse(any())).thenReturn(new ProducerResponse());

        PageResponse<ProducerResponse> result = producerService.getAllProducers(
                null, 2, 5, ProducerSortField.CREATED_AT, Sort.Direction.DESC);

        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(15);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.isLast()).isFalse();
    }

    @Test
    void getAllProducers_shouldPassCorrectSortToRepository() {
        Page<Producer> producerPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(producerRepository.findAll(any(Pageable.class))).thenReturn(producerPage);

        producerService.getAllProducers(null, 0, 20, ProducerSortField.COUNTRY, Sort.Direction.DESC);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(producerRepository).findAll(pageableCaptor.capture());

        Pageable captured = pageableCaptor.getValue();
        assertThat(captured.getSort().getOrderFor("country")).isNotNull();
        assertThat(captured.getSort().getOrderFor("country").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }
}