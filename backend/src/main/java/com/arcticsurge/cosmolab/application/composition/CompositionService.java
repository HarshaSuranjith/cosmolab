package com.arcticsurge.cosmolab.application.composition;

import com.arcticsurge.cosmolab.domain.composition.Composition;
import com.arcticsurge.cosmolab.domain.composition.CompositionRepository;
import com.arcticsurge.cosmolab.domain.composition.CompositionType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompositionService {

    private final CompositionRepository compositionRepository;

    public Composition getById(UUID id, UUID ehrId) {
        return compositionRepository.findById(id)
                .filter(c -> c.getEhrId().equals(ehrId))
                .orElseThrow(() -> new CompositionNotFoundException(id));
    }

    public Page<Composition> listByEhr(UUID ehrId, CompositionType type, Pageable pageable) {
        if (type != null) {
            return compositionRepository.findByEhrIdAndType(ehrId, type, pageable);
        }
        return compositionRepository.findByEhrId(ehrId, pageable);
    }

    @Transactional
    public Composition create(Composition composition) {
        return compositionRepository.save(composition);
    }

    @Transactional
    public Composition update(UUID id, UUID ehrId, Composition updated) {
        Composition existing = getById(id, ehrId);
        existing.setType(updated.getType());
        existing.setStartTime(updated.getStartTime());
        existing.setFacilityName(updated.getFacilityName());
        existing.setStatus(updated.getStatus());
        return compositionRepository.save(existing);
    }
}
