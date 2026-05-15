package com.arcticsurge.cosmolab.application.ward;

import com.arcticsurge.cosmolab.domain.patient.PatientStatus;
import com.arcticsurge.cosmolab.infrastructure.persistence.JpaWardOverviewRepository;
import com.arcticsurge.cosmolab.infrastructure.persistence.WardOverviewRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WardOverviewService {

    private final JpaWardOverviewRepository wardOverviewRepository;

    public List<WardPatientSummary> getOverview(String wardId) {
        return wardOverviewRepository
                .findByWardAndPatientStatus(wardId, PatientStatus.ACTIVE)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    private WardPatientSummary toSummary(WardOverviewRecord r) {
        WardPatientSummary.VitalsSnapshot vitals = r.getVitalsRecordedAt() == null ? null
                : new WardPatientSummary.VitalsSnapshot(
                        r.getVitalsRecordedAt(),
                        r.getSystolicBp(),
                        r.getDiastolicBp(),
                        r.getHeartRate(),
                        r.getTemperature(),
                        r.getOxygenSaturation());

        return new WardPatientSummary(
                r.getPatientId(),
                r.getEhrId(),
                r.getFirstName(),
                r.getLastName(),
                r.getPatientStatus(),
                vitals,
                r.getActiveProblemCount(),
                deriveFlags(r));
    }

    private List<String> deriveFlags(WardOverviewRecord v) {
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
            UUID patientId,
            UUID ehrId,
            String firstName,
            String lastName,
            PatientStatus patientStatus,
            VitalsSnapshot vitals,
            long activeProblemCount,
            List<String> flags) {

        public record VitalsSnapshot(
                Instant recordedAt,
                Integer systolicBp,
                Integer diastolicBp,
                Integer heartRate,
                BigDecimal temperature,
                BigDecimal oxygenSaturation) {}
    }
}
