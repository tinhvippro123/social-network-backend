package com.vietblog.domain.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // --- L?i chung ---
    INTERNAL_SERVER_ERROR("Đã xảy ra lỗi hệ thống"),
    INVALID_INPUT("Dữ liệu đầu vào không hợp lệ"),
    
    // --- L?i li?n quan ??n User ---
    USER_NOT_FOUND("Người dùng không tồn tại"),
    USER_ALREADY_EXISTS("Tên đăng nhập đã được sử dụng"),
    
    // --- L?i li?n quan ??n B?i vi?t (Post) ---
    POST_NOT_FOUND("Bài viết không tồn tại"),
    POST_TITLE_EMPTY("Tiêu đề bài viết không được để trống"),
    
    // --- L?i li?n quan ??n Danh m?c (Category) ---
    CATEGORY_NOT_FOUND("Danh mục không tồn tại"),
    CATEGORY_ALREADY_EXISTS("Tên danh mục đã tồn tại"),
    
    // --- L?i li?n quan ??n B?nh lu?n (Comment) ---
    COMMENT_NOT_FOUND("Bình luận không tồn tại");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }
}
