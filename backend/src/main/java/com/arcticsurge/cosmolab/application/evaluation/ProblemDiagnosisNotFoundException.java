package com.arcticsurge.cosmolab.application.evaluation;

import com.arcticsurge.cosmolab.domain.EntityNotFoundException;

import java.util.UUID;

public class ProblemDiagnosisNotFoundException extends EntityNotFoundException {
    public ProblemDiagnosisNotFoundException(UUID id) {
        super("Problem diagnosis " + id + " not found");
    }
}
