package com.vietblog.domain.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // --- Lỗi chung ---
    INTERNAL_SERVER_ERROR("Đã xảy ra lỗi hệ thống"),
    INVALID_INPUT("Dữ liệu đầu vào không hợp lệ"),
    UNAUTHORIZED("Vui lòng đăng nhập để tiếp tục"),
    FORBIDDEN("Bạn không có quyền thực hiện hành động này"),
    
    // --- Lỗi liên quan đến User ---
    USER_NOT_FOUND("Người dùng không tồn tại"),
    USER_ALREADY_EXISTS("Tên đăng nhập đã được sử dụng"),
    EMAIL_ALREADY_EXISTS("Email này đã được sử dụng"),
    CURRENT_PASSWORD_INCORRECT("Mật khẩu hiện tại không chính xác"),
    
    // --- Lỗi liên quan đến Bài viết (Post) ---
    POST_NOT_FOUND("Bài viết không tồn tại"),
    POST_TITLE_EMPTY("Tiêu đề bài viết không được để trống"),
    POST_CONTENT_TOO_SHORT("Nội dung bài viết phải có ít nhất 50 ký tự"),
    
    // --- Lỗi liên quan đến Danh mục (Category) ---
    CATEGORY_NOT_FOUND("Danh mục không tồn tại"),
    CATEGORY_ALREADY_EXISTS("Tên danh mục đã tồn tại"),
    CATEGORY_NAME_EMPTY("Tên danh mục không được để trống"),
    
    // --- Lỗi liên quan đến Bình luận (Comment) ---
    COMMENT_NOT_FOUND("Bình luận không tồn tại"),
    PARENT_COMMENT_NOT_IN_POST("Bình luận cha không thuộc bài viết này"),
    COMMENT_CONTENT_EMPTY("Nội dung bình luận không được để trống"),
    COMMENT_TOO_LONG("Nội dung bình luận không được vượt quá 2000 ký tự"),
    COMMENT_MAX_DEPTH_EXCEEDED("Không thể phản hồi bình luận vượt quá cấp độ cho phép"),
    COMMENT_DELETE_FORBIDDEN("Bạn không có quyền xóa bình luận này"),

    // --- Lỗi liên quan đến Tệp tin (File) ---
    FILE_EMPTY("Tệp tin tải lên không được để trống"),
    FILE_UPLOAD_FAILED("Tải tệp tin lên hệ thống thất bại");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }
}
