package com.arcticsurge.cosmolab.application.evaluation;

import com.arcticsurge.cosmolab.domain.EntityNotFoundException;

import java.util.UUID;

public class ProblemListEntryNotFoundException extends EntityNotFoundException {
    public ProblemListEntryNotFoundException(UUID id) {
        super("Problem list entry " + id + " not found");
    }
}
