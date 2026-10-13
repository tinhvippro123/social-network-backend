package com.vietblog.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "group_members", uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "user_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GroupMember {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "group_id", nullable = false)
    private Group group;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false) @Builder.Default
    private Role role = Role.MEMBER;
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();
    public enum Role { OWNER, ADMIN, MODERATOR, MEMBER }
}
