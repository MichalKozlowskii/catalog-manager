package com.example.catalog_manager.mappers;

import com.example.catalog_manager.dto.producer.CreateProducerRequest;
import com.example.catalog_manager.dto.producer.ProducerResponse;
import com.example.catalog_manager.dto.producer.UpdateProducerRequest;
import com.example.catalog_manager.entity.Producer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ProducerMapperImpl.class})
class ProducerMapperTest {

    @Autowired
    private ProducerMapper mapper;

    // ---- toResponse ----

    @Test
    void toResponse_shouldMapAllFields() {
        Producer producer = Producer.builder()
                .id(UUID.randomUUID())
                .name("Samsung")
                .country("South Korea")
                .email("contact@samsung.com")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ProducerResponse response = mapper.toResponse(producer);

        assertThat(response.getId()).isEqualTo(producer.getId());
        assertThat(response.getName()).isEqualTo(producer.getName());
        assertThat(response.getCountry()).isEqualTo(producer.getCountry());
        assertThat(response.getEmail()).isEqualTo(producer.getEmail());
        assertThat(response.getCreatedAt()).isEqualTo(producer.getCreatedAt());
        assertThat(response.getUpdatedAt()).isEqualTo(producer.getUpdatedAt());
    }

    @Test
    void toResponse_whenNull_shouldReturnNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    // ---- toEntity ----

    @Test
    void toEntity_shouldMapAllFields() {
        CreateProducerRequest request = CreateProducerRequest.builder()
                .name("Samsung")
                .country("South Korea")
                .email("contact@samsung.com")
                .build();

        Producer producer = mapper.toEntity(request);

        assertThat(producer.getName()).isEqualTo(request.getName());
        assertThat(producer.getCountry()).isEqualTo(request.getCountry());
        assertThat(producer.getEmail()).isEqualTo(request.getEmail());
    }

    @Test
    void toEntity_shouldNotSetId() {
        CreateProducerRequest request = CreateProducerRequest.builder()
                .name("Samsung")
                .country("South Korea")
                .email("contact@samsung.com")
                .build();

        Producer producer = mapper.toEntity(request);

        assertThat(producer.getId()).isNull();
    }

    // ---- updateEntity ----

    @Test
    void updateEntity_shouldUpdateOnlyProvidedFields() {
        Producer existing = Producer.builder()
                .id(UUID.randomUUID())
                .name("Samsung")
                .country("South Korea")
                .email("old@samsung.com")
                .build();

        UpdateProducerRequest request = UpdateProducerRequest.builder()
                .email("new@samsung.com")
                .build();

        mapper.updateEntity(request, existing);

        assertThat(existing.getName()).isEqualTo("Samsung");
        assertThat(existing.getCountry()).isEqualTo("South Korea");
        assertThat(existing.getEmail()).isEqualTo("new@samsung.com");
    }

    @Test
    void updateEntity_whenAllFieldsNull_shouldChangeNothing() {
        Producer existing = Producer.builder()
                .id(UUID.randomUUID())
                .name("Samsung")
                .country("South Korea")
                .email("contact@samsung.com")
                .build();

        mapper.updateEntity(new UpdateProducerRequest(), existing);

        assertThat(existing.getName()).isEqualTo("Samsung");
        assertThat(existing.getCountry()).isEqualTo("South Korea");
        assertThat(existing.getEmail()).isEqualTo("contact@samsung.com");
    }
}