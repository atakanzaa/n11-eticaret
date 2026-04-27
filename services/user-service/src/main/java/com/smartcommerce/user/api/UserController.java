package com.smartcommerce.user.api;

import com.smartcommerce.user.api.dto.*;
import com.smartcommerce.user.service.AddressService;
import com.smartcommerce.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserProfileService userProfileService;
    private final AddressService addressService;

    @GetMapping("/me")
    public UserProfileDto getMyProfile(@AuthenticationPrincipal String userId) {
        return userProfileService.getByUserId(UUID.fromString(userId));
    }

    @PutMapping("/me")
    public UserProfileDto updateMyProfile(@AuthenticationPrincipal String userId,
                                          @Valid @RequestBody UpdateProfileRequest request) {
        return userProfileService.update(UUID.fromString(userId), request);
    }

    @PostMapping("/me/kvkk-consent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void giveKvkkConsent(@AuthenticationPrincipal String userId) {
        userProfileService.giveKvkkConsent(UUID.fromString(userId));
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestDelete(@AuthenticationPrincipal String userId) {
        userProfileService.requestDeletion(UUID.fromString(userId));
    }

    @GetMapping("/me/addresses")
    public List<AddressDto> getMyAddresses(@AuthenticationPrincipal String userId) {
        return addressService.getByUserId(UUID.fromString(userId));
    }

    @PostMapping("/me/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AddressDto addAddress(@AuthenticationPrincipal String userId,
                                 @Valid @RequestBody CreateAddressRequest request) {
        return addressService.create(UUID.fromString(userId), request);
    }

    @PutMapping("/me/addresses/{id}")
    public AddressDto updateAddress(@AuthenticationPrincipal String userId,
                                    @PathVariable UUID id,
                                    @Valid @RequestBody UpdateAddressRequest request) {
        return addressService.update(UUID.fromString(userId), id, request);
    }

    @DeleteMapping("/me/addresses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(@AuthenticationPrincipal String userId, @PathVariable UUID id) {
        addressService.delete(UUID.fromString(userId), id);
    }

    @GetMapping("/internal/addresses/{id}")
    public AddressDto getAddress(@PathVariable UUID id) {
        return addressService.getById(id);
    }

    @GetMapping("/internal/{userId}")
    public UserProfileDto getInternalProfile(@PathVariable UUID userId) {
        return userProfileService.getByUserId(userId);
    }
}
