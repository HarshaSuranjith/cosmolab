package com.arcticsurge.cosmolab.application.evaluation;

import java.util.UUID;

public class ProblemListEntryNotFoundException extends RuntimeException {
    public ProblemListEntryNotFoundException(UUID id) {
        super("Problem list entry " + id + " not found");
    }
}
