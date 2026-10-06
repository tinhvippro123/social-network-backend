package com.vietblog.domain.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // --- Lỗi chung ---
    INTERNAL_SERVER_ERROR("ERR-001", "Đã xảy ra lỗi hệ thống"),
    INVALID_INPUT("ERR-002", "Dữ liệu đầu vào không hợp lệ"),
    
    // --- Lỗi liên quan đến User ---
    USER_NOT_FOUND("USR-001", "Người dùng không tồn tại"),
    USER_ALREADY_EXISTS("USR-002", "Tên đăng nhập đã được sử dụng"),
    
    // --- Lỗi liên quan đến Bài viết (Post) ---
    POST_NOT_FOUND("PST-001", "Bài viết không tồn tại"),
    POST_TITLE_EMPTY("PST-002", "Tiêu đề bài viết không được để trống"),
    
    // --- Lỗi liên quan đến Danh mục (Category) ---
    CATEGORY_NOT_FOUND("CAT-001", "Danh mục không tồn tại"),
    CATEGORY_ALREADY_EXISTS("CAT-002", "Tên danh mục đã tồn tại"),
    
    // --- Lỗi liên quan đến Bình luận (Comment) ---
    COMMENT_NOT_FOUND("CMT-001", "Bình luận không tồn tại");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
