package com.vietblog.application.service;

import com.vietblog.application.dto.post.PostResponse;
import com.vietblog.application.dto.user.UpdateProfileRequest;
import com.vietblog.application.dto.user.UserPreferencesDto;
import com.vietblog.application.dto.user.UserProfileResponse;
import com.vietblog.application.dto.user.UserSummaryResponse;
import com.vietblog.domain.entity.Notification;
import com.vietblog.domain.entity.Post;
import com.vietblog.domain.entity.User;
import com.vietblog.domain.exception.BusinessRuleException;
import com.vietblog.domain.exception.DomainException;
import com.vietblog.domain.exception.ErrorCode;
import com.vietblog.domain.exception.ResourceNotFoundException;
import com.vietblog.infrastructure.repository.BookmarkRepository;
import com.vietblog.infrastructure.repository.ChatBlockRepository;
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
    private final NotificationService notificationService;
    private final BookmarkRepository bookmarkRepository;
    private final ChatBlockRepository chatBlockRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String targetUserId, String currentUserId) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        int postsCount = (int) postRepository.countByAuthorIdAndStatus(targetUserId, Post.PostStatus.PUBLISHED);
        boolean isMe = currentUserId != null && currentUserId.equals(targetUserId);
        boolean isFollowing = (!isMe && currentUserId != null) && userRepository.isFollowing(targetUserId, currentUserId);
        boolean canViewProfile = canViewProfile(targetUser, currentUserId, isFollowing);
        boolean isBlocked = !isMe && currentUserId != null
                && (chatBlockRepository.existsByBlockerIdAndBlockedId(currentUserId, targetUserId)
                || chatBlockRepository.existsByBlockerIdAndBlockedId(targetUserId, currentUserId));
        boolean canMessage = !isMe && currentUserId != null && !isBlocked
                && targetUser.canReceiveMessageFrom(currentUserId, isFollowing);

        return UserProfileResponse.fromEntity(targetUser, isFollowing, isMe, postsCount, canViewProfile, canMessage);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(String currentUserId) {
        return getUserProfile(currentUserId, currentUserId);
    }

    @Transactional(readOnly = true)
    public UserPreferencesDto getPreferences(String currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        return UserPreferencesDto.fromEntity(user);
    }

    @Transactional
    public UserPreferencesDto updatePreferences(String currentUserId, UserPreferencesDto request) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        if (request.getProfileVisibility() != null) {
            user.setProfileVisibility(request.getProfileVisibility());
        }
        if (request.getAllowMessages() != null) {
            user.setAllowMessages(request.getAllowMessages());
        }
        if (request.getShowOnlineStatus() != null) {
            user.setShowOnlineStatus(request.getShowOnlineStatus());
        }
        if (request.getShowReadReceipts() != null) {
            user.setShowReadReceipts(request.getShowReadReceipts());
        }
        if (request.getEmailNewFollower() != null) {
            user.setEmailNewFollower(request.getEmailNewFollower());
        }
        if (request.getEmailNewComment() != null) {
            user.setEmailNewComment(request.getEmailNewComment());
        }
        if (request.getEmailNewMessage() != null) {
            user.setEmailNewMessage(request.getEmailNewMessage());
        }
        if (request.getPushNewFollower() != null) {
            user.setNotifyNewFollower(request.getPushNewFollower());
        }
        if (request.getPushNewComment() != null) {
            user.setNotifyNewComment(request.getPushNewComment());
        }
        if (request.getPushNewMessage() != null) {
            user.setNotifyNewMessage(request.getPushNewMessage());
        }
        if (request.getPushTrending() != null) {
            user.setNotifyTrending(request.getPushTrending());
        }

        userRepository.save(user);
        return UserPreferencesDto.fromEntity(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(String currentUserId, UpdateProfileRequest request) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        String trimmedName = request.getName() == null ? "" : request.getName().trim();
        if (trimmedName.length() < 2) {
            throw new BusinessRuleException(ErrorCode.INVALID_INPUT, "Tên hiển thị phải từ 2 đến 50 ký tự");
        }
        user.setName(trimmedName);
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
            notificationService.notifyWithPreferences(
                    targetUser,
                    Notification.NotificationType.FOLLOW,
                    currentUser.getName() + " đã bắt đầu theo dõi bạn",
                    currentUser.getAvatar(),
                    "/profile/" + currentUser.getId(),
                    "[VietBlog] " + currentUser.getName() + " đã bắt đầu theo dõi bạn",
                    "Người theo dõi mới"
            );
            return true;
        }
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getUserFollowers(String targetUserId, String currentUserId) {
        requireProfileVisible(targetUserId, currentUserId);

        List<User> followers = userRepository.findFollowersByUserId(targetUserId);
        return followers.stream()
                .map(u -> {
                    boolean isFollowing = currentUserId != null && userRepository.isFollowing(u.getId(), currentUserId);
                    int postsCount = Math.max(u.getPostsCount(),
                            (int) postRepository.countByAuthorIdAndStatus(u.getId(), Post.PostStatus.PUBLISHED));
                    return UserSummaryResponse.fromEntity(u, isFollowing, postsCount);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getUserFollowing(String targetUserId, String currentUserId) {
        requireProfileVisible(targetUserId, currentUserId);

        List<User> following = userRepository.findFollowingByUserId(targetUserId);
        return following.stream()
                .map(u -> {
                    boolean isFollowing = currentUserId != null && userRepository.isFollowing(u.getId(), currentUserId);
                    int postsCount = Math.max(u.getPostsCount(),
                            (int) postRepository.countByAuthorIdAndStatus(u.getId(), Post.PostStatus.PUBLISHED));
                    return UserSummaryResponse.fromEntity(u, isFollowing, postsCount);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getRecommendedUsers(String currentUserId, int limit) {
        if (limit < 1 || limit > 100) {
            throw new BusinessRuleException(ErrorCode.INVALID_INPUT);
        }
        Pageable pageable = PageRequest.of(0, limit + 1);
        List<User> topUsers = userRepository.findTopUsers(pageable);

        return topUsers.stream()
                .filter(u -> currentUserId == null || !u.getId().equals(currentUserId))
                .limit(limit)
                .map(u -> {
                    boolean isFollowing = currentUserId != null && userRepository.isFollowing(u.getId(), currentUserId);
                    int postsCount = Math.max(u.getPostsCount(),
                            (int) postRepository.countByAuthorIdAndStatus(u.getId(), Post.PostStatus.PUBLISHED));
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
        if (page < 1 || limit < 1 || limit > 100) {
            throw new BusinessRuleException(ErrorCode.INVALID_INPUT);
        }
        requireProfileVisible(targetUserId, viewerId);

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
        Post.PostStatus postStatus = Post.PostStatus.PUBLISHED;
        if ("DRAFT".equalsIgnoreCase(status)) {
            if (viewerId != null && viewerId.equals(targetUserId)) {
                postStatus = Post.PostStatus.DRAFT;
            } else {
                return Page.empty(pageable);
            }
        }

        Page<Post> postPage = postRepository.findByAuthorIdAndStatus(targetUserId, postStatus, pageable);
        int authorPostsCount = (int) postRepository.countByAuthorIdAndStatus(targetUserId, Post.PostStatus.PUBLISHED);

        return postPage.map(post -> {
            boolean isBookmarked = viewerId != null && bookmarkRepository.existsByUserIdAndPostId(viewerId, post.getId());
            return PostResponse.fromEntity(post, authorPostsCount, isBookmarked);
        });
    }

    private void requireProfileVisible(String targetUserId, String viewerId) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        boolean isMe = viewerId != null && viewerId.equals(targetUserId);
        boolean isFollowing = !isMe && viewerId != null && userRepository.isFollowing(targetUserId, viewerId);
        if (!canViewProfile(targetUser, viewerId, isFollowing)) {
            throw new BusinessRuleException(ErrorCode.PROFILE_PRIVATE);
        }
    }

    private boolean canViewProfile(User targetUser, String viewerId, boolean isFollowing) {
        if (targetUser.canProfileBeViewedBy(viewerId, isFollowing)) {
            return true;
        }
        return viewerId != null && userRepository.findById(viewerId)
                .map(viewer -> viewer.getRole() == User.Role.ADMIN)
                .orElse(false);
    }
}
