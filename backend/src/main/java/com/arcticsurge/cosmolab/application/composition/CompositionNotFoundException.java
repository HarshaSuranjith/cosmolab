package com.arcticsurge.cosmolab.application.composition;

import java.util.UUID;

public class CompositionNotFoundException extends RuntimeException {
    public CompositionNotFoundException(UUID id) {
        super("Composition " + id + " not found");
    }
}
