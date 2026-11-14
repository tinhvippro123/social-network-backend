package com.vietblog.application.dto.comment;

import com.vietblog.domain.entity.Comment;
import com.vietblog.domain.entity.User;
import com.vietblog.domain.search.TextAlgorithms;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Data
@Builder
public class CommentResponse {
    private static final Pattern REPLY_TO_PREFIX = Pattern.compile("^\\[replyTo:([^|\\]]+)(?:\\|([^\\]]*))?\\]\\s*");

    private String id;
    private String content;
    private String authorId;
    private String authorName;
    private String authorAvatar;
    private String replyTo;
    private String replyToAuthorId;
    private int upvotes;
    private int downvotes;
    private LocalDateTime createdAt;
    private List<CommentResponse> replies; // Đệ quy chứa danh sách reply
    private String sentiment; // "positive", "negative", "neutral"
    private double sentimentScore;

    public static String formatReplyToPrefix(String name, String authorId) {
        String safeName = name == null ? "" : name.replace("|", " ").replace("[", "").replace("]", "").trim();
        if (safeName.isEmpty()) return "";
        String safeId = authorId == null ? "" : authorId.replace("|", "").replace("[", "").replace("]", "").trim();
        return "[replyTo:" + safeName + "|" + safeId + "] ";
    }

    public static boolean hasReplyToPrefix(String content) {
        return content != null && REPLY_TO_PREFIX.matcher(content).find();
    }

    public static CommentResponse fromEntity(Comment comment) {
        if (comment == null) return null;
        List<CommentResponse> replies = comment.getReplies() != null
                ? comment.getReplies().stream()
                        .map(CommentResponse::fromEntity) // Đệ quy map các reply
                        .collect(Collectors.toList())
                : List.of();
        return buildResponse(comment, replies);
    }

    public static CommentResponse fromEntity(Comment comment, Map<String, List<Comment>> repliesByParentId) {
        if (comment == null) return null;
        List<Comment> children = repliesByParentId != null
                ? repliesByParentId.getOrDefault(comment.getId(), List.of())
                : List.of();
        List<CommentResponse> replies = children.stream()
                .map(child -> fromEntity(child, repliesByParentId))
                .collect(Collectors.toList());
        return buildResponse(comment, replies);
    }

    private static CommentResponse buildResponse(Comment comment, List<CommentResponse> replies) {
        String rawContent = comment.getContent() != null ? comment.getContent() : "";
        String cleanContent = rawContent;
        String replyTo = null;
        String replyToAuthorId = null;

        Matcher matcher = REPLY_TO_PREFIX.matcher(rawContent);
        if (matcher.find()) {
            replyTo = matcher.group(1);
            String parsedId = matcher.group(2);
            replyToAuthorId = (parsedId != null && !parsedId.isBlank()) ? parsedId : null;
            cleanContent = rawContent.substring(matcher.end());
        } else if (comment.getParent() != null && comment.getParent().getAuthor() != null && comment.getAuthor() != null) {
            User parentAuthor = comment.getParent().getAuthor();
            if (!parentAuthor.getId().equals(comment.getAuthor().getId())) {
                replyTo = parentAuthor.getName();
                replyToAuthorId = parentAuthor.getId();
            }
        }

        var sentimentResult = TextAlgorithms.sentiment(cleanContent);

        return CommentResponse.builder()
                .id(comment.getId())
                .content(cleanContent)
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getName())
                .authorAvatar(comment.getAuthor().getAvatar())
                .replyTo(replyTo)
                .replyToAuthorId(replyToAuthorId)
                .upvotes(comment.getUpvotes())
                .downvotes(comment.getDownvotes())
                .createdAt(comment.getCreatedAt())
                .sentiment(sentimentResult.label())
                .sentimentScore(sentimentResult.score())
                .replies(replies)
                .build();
    }
}
