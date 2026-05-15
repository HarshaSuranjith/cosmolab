package com.arcticsurge.cosmolab.application.ehr;

import com.arcticsurge.cosmolab.domain.ehr.EhrRecord;
import com.arcticsurge.cosmolab.domain.ehr.EhrRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EhrService {

    private final EhrRepository ehrRepository;

    public EhrRecord getById(UUID ehrId) {
        return ehrRepository.findById(ehrId)
                .orElseThrow(() -> new EhrNotFoundException(ehrId));
    }

    public EhrRecord getByPatientId(UUID patientId) {
        return ehrRepository.findBySubjectId(patientId)
                .orElseThrow(() -> new EhrNotFoundException(patientId));
    }

    @Transactional
    public EhrRecord create(UUID patientId) {
        if (ehrRepository.existsBySubjectId(patientId)) {
            return ehrRepository.findBySubjectId(patientId).get();
        }
        EhrRecord ehr = new EhrRecord();
        ehr.setSubjectId(patientId);
        return ehrRepository.save(ehr);
    }
}
