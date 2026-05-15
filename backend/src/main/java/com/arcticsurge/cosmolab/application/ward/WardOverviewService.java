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
        return patientRepository
                .findByWardAndStatus(wardId, PatientStatus.ACTIVE, PageRequest.of(0, 100))
                .getContent()
                .stream()
                .flatMap(patient -> ehrRepository.findBySubjectId(patient.getId())
                        .map(ehr -> buildSummary(patient, ehr))
                        .stream())
                .toList();
    }

    private WardPatientSummary buildSummary(Patient patient, EhrRecord ehr) {
        UUID ehrId = ehr.getEhrId();
        Optional<VitalSigns> vitals = vitalSignsRepository.findLatestByEhrId(ehrId);
        long activeProblemCount = problemListRepository.countByEhrIdAndStatus(ehrId, ProblemStatus.ACTIVE);
        return new WardPatientSummary(
                patient,
                ehr,
                vitals.orElse(null),
                activeProblemCount,
                vitals.map(this::deriveFlags).orElseGet(List::of)
        );
    }

    private List<String> deriveFlags(VitalSigns v) {
        var flags = new ArrayList<String>();
        Optional.ofNullable(v.getSystolicBp()).filter(bp -> bp > 140).ifPresent(bp -> flags.add("systolicBP:HIGH"));
        Optional.ofNullable(v.getSystolicBp()).filter(bp -> bp < 90).ifPresent(bp -> flags.add("systolicBP:LOW"));
        Optional.ofNullable(v.getHeartRate()).filter(hr -> hr > 100).ifPresent(hr -> flags.add("heartRate:HIGH"));
        Optional.ofNullable(v.getHeartRate()).filter(hr -> hr < 60).ifPresent(hr -> flags.add("heartRate:LOW"));
        Optional.ofNullable(v.getTemperature())
                .filter(t -> t.compareTo(new BigDecimal("37.2")) > 0)
                .ifPresent(t -> flags.add("temperature:HIGH"));
        Optional.ofNullable(v.getOxygenSaturation())
                .filter(s -> s.compareTo(new BigDecimal("95")) < 0)
                .ifPresent(s -> flags.add("oxygenSaturation:LOW"));
        return List.copyOf(flags);
    }

    public record WardPatientSummary(
            Patient patient,
            EhrRecord ehr,
            VitalSigns latestVitals,
            long activeProblemCount,
            List<String> flags
    ) {}
}
