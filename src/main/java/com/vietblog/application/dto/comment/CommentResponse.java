package com.vietblog.application.dto.comment;

import com.vietblog.domain.entity.Comment;
import com.vietblog.domain.search.TextAlgorithms;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class CommentResponse {
    private String id;
    private String content;
    private String authorId;
    private String authorName;
    private int upvotes;
    private int downvotes;
    private LocalDateTime createdAt;
    private List<CommentResponse> replies; // Đệ quy chứa danh sách reply
    private String sentiment; // "positive", "negative", "neutral"
    private double sentimentScore;

    public static CommentResponse fromEntity(Comment comment) {
        if (comment == null) return null;

        var sentimentResult = TextAlgorithms.sentiment(comment.getContent());

        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getName())
                .upvotes(comment.getUpvotes())
                .downvotes(comment.getDownvotes())
                .createdAt(comment.getCreatedAt())
                .sentiment(sentimentResult.label())
                .sentimentScore(sentimentResult.score())
                .replies(comment.getReplies() != null 
                        ? comment.getReplies().stream()
                                .map(CommentResponse::fromEntity) // Đệ quy map các reply
                                .collect(Collectors.toList())
                        : List.of())
                .build();
    }
}
