package com.arcticsurge.cosmolab.application.ward;

import com.arcticsurge.cosmolab.domain.evaluation.ProblemListRepository;
import com.arcticsurge.cosmolab.domain.evaluation.ProblemStatus;
import com.arcticsurge.cosmolab.domain.ehr.EhrRecord;
import com.arcticsurge.cosmolab.domain.ehr.EhrRepository;
import com.arcticsurge.cosmolab.domain.observation.VitalSigns;
import com.arcticsurge.cosmolab.domain.observation.VitalSignsRepository;
import com.arcticsurge.cosmolab.domain.patient.Patient;
import com.arcticsurge.cosmolab.domain.patient.PatientRepository;
import com.arcticsurge.cosmolab.domain.patient.PatientStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WardOverviewService {

    private final PatientRepository patientRepository;
    private final EhrRepository ehrRepository;
    private final VitalSignsRepository vitalSignsRepository;
    private final ProblemListRepository problemListRepository;

    public List<WardPatientSummary> getOverview(String wardId) {
        List<Patient> patients = patientRepository
                .findByWardAndStatus(wardId, PatientStatus.ACTIVE, PageRequest.of(0, 100))
                .getContent();

        List<WardPatientSummary> summaries = new ArrayList<>();
        for (Patient patient : patients) {
            Optional<EhrRecord> ehr = ehrRepository.findBySubjectId(patient.getId());
            if (ehr.isEmpty()) continue;

            UUID ehrId = ehr.get().getEhrId();
            Optional<VitalSigns> latestVitals = vitalSignsRepository.findLatestByEhrId(ehrId);
            long activeProblemCount = problemListRepository.countByEhrIdAndStatus(ehrId, ProblemStatus.ACTIVE);

            summaries.add(new WardPatientSummary(
                    patient,
                    ehr.get(),
                    latestVitals.orElse(null),
                    activeProblemCount,
                    deriveFlags(latestVitals.orElse(null))
            ));
        }
        return summaries;
    }

    private List<String> deriveFlags(VitalSigns v) {
        List<String> flags = new ArrayList<>();
        if (v == null) return flags;
        if (v.getSystolicBp() != null && v.getSystolicBp() > 140) flags.add("systolicBP:HIGH");
        if (v.getSystolicBp() != null && v.getSystolicBp() < 90)  flags.add("systolicBP:LOW");
        if (v.getHeartRate() != null && v.getHeartRate() > 100)   flags.add("heartRate:HIGH");
        if (v.getHeartRate() != null && v.getHeartRate() < 60)    flags.add("heartRate:LOW");
        if (v.getTemperature() != null && v.getTemperature().compareTo(new BigDecimal("37.2")) > 0) flags.add("temperature:HIGH");
        if (v.getOxygenSaturation() != null && v.getOxygenSaturation().compareTo(new BigDecimal("95")) < 0) flags.add("oxygenSaturation:LOW");
        return flags;
    }

    public record WardPatientSummary(
            Patient patient,
            EhrRecord ehr,
            VitalSigns latestVitals,
            long activeProblemCount,
            List<String> flags
    ) {}
}
