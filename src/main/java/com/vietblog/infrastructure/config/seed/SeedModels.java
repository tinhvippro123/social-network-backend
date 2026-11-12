package com.vietblog.infrastructure.config.seed;

import com.vietblog.domain.entity.Report;
import com.vietblog.domain.entity.User;

import java.util.List;

/**
 * Các mô hình dữ liệu bất biến phục vụ khởi tạo bộ dữ liệu trình diễn Đồ án Chuyên ngành.
 */
public final class SeedModels {

    private SeedModels() {}

    public record Img(String src, String alt) {}

    public static Img img(String src, String alt) {
        return new Img(src, alt);
    }

    /**
     * Sinh thẻ <p><img .../>...</p> chuẩn để proseGallery.ts tự động gom nhóm:
     * - 1 ảnh: Ảnh minh họa đơn lẻ toàn chiều ngang
     * - 2, 3, 4 ảnh: Lưới Collage tương ứng
     * - 5+ ảnh: Lưới Collage 2x2 kèm nhãn +N xem toàn màn hình qua Lightbox
     */
    public static String gallery(Img... images) {
        if (images == null || images.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder("<p>");
        for (Img image : images) {
            sb.append("<img src=\"")
              .append(escapeAttr(image.src()))
              .append("\" alt=\"")
              .append(escapeAttr(image.alt()))
              .append("\" />");
        }
        sb.append("</p>");
        return sb.toString();
    }

    private static String escapeAttr(String raw) {
        if (raw == null) return "";
        return raw.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public record UserSeed(
            String email,
            String name,
            String avatar,
            String bio,
            User.Role role,
            int joinedDaysAgo
    ) {}

    public record CommentSeed(
            String authorEmail,
            String content,
            int upvotes,
            int hoursAgo,
            List<CommentSeed> replies
    ) {
        public static CommentSeed of(String authorEmail, String content, int upvotes, int hoursAgo) {
            return new CommentSeed(authorEmail, content, upvotes, hoursAgo, List.of());
        }

        public static CommentSeed of(String authorEmail, String content, int upvotes, int hoursAgo, List<CommentSeed> replies) {
            return new CommentSeed(authorEmail, content, upvotes, hoursAgo, replies);
        }
    }

    public record PostSeed(
            String key,
            String title,
            String excerpt,
            String content,
            String coverImage,
            String categorySlug,
            String authorEmail,
            List<String> tags,
            Double latitude,
            Double longitude,
            String locationAddress,
            int viewsCount,
            int upvotesCount,
            int downvotesCount,
            int daysAgo,
            Integer eventStartsInDays,
            Integer eventDurationHours,
            List<CommentSeed> comments
    ) {}

    public record GroupCommentSeed(
            String authorEmail,
            String content,
            int hoursAgo
    ) {}

    public record GroupPostSeed(
            String authorEmail,
            String title,
            String excerpt,
            String content,
            String coverImage,
            int daysAgo,
            List<GroupCommentSeed> comments
    ) {}

    public record GroupSeed(
            String name,
            String description,
            String avatar,
            String coverImage,
            String ownerEmail,
            boolean publicGroup,
            int createdDaysAgo,
            List<String> memberEmails,
            List<GroupPostSeed> posts
    ) {}

    public record ChatMsgSeed(
            String senderEmail,
            String content,
            String type,
            String imageUrl,
            Integer replyToIndex,
            boolean pinned,
            int minutesAgo
    ) {
        public static ChatMsgSeed text(String senderEmail, String content, int minutesAgo) {
            return new ChatMsgSeed(senderEmail, content, "text", null, null, false, minutesAgo);
        }

        public static ChatMsgSeed pinnedText(String senderEmail, String content, int minutesAgo) {
            return new ChatMsgSeed(senderEmail, content, "text", null, null, true, minutesAgo);
        }

        public static ChatMsgSeed reply(String senderEmail, String content, int replyToIndex, int minutesAgo) {
            return new ChatMsgSeed(senderEmail, content, "text", null, replyToIndex, false, minutesAgo);
        }

        public static ChatMsgSeed image(String senderEmail, String caption, String imageUrl, int minutesAgo) {
            return new ChatMsgSeed(senderEmail, caption, "image", imageUrl, null, false, minutesAgo);
        }
    }

    public record GroupChatSeed(
            String name,
            String creatorEmail,
            List<String> memberEmails,
            boolean pinnedForPrimaryUsers,
            List<ChatMsgSeed> messages
    ) {}

    public record DirectChatSeed(
            String user1Email,
            String user2Email,
            String nicknameForUser2,
            boolean pinnedForUser1,
            int unreadCountForUser1,
            List<ChatMsgSeed> messages
    ) {}

    public record ReportSeed(
            Report.TargetType targetType,
            String targetRefKey,
            String reporterEmail,
            String reason,
            String description,
            Report.ReportStatus status,
            String resolutionAction,
            int hoursAgo
    ) {}
}

