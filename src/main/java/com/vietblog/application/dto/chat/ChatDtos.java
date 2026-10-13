package com.vietblog.application.dto.chat;

import com.vietblog.application.dto.community.CommunityDtos.PublicUser;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public final class ChatDtos {
    private ChatDtos() {}
    public record ConversationRequest(@NotNull @Size(min = 1, max = 49) List<@NotBlank String> participantIds,
                                      @Size(max = 120) String name, @JsonProperty("isGroup") boolean isGroup) {}
    public record SendRequest(@NotNull @Size(max = 10000) String content,
                              @Pattern(regexp = "text|image|file") String type, String replyToId,
                              @Size(max = 255) String fileName, @Size(max = 255) String fileSize,
                              @Size(max = 2000) String fileUrl, @Size(max = 2000) String imageUrl) {}
    public record FlagRequest(boolean value) {}
    public record ConversationResponse(String id, String name, String avatar, String lastMessage,
                                        LocalDateTime lastMessageTime, long unreadCount,
                                        @JsonProperty("isGroup") boolean isGroup, @JsonProperty("isOnline") boolean isOnline,
                                        @JsonProperty("isPinned") boolean isPinned, @JsonProperty("isMuted") boolean isMuted,
                                        List<PublicUser> participants) {}
    public record ReplyResponse(String id, String content, String senderName) {}
    public record ReadReceipt(PublicUser user, LocalDateTime readAt) {}
    public record MessageResponse(String id, String conversationId, String content, PublicUser sender,
                                   LocalDateTime createdAt, @JsonProperty("isOwn") boolean isOwn,
                                   @JsonProperty("isRevoked") boolean isRevoked, String type, String status,
                                   List<ReadReceipt> readBy, ReplyResponse replyTo,
                                   String fileName, String fileSize, String fileUrl, String imageUrl,
                                   @JsonProperty("isPinned") boolean isPinned) {}
}
