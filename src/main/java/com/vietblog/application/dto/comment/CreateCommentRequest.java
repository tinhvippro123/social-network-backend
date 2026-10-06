package com.vietblog.application.dto.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCommentRequest {
    @NotBlank(message = "Nội dung bình luận không được để trống")
    private String content;

    private String parentId; // Nếu là null -> Bình luận cấp 1. Nếu có giá trị -> Là Reply.
}
