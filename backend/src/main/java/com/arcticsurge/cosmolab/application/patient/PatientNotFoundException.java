package com.arcticsurge.cosmolab.application.patient;

import com.arcticsurge.cosmolab.domain.EntityNotFoundException;

import java.util.UUID;

public class PatientNotFoundException extends EntityNotFoundException {
    public PatientNotFoundException(UUID id) {
        super("Patient " + id + " not found");
    }
}
