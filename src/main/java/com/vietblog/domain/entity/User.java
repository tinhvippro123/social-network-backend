package com.vietblog.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    private String avatar;
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Builder.Default
    private boolean isActive = true;

    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();

    private LocalDateTime lastLoginAt;

    // Nullable trong DB để PostgreSQL ddl-auto: update an toàn trên dữ liệu cũ (null = đã xác thực)
    @Builder.Default
    private Boolean emailVerified = true;

    // Cài đặt quyền riêng tư (nullable-safe cho ddl-auto: update)
    private String profileVisibility;
    private String allowMessages;
    private Boolean showOnlineStatus;
    private Boolean showReadReceipts;

    // Cài đặt thông báo (nullable-safe cho ddl-auto: update)
    private Boolean emailNewFollower;
    private Boolean emailNewComment;
    private Boolean emailNewMessage;
    private Boolean notifyNewFollower;
    private Boolean notifyNewComment;
    private Boolean notifyNewMessage;
    private Boolean notifyTrending;

    public boolean isEmailVerifiedEffective() {
        return !Boolean.FALSE.equals(emailVerified);
    }

    public String effectiveProfileVisibility() {
        if ("followers".equalsIgnoreCase(profileVisibility)) return "followers";
        if ("private".equalsIgnoreCase(profileVisibility)) return "private";
        return "public";
    }

    public String effectiveAllowMessages() {
        if ("followers".equalsIgnoreCase(allowMessages)) return "followers";
        if ("nobody".equalsIgnoreCase(allowMessages)) return "nobody";
        return "everyone";
    }

    public boolean isShowOnlineStatusEffective() {
        return !Boolean.FALSE.equals(showOnlineStatus);
    }

    public boolean isShowReadReceiptsEffective() {
        return !Boolean.FALSE.equals(showReadReceipts);
    }

    public boolean isEmailNewFollowerEffective() {
        return !Boolean.FALSE.equals(emailNewFollower);
    }

    public boolean isEmailNewCommentEffective() {
        return !Boolean.FALSE.equals(emailNewComment);
    }

    public boolean isEmailNewMessageEffective() {
        return Boolean.TRUE.equals(emailNewMessage);
    }

    public boolean isNotifyNewFollowerEffective() {
        return !Boolean.FALSE.equals(notifyNewFollower);
    }

    public boolean isNotifyNewCommentEffective() {
        return !Boolean.FALSE.equals(notifyNewComment);
    }

    public boolean isNotifyNewMessageEffective() {
        return !Boolean.FALSE.equals(notifyNewMessage);
    }

    public boolean isNotifyTrendingEffective() {
        return Boolean.TRUE.equals(notifyTrending);
    }

    public boolean canProfileBeViewedBy(String viewerId, boolean viewerIsFollower) {
        if (viewerId != null && viewerId.equals(id)) {
            return true;
        }
        return switch (effectiveProfileVisibility()) {
            case "private" -> false;
            case "followers" -> viewerId != null && viewerIsFollower;
            default -> true;
        };
    }

    public boolean canReceiveMessageFrom(String senderId, boolean senderIsFollower) {
        if (senderId != null && senderId.equals(id)) {
            return true;
        }
        return switch (effectiveAllowMessages()) {
            case "nobody" -> false;
            case "followers" -> senderId != null && senderIsFollower;
            default -> true;
        };
    }

    // Đếm cache (cập nhật khi có thay đổi, tránh COUNT query)
    @Builder.Default
    private int postsCount = 0;
    @Builder.Default
    private int followersCount = 0;
    @Builder.Default
    private int followingCount = 0;

    @ManyToMany
    @JoinTable(
        name = "user_followers",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "follower_id")
    )
    @Builder.Default
    private Set<User> followers = new HashSet<>();

    @ManyToMany(mappedBy = "followers")
    @Builder.Default
    private Set<User> following = new HashSet<>();

    public enum Role {
        USER, MODERATOR, ADMIN
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
