package com.vietblog.application.service;

import com.vietblog.application.dto.community.CommunityDtos.*;
import com.vietblog.domain.entity.*;
import com.vietblog.domain.exception.*;
import com.vietblog.infrastructure.repository.*;
import com.vietblog.infrastructure.security.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor @Transactional
public class GroupService {
    private final GroupRepository groups;
    private final GroupMemberRepository members;
    private final GroupPostRepository posts;
    private final GroupPostLikeRepository postLikes;
    private final GroupPostCommentRepository postComments;
    private final UserRepository users;
    private final HtmlSanitizer htmlSanitizer;

    @Transactional(readOnly = true)
    public Page<GroupResponse> list(String userId, boolean joined, String query, int page, int limit) {
        return groups.search(userId, joined, query == null ? "" : query.trim(), page(page, limit)).map(g -> response(g, userId));
    }

    @Transactional(readOnly = true)
    public GroupResponse get(String id, String userId) { return response(group(id), userId); }

    public GroupResponse create(String userId, GroupRequest request) {
        User owner = user(userId);
        Group group = Group.builder().owner(owner).name(request.name().trim()).description(request.description())
                .avatar(request.avatar()).coverImage(request.coverImage()).publicGroup(!Boolean.FALSE.equals(request.isPublic())).build();
        groups.save(group);
        members.save(GroupMember.builder().group(group).user(owner).role(GroupMember.Role.OWNER).build());
        return response(group, userId);
    }

    public GroupResponse update(String id, String userId, GroupRequest request) {
        Group group = lock(id);
        requireManager(id, userId);
        group.setName(request.name().trim());
        group.setDescription(request.description());
        group.setAvatar(request.avatar());
        group.setCoverImage(request.coverImage());
        if (request.isPublic() != null) group.setPublicGroup(request.isPublic());
        return response(group, userId);
    }

    public void delete(String id, String userId) {
        Group group = lock(id);
        if (!group.getOwner().getId().equals(userId) && user(userId).getRole() != User.Role.ADMIN) forbidden();
        posts.deleteByGroupId(id);
        members.deleteByGroupId(id);
        groups.delete(group);
    }

    public GroupResponse join(String id, String userId) {
        Group group = lock(id);
        if (members.existsByGroupIdAndUserId(id, userId)) return response(group, userId);
        if (!group.isPublicGroup()) forbidden();
        members.save(GroupMember.builder().group(group).user(user(userId)).build());
        return response(group, userId);
    }

    public void addMember(String id, String actorId, String userId) {
        Group group = lock(id);
        requireManager(id, actorId);
        if (!members.existsByGroupIdAndUserId(id, userId)) {
            members.save(GroupMember.builder().group(group).user(user(userId)).build());
        }
    }

    public void removeMember(String id, String actorId, String userId) {
        lock(id);
        GroupMember target = member(id, userId);
        if (target.getRole() == GroupMember.Role.OWNER) invalid();
        if (!actorId.equals(userId)) {
            GroupMember actor = requireManager(id, actorId);
            if (target.getRole() == GroupMember.Role.ADMIN && actor.getRole() != GroupMember.Role.OWNER) forbidden();
        }
        members.delete(target);
    }

    public void setRole(String id, String actorId, String userId, GroupMember.Role role) {
        Group group = lock(id);
        if (!group.getOwner().getId().equals(actorId)) forbidden();
        GroupMember target = member(id, userId);
        if (target.getRole() == GroupMember.Role.OWNER || role == GroupMember.Role.OWNER) invalid();
        target.setRole(role);
    }

    @Transactional(readOnly = true)
    public Page<MemberResponse> members(String id, String userId, int page, int limit) {
        requireReadable(group(id), userId);
        page(page, limit);
        return members.findByGroupId(id, PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "joinedAt")))
                .map(m -> new MemberResponse(PublicUser.from(m.getUser()), m.getRole().name(), m.getJoinedAt()));
    }

    @Transactional(readOnly = true)
    public Page<GroupPostResponse> posts(String id, String userId, int page, int limit) {
        requireReadable(group(id), userId);
        return posts.findByGroupId(id, page(page, limit)).map(p -> postResponse(p, userId));
    }

    @Transactional(readOnly = true)
    public GroupPostResponse post(String id, String postId, String userId) {
        requireReadable(group(id), userId);
        return postResponse(findPost(id, postId), userId);
    }

    public GroupPostResponse createPost(String id, String userId, GroupPostRequest request) {
        Group group = lock(id);
        GroupMember author = member(id, userId);
        GroupPost post = GroupPost.builder().group(group).author(author.getUser()).title(request.title().trim())
                .content(htmlSanitizer.sanitize(request.content())).excerpt(request.excerpt()).coverImage(request.coverImage()).build();
        return postResponse(posts.save(post), userId);
    }

    public GroupPostResponse updatePost(String id, String postId, String userId, GroupPostRequest request) {
        lock(id);
        member(id, userId);
        GroupPost post = findPost(id, postId);
        if (!post.getAuthor().getId().equals(userId)) forbidden();
        post.setTitle(request.title().trim());
        post.setContent(htmlSanitizer.sanitize(request.content()));
        post.setExcerpt(request.excerpt());
        post.setCoverImage(request.coverImage());
        post.setUpdatedAt(LocalDateTime.now());
        return postResponse(post, userId);
    }

    public void deletePost(String id, String postId, String userId) {
        lock(id);
        GroupMember actor = member(id, userId);
        GroupPost post = findPost(id, postId);
        if (!post.getAuthor().getId().equals(userId) && actor.getRole() == GroupMember.Role.MEMBER) forbidden();
        postLikes.deleteByPostId(postId);
        postComments.deleteByPostId(postId);
        posts.delete(post);
    }

    public GroupLikeResponse togglePostLike(String id, String postId, String userId) {
        requireReadable(group(id), userId);
        GroupPost post = findPost(id, postId);
        var existing = postLikes.findByPostIdAndUserId(postId, userId);
        boolean isLiked;
        if (existing.isPresent()) {
            postLikes.delete(existing.get());
            isLiked = false;
        } else {
            postLikes.save(GroupPostLike.builder().post(post).user(user(userId)).build());
            isLiked = true;
        }
        return new GroupLikeResponse(isLiked, postLikes.countByPostId(postId));
    }

    @Transactional(readOnly = true)
    public java.util.List<GroupCommentResponse> getComments(String id, String postId, String userId) {
        requireReadable(group(id), userId);
        findPost(id, postId);
        return postComments.findByPostIdOrderByCreatedAtAsc(postId).stream().map(GroupCommentResponse::from).toList();
    }

    public GroupCommentResponse addComment(String id, String postId, String userId, GroupCommentRequest request) {
        requireReadable(group(id), userId);
        GroupPost post = findPost(id, postId);
        GroupPostComment comment = GroupPostComment.builder()
                .post(post)
                .author(user(userId))
                .content(htmlSanitizer.sanitize(request.content().trim()))
                .build();
        return GroupCommentResponse.from(postComments.save(comment));
    }

    public void deleteComment(String id, String postId, String commentId, String userId) {
        lock(id);
        findPost(id, postId);
        GroupPostComment comment = postComments.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COMMENT_NOT_FOUND));
        GroupMember actor = member(id, userId);
        if (!comment.getAuthor().getId().equals(userId) && actor.getRole() == GroupMember.Role.MEMBER) {
            forbidden();
        }
        postComments.delete(comment);
    }

    private GroupPostResponse postResponse(GroupPost post, String userId) {
        long likes = postLikes.countByPostId(post.getId());
        long comments = postComments.countByPostId(post.getId());
        boolean isLiked = userId != null && postLikes.existsByPostIdAndUserId(post.getId(), userId);
        return GroupPostResponse.from(post, likes, comments, isLiked);
    }

    private GroupPost findPost(String groupId, String postId) {
        return posts.findByIdAndGroupId(postId, groupId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND));
    }
    private GroupResponse response(Group group, String userId) {
        GroupMember membership = members.findByGroupIdAndUserId(group.getId(), userId).orElse(null);
        return new GroupResponse(group.getId(), group.getName(), group.getDescription(), group.getAvatar(), group.getCoverImage(),
                members.countByGroupId(group.getId()), posts.countByGroupId(group.getId()), membership != null, group.isPublicGroup(),
                group.getCreatedAt(), PublicUser.from(group.getOwner()), membership == null ? null : membership.getRole().name());
    }
    private Group group(String id) { return groups.findById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.GROUP_NOT_FOUND)); }
    private Group lock(String id) { return groups.findLockedById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.GROUP_NOT_FOUND)); }
    private GroupMember member(String id, String userId) {
        return members.findByGroupIdAndUserId(id, userId).orElseThrow(() -> new ForbiddenException(ErrorCode.FORBIDDEN));
    }
    private GroupMember requireManager(String id, String userId) {
        GroupMember membership = member(id, userId);
        if (membership.getRole() != GroupMember.Role.OWNER && membership.getRole() != GroupMember.Role.ADMIN) forbidden();
        return membership;
    }
    private void requireReadable(Group group, String userId) {
        if (!group.isPublicGroup() && !members.existsByGroupIdAndUserId(group.getId(), userId)) forbidden();
    }
    private User user(String id) {
        User user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        if (!user.isActive()) forbidden();
        return user;
    }
    public static PageRequest page(int page, int limit) {
        if (page < 1 || limit < 1 || limit > 100) invalid();
        return PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
    }
    private static void forbidden() { throw new ForbiddenException(ErrorCode.FORBIDDEN); }
    private static void invalid() { throw new BusinessRuleException(ErrorCode.INVALID_INPUT); }
}
