package com.smartcommerce.user.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.common.events.BaseEvent;
import com.smartcommerce.common.events.Topics;
import com.smartcommerce.common.events.payload.UserRegisteredPayload;
import com.smartcommerce.user.domain.ProcessedEvent;
import com.smartcommerce.user.domain.UserProfile;
import com.smartcommerce.user.repository.ProcessedEventRepository;
import com.smartcommerce.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredConsumer {
    private final UserProfileRepository userProfileRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = Topics.USER_REGISTERED, groupId = "user-service")
    @Transactional
    public void onUserRegistered(BaseEvent<?> event) {
        if (event.getEventId() != null && processedEventRepository.existsById(event.getEventId())) {
            return;
        }

        var payload = objectMapper.convertValue(event.getPayload(), UserRegisteredPayload.class);
        if (userProfileRepository.existsByUserId(payload.getUserId())) {
            return;
        }

        var profile = UserProfile.builder()
            .userId(payload.getUserId())
            .email(payload.getEmail())
            .firstName(payload.getFirstName())
            .lastName(payload.getLastName())
            .preferredLanguage("tr")
            .preferredCurrency("TRY")
            .build();
        userProfileRepository.save(profile);
        log.info("Created profile for registered user {}", payload.getUserId());

        if (event.getEventId() != null) {
            processedEventRepository.save(new ProcessedEvent(event.getEventId(), Instant.now()));
        }
    }
}
