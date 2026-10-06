package com.vietblog.domain.exception;

public class BusinessRuleException extends DomainException {
    public BusinessRuleException(ErrorCode errorCode) {
        super(errorCode);
    }
}
