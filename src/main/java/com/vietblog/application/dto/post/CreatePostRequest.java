package com.vietblog.application.dto.post;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
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

    @Size(max = 20)
    private List<@NotBlank @Size(max = 64) String> tags;
    private String coverImage;
    
    // Draft or Published
    @Pattern(regexp = "(?i)DRAFT|PUBLISHED")
    private String status;

    @DecimalMin("-90")
    @DecimalMax("90")
    private Double latitude;
    @DecimalMin("-180")
    @DecimalMax("180")
    private Double longitude;

    @AssertTrue(message = "Latitude and longitude must be provided together")
    public boolean isLocationComplete() { return (latitude == null) == (longitude == null); }
}
