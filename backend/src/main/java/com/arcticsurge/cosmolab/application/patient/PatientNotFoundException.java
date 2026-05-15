package com.arcticsurge.cosmolab.application.patient;

import java.util.UUID;

public class PatientNotFoundException extends RuntimeException {
    public PatientNotFoundException(UUID id) {
        super("Patient " + id + " not found");
    }
}
