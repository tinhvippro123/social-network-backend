package com.vietblog.application.dto.reaction;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReactionRequest {
    @NotBlank(message = "Emoji không được để trống")
    private String emoji; // Ví dụ: "UPVOTE", "DOWNVOTE", "LIKE", "HEART"
}
