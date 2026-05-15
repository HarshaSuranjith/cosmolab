package com.arcticsurge.cosmolab.domain;

public abstract class EntityNotFoundException extends RuntimeException {
    protected EntityNotFoundException(String message) {
        super(message);
    }
}
