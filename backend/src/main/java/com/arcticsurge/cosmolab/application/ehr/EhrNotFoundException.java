package com.arcticsurge.cosmolab.application.ehr;

import com.arcticsurge.cosmolab.domain.EntityNotFoundException;

import java.util.UUID;

public class EhrNotFoundException extends EntityNotFoundException {
    public EhrNotFoundException(UUID ehrId) {
        super("EHR " + ehrId + " not found");
    }
}
