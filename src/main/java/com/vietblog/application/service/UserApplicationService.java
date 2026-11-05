package com.vietblog.application.service;

import com.vietblog.application.dto.post.PostResponse;
import com.vietblog.application.dto.user.UpdateProfileRequest;
import com.vietblog.application.dto.user.UserProfileResponse;
import com.vietblog.application.dto.user.UserSummaryResponse;
import com.vietblog.domain.entity.Notification;
import com.vietblog.domain.entity.Post;
import com.vietblog.domain.entity.User;
import com.vietblog.domain.exception.DomainException;
import com.vietblog.domain.exception.ErrorCode;
import com.vietblog.domain.exception.ResourceNotFoundException;
import com.vietblog.infrastructure.repository.BookmarkRepository;
import com.vietblog.infrastructure.repository.NotificationRepository;
import com.vietblog.infrastructure.repository.PostRepository;
import com.vietblog.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final NotificationRepository notificationRepository;
    private final BookmarkRepository bookmarkRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String targetUserId, String currentUserId) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        int postsCount = (int) postRepository.countByAuthorIdAndStatus(targetUserId, Post.PostStatus.PUBLISHED);
        boolean isMe = currentUserId != null && currentUserId.equals(targetUserId);
        boolean isFollowing = (!isMe && currentUserId != null) && userRepository.isFollowing(targetUserId, currentUserId);

        return UserProfileResponse.fromEntity(targetUser, isFollowing, isMe, postsCount);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(String currentUserId) {
        return getUserProfile(currentUserId, currentUserId);
    }

    @Transactional
    public UserProfileResponse updateProfile(String currentUserId, UpdateProfileRequest request) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        user.setName(request.getName().trim());
        if (request.getBio() != null) {
            user.setBio(request.getBio().trim());
        }
        if (request.getAvatar() != null && !request.getAvatar().isBlank()) {
            user.setAvatar(request.getAvatar().trim());
        }

        userRepository.save(user);
        int postsCount = (int) postRepository.countByAuthorIdAndStatus(currentUserId, Post.PostStatus.PUBLISHED);

        return UserProfileResponse.fromEntity(user, false, true, postsCount);
    }

    @Transactional
    public boolean toggleFollow(String targetUserId, String currentUserId) {
        if (targetUserId.equals(currentUserId)) {
            throw new DomainException(ErrorCode.CANNOT_FOLLOW_SELF);
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        boolean isAlreadyFollowing = userRepository.isFollowing(targetUserId, currentUserId);

        if (isAlreadyFollowing) {
            targetUser.getFollowers().removeIf(u -> u.getId().equals(currentUser.getId()));
            currentUser.getFollowing().removeIf(u -> u.getId().equals(targetUser.getId()));

            targetUser.setFollowersCount(Math.max(0, targetUser.getFollowersCount() - 1));
            currentUser.setFollowingCount(Math.max(0, currentUser.getFollowingCount() - 1));

            userRepository.save(targetUser);
            userRepository.save(currentUser);
            return false;
        } else {
            targetUser.getFollowers().add(currentUser);
            currentUser.getFollowing().add(targetUser);

            targetUser.setFollowersCount(targetUser.getFollowersCount() + 1);
            currentUser.setFollowingCount(currentUser.getFollowingCount() + 1);

            userRepository.save(targetUser);
            userRepository.save(currentUser);
            notificationRepository.save(Notification.builder()
                    .user(targetUser)
                    .type(Notification.NotificationType.FOLLOW)
                    .message(currentUser.getName() + " đã bắt đầu theo dõi bạn")
                    .avatar(currentUser.getAvatar())
                    .link("/profile/" + currentUser.getId())
                    .build());
            return true;
        }
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getUserFollowers(String targetUserId, String currentUserId) {
        if (!userRepository.existsById(targetUserId)) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
        }

        List<User> followers = userRepository.findFollowersByUserId(targetUserId);
        return followers.stream()
                .map(u -> {
                    boolean isFollowing = currentUserId != null && userRepository.isFollowing(u.getId(), currentUserId);
                    int postsCount = Math.max(u.getPostsCount(), (int) postRepository.countByAuthorId(u.getId()));
                    return UserSummaryResponse.fromEntity(u, isFollowing, postsCount);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getUserFollowing(String targetUserId, String currentUserId) {
        if (!userRepository.existsById(targetUserId)) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
        }

        List<User> following = userRepository.findFollowingByUserId(targetUserId);
        return following.stream()
                .map(u -> {
                    boolean isFollowing = currentUserId != null && userRepository.isFollowing(u.getId(), currentUserId);
                    int postsCount = Math.max(u.getPostsCount(), (int) postRepository.countByAuthorId(u.getId()));
                    return UserSummaryResponse.fromEntity(u, isFollowing, postsCount);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getRecommendedUsers(String currentUserId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<User> topUsers = userRepository.findTopUsers(pageable);

        return topUsers.stream()
                .filter(u -> currentUserId == null || !u.getId().equals(currentUserId))
                .map(u -> {
                    boolean isFollowing = currentUserId != null && userRepository.isFollowing(u.getId(), currentUserId);
                    int postsCount = Math.max(u.getPostsCount(), (int) postRepository.countByAuthorId(u.getId()));
                    return UserSummaryResponse.fromEntity(u, isFollowing, postsCount);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getUserPosts(String targetUserId, int page, int limit) {
        return getUserPosts(targetUserId, page, limit, "PUBLISHED", null);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getUserPosts(String targetUserId, int page, int limit, String status, String viewerId) {
        if (!userRepository.existsById(targetUserId)) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
        }

        Post.PostStatus postStatus = Post.PostStatus.PUBLISHED;
        if ("DRAFT".equalsIgnoreCase(status) && viewerId != null && viewerId.equals(targetUserId)) {
            postStatus = Post.PostStatus.DRAFT;
        }

        Pageable pageable = PageRequest.of(Math.max(0, page - 1), limit, Sort.by("createdAt").descending());
        Page<Post> postPage = postRepository.findByAuthorIdAndStatus(targetUserId, postStatus, pageable);
        int authorPostsCount = (int) postRepository.countByAuthorIdAndStatus(targetUserId, Post.PostStatus.PUBLISHED);

        return postPage.map(post -> {
            boolean isBookmarked = viewerId != null && bookmarkRepository.existsByUserIdAndPostId(viewerId, post.getId());
            return PostResponse.fromEntity(post, authorPostsCount, isBookmarked);
        });
    }
}
