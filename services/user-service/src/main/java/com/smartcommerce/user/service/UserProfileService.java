package com.smartcommerce.user.service;

import com.smartcommerce.common.errors.ErrorCode;
import com.smartcommerce.common.errors.ResourceNotFoundException;
import com.smartcommerce.common.events.EventType;
import com.smartcommerce.common.events.Topics;
import com.smartcommerce.user.api.dto.UpdateProfileRequest;
import com.smartcommerce.user.api.dto.UserProfileDto;
import com.smartcommerce.user.api.mapper.UserProfileMapper;
import com.smartcommerce.user.event.outbox.OutboxService;
import com.smartcommerce.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;
    private final OutboxService outboxService;

    public UserProfileDto getByUserId(UUID userId) {
        var profile = userProfileRepository.findByUserIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "User profile not found"));
        return userProfileMapper.toDto(profile);
    }

    @Transactional
    public UserProfileDto update(UUID userId, UpdateProfileRequest request) {
        var profile = userProfileRepository.findByUserIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "User profile not found"));

        if (request.firstName() != null) profile.setFirstName(request.firstName());
        if (request.lastName() != null) profile.setLastName(request.lastName());
        if (request.phone() != null) profile.setPhone(request.phone());
        if (request.dateOfBirth() != null) profile.setDateOfBirth(request.dateOfBirth());
        if (request.gender() != null) profile.setGender(request.gender());
        if (request.avatarUrl() != null) profile.setAvatarUrl(request.avatarUrl());
        if (request.preferredLanguage() != null) profile.setPreferredLanguage(request.preferredLanguage());
        if (request.preferredCurrency() != null) profile.setPreferredCurrency(request.preferredCurrency());
        if (request.marketingConsent() != null) {
            profile.setMarketingConsent(request.marketingConsent());
            profile.setMarketingConsentAt(request.marketingConsent() ? Instant.now() : null);
        }

        profile = userProfileRepository.save(profile);
        outboxService.publish(Topics.USER_PROFILE_UPDATED, EventType.USER_PROFILE_UPDATED,
            profile.getId().toString(), "USER_PROFILE", userProfileMapper.toDto(profile));
        return userProfileMapper.toDto(profile);
    }

    @Transactional
    public void giveKvkkConsent(UUID userId) {
        var profile = userProfileRepository.findByUserIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "User profile not found"));
        profile.setKvkkConsent(true);
        profile.setKvkkConsentAt(Instant.now());
        userProfileRepository.save(profile);
    }

    @Transactional
    public void requestDeletion(UUID userId) {
        var profile = userProfileRepository.findByUserIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "User profile not found"));
        profile.setDeletedAt(Instant.now());
        userProfileRepository.save(profile);
        outboxService.publish(Topics.USER_DELETED, EventType.USER_DELETED,
            profile.getId().toString(), "USER_PROFILE", Map.of("userId", userId, "profileId", profile.getId()));
    }
}
