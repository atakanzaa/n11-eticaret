package com.smartcommerce.fraud.service;

import com.smartcommerce.common.errors.ErrorCode;
import com.smartcommerce.common.errors.ResourceNotFoundException;
import com.smartcommerce.fraud.api.dto.CreateBlacklistRequest;
import com.smartcommerce.fraud.domain.FraudBlacklist;
import com.smartcommerce.fraud.repository.FraudBlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlacklistService {

    private final FraudBlacklistRepository repository;

    @Transactional
    public FraudBlacklist add(CreateBlacklistRequest request) {
        return repository.save(FraudBlacklist.builder()
            .blacklistType(request.blacklistType())
            .value(request.value())
            .reason(request.reason())
            .build());
    }

    @Transactional
    public void remove(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Blacklist entry not found");
        }
        repository.deleteById(id);
    }

    public List<FraudBlacklist> listAll() {
        return repository.findAll();
    }
}
