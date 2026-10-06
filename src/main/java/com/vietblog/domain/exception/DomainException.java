package com.vietblog.domain.exception;

/**
 * Base exception cho toàn bộ logic nghiệp vụ (Domain).
 * Tuyệt đối KHÔNG chứa code của thư viện Spring ở đây (không có @ResponseStatus).
 */
import lombok.Getter;

@Getter
public class DomainException extends RuntimeException {
    private final ErrorCode errorCode;

    public DomainException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
