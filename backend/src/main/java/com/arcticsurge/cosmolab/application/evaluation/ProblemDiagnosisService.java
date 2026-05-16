package com.arcticsurge.cosmolab.application.evaluation;

import com.arcticsurge.cosmolab.domain.evaluation.ProblemDiagnosis;
import com.arcticsurge.cosmolab.domain.evaluation.ProblemDiagnosisRepository;
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
public class ProblemDiagnosisService {

    private final ProblemDiagnosisRepository problemDiagnosisRepository;
    private final ProblemMapper problemMapper;

    public List<ProblemDiagnosis> listByEhr(UUID ehrId, ProblemStatus status) {
        return status != null
                ? problemDiagnosisRepository.findByEhrIdAndStatus(ehrId, status)
                : problemDiagnosisRepository.findByEhrId(ehrId);
    }

    public ProblemDiagnosis getById(UUID id) {
        return problemDiagnosisRepository.findById(id)
                .orElseThrow(() -> new ProblemDiagnosisNotFoundException(id));
    }

    @Transactional
    public ProblemDiagnosis create(ProblemDiagnosis entry) {
        return problemDiagnosisRepository.save(entry);
    }

    @Transactional
    public ProblemDiagnosis update(UUID id, ProblemRequest request) {
        ProblemDiagnosis existing = getById(id);
        problemMapper.merge(request, existing);
        return existing;
    }
}
