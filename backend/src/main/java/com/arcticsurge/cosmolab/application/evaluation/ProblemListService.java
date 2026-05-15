package com.arcticsurge.cosmolab.application.evaluation;

import com.arcticsurge.cosmolab.domain.evaluation.ProblemListEntry;
import com.arcticsurge.cosmolab.domain.evaluation.ProblemListRepository;
import com.arcticsurge.cosmolab.domain.evaluation.ProblemStatus;
import com.arcticsurge.cosmolab.interfaces.rest.dto.ProblemRequest;
import com.arcticsurge.cosmolab.interfaces.rest.mapper.ProblemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemListService {

    private final ProblemListRepository problemListRepository;
    private final ProblemMapper problemMapper;

    public List<ProblemListEntry> listByEhr(UUID ehrId, ProblemStatus status) {
        return status != null
                ? problemListRepository.findByEhrIdAndStatus(ehrId, status)
                : problemListRepository.findByEhrId(ehrId);
    }

    public ProblemListEntry getById(UUID id) {
        return problemListRepository.findById(id)
                .orElseThrow(() -> new ProblemListEntryNotFoundException(id));
    }

    @Transactional
    public ProblemListEntry create(ProblemListEntry entry) {
        return problemListRepository.save(entry);
    }

    @Transactional
    public ProblemListEntry update(UUID id, ProblemRequest request) {
        ProblemListEntry existing = getById(id);
        problemMapper.merge(request, existing);
        return existing;
    }
}
