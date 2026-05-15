package com.arcticsurge.cosmolab.application.observation;

import com.arcticsurge.cosmolab.domain.observation.VitalSigns;
import com.arcticsurge.cosmolab.domain.observation.VitalSignsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VitalSignsService {

    private final VitalSignsRepository vitalSignsRepository;

    public List<VitalSigns> listByEhr(UUID ehrId, Instant from, Instant to) {
        return vitalSignsRepository.findByEhrIdBetween(ehrId, from, to);
    }

    public Optional<VitalSigns> getLatest(UUID ehrId) {
        return vitalSignsRepository.findLatestByEhrId(ehrId);
    }

    @Transactional
    public VitalSigns record(VitalSigns vitalSigns) {
        return vitalSignsRepository.save(vitalSigns);
    }
}
