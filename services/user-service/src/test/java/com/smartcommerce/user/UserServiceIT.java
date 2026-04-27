package com.smartcommerce.user;

import com.smartcommerce.common.events.BaseEvent;
import com.smartcommerce.common.events.EventType;
import com.smartcommerce.common.events.payload.UserRegisteredPayload;
import com.smartcommerce.common.test.AbstractIntegrationTest;
import com.smartcommerce.user.api.dto.*;
import com.smartcommerce.user.domain.AddressType;
import com.smartcommerce.user.event.consumer.UserRegisteredConsumer;
import com.smartcommerce.user.repository.*;
import com.smartcommerce.user.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserServiceIT extends AbstractIntegrationTest {

    @Autowired UserRegisteredConsumer userRegisteredConsumer;
    @Autowired UserProfileService userProfileService;
    @Autowired AddressService addressService;
    @Autowired UserProfileRepository userProfileRepository;
    @Autowired AddressRepository addressRepository;
    @Autowired OutboxRepository outboxRepository;

    @Test
    @DisplayName("USER_REGISTERED event creates profile once")
    void userRegistered_createsProfile_idempotently() {
        var userId = UUID.randomUUID();
        var event = BaseEvent.create(EventType.USER_REGISTERED, userId.toString(), "USER",
            UserRegisteredPayload.builder()
                .userId(userId)
                .email("user-" + userId + "@test.com")
                .firstName("Test")
                .lastName("User")
                .roles(Set.of("CUSTOMER"))
                .build());

        userRegisteredConsumer.onUserRegistered(event);
        userRegisteredConsumer.onUserRegistered(event);

        var profile = userProfileService.getByUserId(userId);
        assertThat(profile.email()).isEqualTo("user-" + userId + "@test.com");
        assertThat(userProfileRepository.findAll().stream().filter(p -> p.getUserId().equals(userId))).hasSize(1);
    }

    @Test
    @DisplayName("update profile increments version and publishes event")
    void updateProfile_incrementsVersionAndPublishesEvent() {
        var userId = createProfile();
        var before = userProfileRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow().getVersion();

        var updated = userProfileService.update(userId, new UpdateProfileRequest(
            "Updated", null, "5551112233", null, null, null, null, null, true));

        assertThat(updated.firstName()).isEqualTo("Updated");
        assertThat(updated.version()).isGreaterThan(before);
        assertThat(outboxRepository.findAll())
            .anySatisfy(event -> assertThat(event.getEventType()).isEqualTo(EventType.USER_PROFILE_UPDATED));
    }

    @Test
    @DisplayName("second default address unsets first default")
    void addAddress_whenSecondDefault_unsetsPreviousDefault() {
        var userId = createProfile();

        var first = addressService.create(userId, addressRequest("Ev", true, true));
        var second = addressService.create(userId, addressRequest("Is", true, true));

        var addresses = addressRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
        assertThat(addresses).hasSize(2);
        assertThat(addresses.stream().filter(address -> address.isDefaultShipping()).map(address -> address.getId()))
            .containsExactly(second.id());
        assertThat(addresses.stream().filter(address -> address.isDefaultBilling()).map(address -> address.getId()))
            .containsExactly(second.id());
        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @Test
    @DisplayName("KVKK delete soft deletes profile and publishes USER_DELETED")
    void requestDeletion_softDeletesAndPublishesEvent() {
        var userId = createProfile();

        userProfileService.requestDeletion(userId);

        assertThat(userProfileRepository.findByUserIdAndDeletedAtIsNull(userId)).isEmpty();
        assertThat(outboxRepository.findAll())
            .anySatisfy(event -> assertThat(event.getEventType()).isEqualTo(EventType.USER_DELETED));
    }

    private UUID createProfile() {
        var userId = UUID.randomUUID();
        var event = BaseEvent.create(EventType.USER_REGISTERED, userId.toString(), "USER",
            UserRegisteredPayload.builder()
                .userId(userId)
                .email("profile-" + userId + "@test.com")
                .firstName("Profile")
                .lastName("Owner")
                .roles(Set.of("CUSTOMER"))
                .build());
        userRegisteredConsumer.onUserRegistered(event);
        return userId;
    }

    private CreateAddressRequest addressRequest(String label, boolean defaultShipping, boolean defaultBilling) {
        return new CreateAddressRequest(label, "Atakan Test", "5551112233", "TR", "Istanbul",
            "Kadikoy", "Caferaga", "Moda Caddesi", "1", "2", "34710",
            "Moda Caddesi No:1 D:2 Kadikoy/Istanbul", defaultShipping, defaultBilling, AddressType.HOME);
    }
}
