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

    // Äáº¿m cache (cáº­p nháº­t khi cÃ³ thay Ä‘á»•i, trÃ¡nh COUNT query)
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
}
