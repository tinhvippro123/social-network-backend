package com.vietblog.domain.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // --- L?i chung ---
    INTERNAL_SERVER_ERROR("Đã xảy ra lỗi hệ thống"),
    INVALID_INPUT("Dữ liệu đầu vào không hợp lệ"),
    
    // --- Lỗi liên quan đến User ---
    USER_NOT_FOUND("Người dùng không tồn tại"),
    USER_ALREADY_EXISTS("Tên đăng nhập đã được sử dụng"),
    EMAIL_ALREADY_EXISTS("Email này đã được sử dụng"),
    CURRENT_PASSWORD_INCORRECT("Mật khẩu hiện tại không chính xác"),
    
    // --- Lỗi liên quan đến Bài viết (Post) ---
    POST_NOT_FOUND("Bài viết không tồn tại"),
    POST_TITLE_EMPTY("Tiêu đề bài viết không được để trống"),
    
    // --- Lỗi liên quan đến Danh mục (Category) ---
    CATEGORY_NOT_FOUND("Danh mục không tồn tại"),
    CATEGORY_ALREADY_EXISTS("Tên danh mục đã tồn tại"),
    
    // --- Lỗi liên quan đến Bình luận (Comment) ---
    COMMENT_NOT_FOUND("Bình luận không tồn tại"),
    PARENT_COMMENT_NOT_IN_POST("Bình luận cha không thuộc bài viết này");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }
}
