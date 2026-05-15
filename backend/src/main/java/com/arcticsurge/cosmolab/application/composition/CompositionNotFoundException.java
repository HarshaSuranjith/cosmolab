package com.arcticsurge.cosmolab.application.composition;

import com.arcticsurge.cosmolab.domain.EntityNotFoundException;

import java.util.UUID;

public class CompositionNotFoundException extends EntityNotFoundException {
    public CompositionNotFoundException(UUID id) {
        super("Composition " + id + " not found");
    }
}
