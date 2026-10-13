package com.vietblog.application.dto.community;

import com.vietblog.domain.entity.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public final class CommunityDtos {
    private CommunityDtos() {}
    public record PublicUser(String id, String name, String avatar) {
        public static PublicUser from(User user) { return new PublicUser(user.getId(), user.getName(), user.getAvatar()); }
    }
    public record GroupRequest(@NotBlank @Size(max = 120) String name,
                               @Size(max = 3000) String description,
                               @Size(max = 255) String avatar, @Size(max = 255) String coverImage,
                               @JsonProperty("isPublic") Boolean isPublic) {}
    public record GroupResponse(String id, String name, String description, String avatar, String coverImage,
                                long membersCount, long postsCount, @JsonProperty("isJoined") boolean isJoined,
                                @JsonProperty("isPublic") boolean isPublic, LocalDateTime createdAt, PublicUser owner,
                                String memberRole) {}
    public record MemberResponse(PublicUser user, String role, LocalDateTime joinedAt) {}
    public record MemberRequest(@NotBlank String userId) {}
    public record RoleRequest(@NotNull GroupMember.Role role) {}
    public record GroupPostRequest(@NotBlank @Size(max = 255) String title,
                                   @NotBlank @Size(max = 100000) String content,
                                   @Size(max = 1000) String excerpt, @Size(max = 255) String coverImage) {}
    public record GroupPostResponse(String id, String groupId, String title, String content, String excerpt,
                                    String coverImage, PublicUser author, LocalDateTime createdAt, LocalDateTime updatedAt) {
        public static GroupPostResponse from(GroupPost post) {
            return new GroupPostResponse(post.getId(), post.getGroup().getId(), post.getTitle(), post.getContent(),
                    post.getExcerpt(), post.getCoverImage(), PublicUser.from(post.getAuthor()), post.getCreatedAt(), post.getUpdatedAt());
        }
    }
}
