package com.arcticsurge.cosmolab.application.ehr;

import java.util.UUID;

public class EhrNotFoundException extends RuntimeException {
    public EhrNotFoundException(UUID ehrId) {
        super("EHR " + ehrId + " not found");
    }
}
