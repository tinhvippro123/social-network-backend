package com.vietblog.domain.exception;

public class ResourceNotFoundException extends DomainException {
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s không tồn tại với %s : '%s'", resourceName, fieldName, fieldValue));
    }
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
