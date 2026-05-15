package com.arcticsurge.cosmolab.infrastructure.persistence;

import com.arcticsurge.cosmolab.domain.patient.Patient;
import com.arcticsurge.cosmolab.domain.patient.PatientStatus;
import org.springframework.data.jpa.domain.Specification;

public final class PatientSpecifications {

    private PatientSpecifications() {}

    public static Specification<Patient> wardEquals(String ward) {
        return (root, query, cb) ->
                ward == null ? null : cb.equal(root.get("ward"), ward);
    }

    public static Specification<Patient> statusEquals(PatientStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Patient> nameContains(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) return null;
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern),
                    cb.like(cb.lower(root.get("lastName")), pattern));
        };
    }
}
