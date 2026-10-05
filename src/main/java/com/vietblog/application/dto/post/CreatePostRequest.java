package com.vietblog.application.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class CreatePostRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    private String content;

    private String excerpt;

    @NotNull(message = "Danh mục không được để trống")
    private String categoryId;

    private List<String> tags;
    private String coverImage;
    
    // Draft or Published
    private String status;

    private Double latitude;
    private Double longitude;
}
