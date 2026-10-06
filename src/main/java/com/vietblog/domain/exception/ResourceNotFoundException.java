package com.vietblog.domain.exception;

public class ResourceNotFoundException extends DomainException {
    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}
