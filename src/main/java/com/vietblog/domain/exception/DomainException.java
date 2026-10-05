package com.vietblog.domain.exception;

/**
 * Base exception cho toàn bộ logic nghiệp vụ (Domain).
 * Tuyệt đối KHÔNG chứa code của thư viện Spring ở đây (không có @ResponseStatus).
 */
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
