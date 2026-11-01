package com.vietblog.infrastructure.config;

import com.vietblog.domain.entity.*;
import com.vietblog.domain.service.PostDomainService;
import com.vietblog.infrastructure.config.seed.*;
import com.vietblog.infrastructure.config.seed.SeedModels.*;
import com.vietblog.infrastructure.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Điều phối khởi tạo và làm mới bộ dữ liệu trình diễn toàn diện cho nền tảng VietBlog.
 * Sử dụng khóa phiên bản trong system_settings để chỉ làm sạch dữ liệu cũ một lần duy nhất,
 * giữ nguyên mọi thao tác thử nghiệm tiếp theo của người dùng.
 */
@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class ExtendedDataSeeder implements CommandLineRunner {

    public static final String SEED_VERSION_KEY = "seed.dataset.version";
    public static final String CURRENT_SEED_VERSION = "defense-2026-v2";

    private static final List<String> PRESERVED_ADMIN_EMAILS = List.of(
            "lethanhtinh2004@gmail.com",
            "1250080199@sv.hcmunre.edu.vn"
    );

    private static final String DEFAULT_AVATAR =
            "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=400&q=80";

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final BookmarkRepository bookmarkRepository;
    private final NotificationRepository notificationRepository;
    private final ReportRepository reportRepository;
    private final PostImageFeaturesRepository postImageFeaturesRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupPostRepository groupPostRepository;
    private final GroupPostCommentRepository groupPostCommentRepository;
    private final GroupPostLikeRepository groupPostLikeRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatBlockRepository chatBlockRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final PostDomainService postDomainService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        String existingVersion = systemSettingRepository.findById(SEED_VERSION_KEY)
                .map(SystemSetting::getValue)
                .orElse("");

        boolean needsRefresh = !CURRENT_SEED_VERSION.equals(existingVersion) || postRepository.count() == 0;

        if (!needsRefresh) {
            ensureAndUpsertUsers(false);
            syncAllCounters();
            log.info("Dataset version '{}' already active. Preserving existing platform content.", CURRENT_SEED_VERSION);
            return;
        }

        log.info("Refreshing platform content to dataset version '{}'...", CURRENT_SEED_VERSION);

        cleanAllContentTables();
        Map<String, User> usersByEmail = ensureAndUpsertUsers(true);
        seedFollowNetwork(usersByEmail);
        SeededPostsContext postsCtx = seedPostsCommentsReactionsAndBookmarks(usersByEmail);
        seedCommunityGroups(usersByEmail);
        seedChatConversations(usersByEmail);
        seedModerationReports(usersByEmail, postsCtx);
        syncAllCounters();

        systemSettingRepository.save(new SystemSetting(SEED_VERSION_KEY, CURRENT_SEED_VERSION));

        log.info("Dataset '{}' ready! Users: {}, Posts: {}, Comments: {}, Groups: {}, Conversations: {}, Reports: {}",
                CURRENT_SEED_VERSION,
                userRepository.count(),
                postRepository.count(),
                commentRepository.count(),
                groupRepository.count(),
                conversationRepository.count(),
                reportRepository.count());
    }

    private Map<String, User> ensureAndUpsertUsers(boolean refreshDates) {
        String encodedDefaultPwd = passwordEncoder.encode("Password123!");
        LocalDateTime now = LocalDateTime.now();

        for (UserSeed seed : CommunityAndSocialCatalog.users()) {
            User user = userRepository.findByEmail(seed.email()).orElseGet(User::new);
            user.setEmail(seed.email());
            user.setName(seed.name());
            user.setPassword(encodedDefaultPwd);
            user.setAvatar(seed.avatar());
            user.setBio(seed.bio());
            user.setRole(seed.role());
            user.setActive(true);
            if (refreshDates || user.getJoinedAt() == null) {
                LocalDateTime joined = seed.joinedDaysAgo() == 0
                        ? now.minusHours(2)
                        : now.minusDays(seed.joinedDaysAgo()).minusHours(3);
                user.setJoinedAt(joined);
                user.setLastLoginAt(now.minusMinutes(15L + seed.joinedDaysAgo() * 7L));
            }
            userRepository.save(user);
        }

        for (String studentEmail : PRESERVED_ADMIN_EMAILS) {
            userRepository.findByEmail(studentEmail).ifPresent(student -> {
                student.setRole(User.Role.ADMIN);
                student.setActive(true);
                if (student.getAvatar() == null || student.getAvatar().isBlank()) {
                    student.setAvatar("https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=400&q=80");
                }
                if (student.getBio() == null || student.getBio().isBlank()) {
                    student.setBio("Kỹ sư phát triển hệ thống VietBlog — Khoa Công nghệ Thông tin, Trường Đại học Tài nguyên và Môi trường TP.HCM (HCMUNRE).");
                }
                userRepository.save(student);
            });
        }

        Map<String, User> byEmail = new LinkedHashMap<>();
        for (User u : userRepository.findAll()) {
            if (u.getAvatar() == null || u.getAvatar().isBlank()) {
                u.setAvatar(DEFAULT_AVATAR);
                userRepository.save(u);
            }
            byEmail.put(u.getEmail(), u);
        }
        userRepository.flush();
        return byEmail;
    }

    private void cleanAllContentTables() {
        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            u.getFollowers().clear();
            u.getFollowing().clear();
        }
        userRepository.saveAll(allUsers);
        userRepository.flush();

        chatBlockRepository.deleteAllInBatch();

        List<ChatMessage> existingMessages = chatMessageRepository.findAll();
        if (!existingMessages.isEmpty()) {
            existingMessages.forEach(m -> m.setReplyTo(null));
            chatMessageRepository.saveAll(existingMessages);
            chatMessageRepository.flush();
            chatMessageRepository.deleteAllInBatch();
        }

        conversationMemberRepository.deleteAllInBatch();
        conversationRepository.deleteAllInBatch();

        groupPostLikeRepository.deleteAllInBatch();
        groupPostCommentRepository.deleteAllInBatch();
        groupPostRepository.deleteAllInBatch();
        groupMemberRepository.deleteAllInBatch();
        groupRepository.deleteAllInBatch();

        notificationRepository.deleteAllInBatch();
        reportRepository.deleteAllInBatch();
        reactionRepository.deleteAllInBatch();
        bookmarkRepository.deleteAllInBatch();
        postImageFeaturesRepository.deleteAllInBatch();

        List<Comment> existingComments = commentRepository.findAll();
        if (!existingComments.isEmpty()) {
            existingComments.forEach(c -> c.setParent(null));
            commentRepository.saveAll(existingComments);
            commentRepository.flush();
            commentRepository.deleteAllInBatch();
        }

        postRepository.deleteAll();
        postRepository.flush();

        passwordResetTokenRepository.deleteAllInBatch();

        Set<String> allowedEmails = new HashSet<>(PRESERVED_ADMIN_EMAILS);
        CommunityAndSocialCatalog.users().forEach(s -> allowedEmails.add(s.email()));
        List<User> obsoleteUsers = userRepository.findAll().stream()
                .filter(u -> !allowedEmails.contains(u.getEmail()))
                .toList();
        if (!obsoleteUsers.isEmpty()) {
            userRepository.deleteAll(obsoleteUsers);
            userRepository.flush();
            log.info("Removed {} obsolete test user accounts during dataset refresh.", obsoleteUsers.size());
        }
    }

    private void seedFollowNetwork(Map<String, User> usersByEmail) {
        List<String> primaryTargets = new ArrayList<>(List.of(
                "nguyenvanan@gmail.com",
                "admin@vietblog.com",
                "phamminhduc@gmail.com",
                "lehongnhung@gmail.com",
                "dangthimylinh@gmail.com"
        ));
        for (String preserved : PRESERVED_ADMIN_EMAILS) {
            if (usersByEmail.containsKey(preserved)) {
                primaryTargets.add(preserved);
            }
        }

        List<User> allCurated = CommunityAndSocialCatalog.users().stream()
                .map(s -> usersByEmail.get(s.email()))
                .filter(Objects::nonNull)
                .toList();

        for (String targetEmail : primaryTargets) {
            User target = usersByEmail.get(targetEmail);
            if (target == null) continue;
            for (int i = 0; i < Math.min(8, allCurated.size()); i++) {
                User follower = allCurated.get(i);
                if (!follower.getId().equals(target.getId())) {
                    target.getFollowers().add(follower);
                }
            }
            userRepository.save(target);
        }

        User an = usersByEmail.get("nguyenvanan@gmail.com");
        User duc = usersByEmail.get("phamminhduc@gmail.com");
        User linh = usersByEmail.get("dangthimylinh@gmail.com");
        if (an != null && duc != null) {
            notificationRepository.save(Notification.builder()
                    .user(an)
                    .type(Notification.NotificationType.FOLLOW)
                    .message(duc.getName() + " đã bắt đầu theo dõi bạn")
                    .avatar(duc.getAvatar())
                    .link("/profile/" + duc.getId())
                    .isRead(true)
                    .createdAt(LocalDateTime.now().minusHours(20))
                    .build());
        }
        if (an != null && linh != null) {
            notificationRepository.save(Notification.builder()
                    .user(an)
                    .type(Notification.NotificationType.FOLLOW)
                    .message(linh.getName() + " đã bắt đầu theo dõi bạn")
                    .avatar(linh.getAvatar())
                    .link("/profile/" + linh.getId())
                    .isRead(false)
                    .createdAt(LocalDateTime.now().minusHours(3))
                    .build());
        }
        userRepository.flush();
    }

    private record SeededPostsContext(
            Map<String, Post> postsByKey,
            Map<String, Comment> targetCommentByPostKey
    ) {}

    private SeededPostsContext seedPostsCommentsReactionsAndBookmarks(Map<String, User> usersByEmail) {
        Map<String, Category> categoriesBySlug = new HashMap<>();
        categoryRepository.findAll().forEach(c -> categoriesBySlug.put(c.getSlug(), c));

        Category defaultCat = categoriesBySlug.values().stream().findFirst().orElseThrow();
        User fallbackAuthor = usersByEmail.getOrDefault("admin@vietblog.com",
                usersByEmail.values().iterator().next());

        List<PostSeed> allPostSeeds = new ArrayList<>();
        allPostSeeds.addAll(TechAndEventPostsCatalog.posts());
        allPostSeeds.addAll(LifestyleTravelFoodPostsCatalog.posts());

        Map<String, Post> postsByKey = new LinkedHashMap<>();
        Map<String, Comment> targetCommentByPostKey = new LinkedHashMap<>();

        LocalDateTime now = LocalDateTime.now();
        List<String> emojis = List.of("👍", "❤️", "😮", "😂");
        List<User> reactors = CommunityAndSocialCatalog.users().stream()
                .map(s -> usersByEmail.get(s.email()))
                .filter(Objects::nonNull)
                .toList();

        Set<String> notifyRecipients = new HashSet<>(List.of(
                "nguyenvanan@gmail.com",
                "admin@vietblog.com"
        ));
        notifyRecipients.addAll(PRESERVED_ADMIN_EMAILS);

        int postIdx = 0;
        for (PostSeed seed : allPostSeeds) {
            Category category = categoriesBySlug.getOrDefault(seed.categorySlug(), defaultCat);
            User author = usersByEmail.getOrDefault(seed.authorEmail(), fallbackAuthor);

            LocalDateTime createdAt = seed.daysAgo() == 0
                    ? now.minusHours(3L + (postIdx % 3))
                    : now.minusDays(seed.daysAgo()).minusHours(postIdx % 8);

            LocalDateTime eventStart = seed.eventStartsInDays() != null
                    ? now.plusDays(seed.eventStartsInDays()).withHour(8).withMinute(30).withSecond(0).withNano(0)
                    : null;
            LocalDateTime eventEnd = (eventStart != null && seed.eventDurationHours() != null)
                    ? eventStart.plusHours(seed.eventDurationHours())
                    : null;

            Post post = Post.builder()
                    .title(seed.title())
                    .excerpt(seed.excerpt())
                    .content(seed.content())
                    .coverImage(seed.coverImage())
                    .category(category)
                    .author(author)
                    .tags(new ArrayList<>(seed.tags()))
                    .status(Post.PostStatus.PUBLISHED)
                    .latitude(seed.latitude())
                    .longitude(seed.longitude())
                    .locationAddress(seed.locationAddress())
                    .eventStartTime(eventStart)
                    .eventEndTime(eventEnd)
                    .viewsCount(seed.viewsCount())
                    .upvotesCount(seed.upvotesCount())
                    .downvotesCount(seed.downvotesCount())
                    .createdAt(createdAt)
                    .updatedAt(createdAt)
                    .build();

            post = postRepository.save(post);
            postsByKey.put(seed.key(), post);

            // Seed multi-level comments (Level 1 -> Level 2 -> Level 3)
            for (CommentSeed cSeed : seed.comments()) {
                Comment lastSaved = saveCommentNode(post, null, cSeed, usersByEmail, fallbackAuthor, notifyRecipients);
                targetCommentByPostKey.put(seed.key(), lastSaved);
            }

            // Seed natural emoji reactions across users
            int reactionCount = Math.min(reactors.size(), 5 + (postIdx % 5));
            for (int r = 0; r < reactionCount; r++) {
                User reactor = reactors.get((postIdx + r) % reactors.size());
                if (!reactor.getId().equals(author.getId())) {
                    String emoji = emojis.get((postIdx + r) % emojis.size());
                    reactionRepository.save(Reaction.builder()
                            .post(post)
                            .user(reactor)
                            .emoji(emoji)
                            .createdAt(createdAt.plusMinutes(20L + r * 9L))
                            .build());
                }
            }

            // Create upvote notification if authored by a primary demo user
            if (notifyRecipients.contains(author.getEmail()) && !reactors.isEmpty()) {
                List<User> fanCandidates = reactors.stream()
                        .filter(u -> !u.getId().equals(author.getId()))
                        .toList();
                User fan = fanCandidates.isEmpty() ? null : fanCandidates.get(postIdx % fanCandidates.size());
                if (fan != null) {
                    notificationRepository.save(Notification.builder()
                            .user(author)
                            .type(Notification.NotificationType.UPVOTE)
                            .message(fan.getName() + " đã bày tỏ cảm xúc về bài viết của bạn")
                            .avatar(fan.getAvatar())
                            .link("/posts/" + post.getId())
                            .isRead(postIdx % 2 == 1)
                            .createdAt(now.minusHours(4L + postIdx))
                            .build());
                }
            }

            postIdx++;
        }

        // Seed curated bookmarks for primary users
        List<String> bookmarkedKeys = List.of(
                "clean-architecture-spring-boot",
                "vietnamese-nlp-textrank-sentiment",
                "san-may-cau-dat-da-lat",
                "ban-do-ca-phe-specialty-saigon",
                "hcmunre-it-career-techday-2026"
        );
        for (String email : notifyRecipients) {
            User saver = usersByEmail.get(email);
            if (saver == null) continue;
            int offset = 1;
            for (String key : bookmarkedKeys) {
                Post targetPost = postsByKey.get(key);
                if (targetPost != null) {
                    bookmarkRepository.save(Bookmark.builder()
                            .user(saver)
                            .post(targetPost)
                            .createdAt(now.minusHours(offset * 5L))
                            .build());
                    offset++;
                }
            }
        }

        // Ensure preserved student accounts also receive clean deep-linked notifications
        Post techDayPost = postsByKey.get("hcmunre-it-career-techday-2026");
        Comment techDayComment = targetCommentByPostKey.get("hcmunre-it-career-techday-2026");
        User tuan = usersByEmail.get("vuquoctuan@gmail.com");
        for (String studentEmail : PRESERVED_ADMIN_EMAILS) {
            User student = usersByEmail.get(studentEmail);
            if (student != null && techDayPost != null && techDayComment != null && tuan != null) {
                notificationRepository.save(Notification.builder()
                        .user(student)
                        .type(Notification.NotificationType.COMMENT)
                        .message(tuan.getName() + " đã bình luận về sự kiện " + techDayPost.getTitle())
                        .avatar(tuan.getAvatar())
                        .link("/posts/" + techDayPost.getId() + "#comment-" + techDayComment.getId())
                        .isRead(false)
                        .createdAt(now.minusMinutes(35))
                        .build());
            }
        }

        postRepository.flush();
        commentRepository.flush();
        return new SeededPostsContext(postsByKey, targetCommentByPostKey);
    }

    private Comment saveCommentNode(
            Post post,
            Comment parent,
            CommentSeed seed,
            Map<String, User> usersByEmail,
            User fallbackAuthor,
            Set<String> notifyRecipients
    ) {
        User commenter = usersByEmail.getOrDefault(seed.authorEmail(), fallbackAuthor);
        LocalDateTime commentTime = LocalDateTime.now().minusHours(Math.max(1, seed.hoursAgo()));

        Comment saved = commentRepository.save(Comment.builder()
                .post(post)
                .parent(parent)
                .author(commenter)
                .content(seed.content())
                .upvotes(seed.upvotes())
                .downvotes(0)
                .createdAt(commentTime)
                .build());

        // Notify post author when someone else comments on their post
        User postAuthor = post.getAuthor();
        if (notifyRecipients.contains(postAuthor.getEmail()) && !postAuthor.getId().equals(commenter.getId())) {
            String msg = (parent == null)
                    ? commenter.getName() + " đã bình luận về bài viết của bạn"
                    : commenter.getName() + " đã trả lời một thảo luận trong bài viết của bạn";
            notificationRepository.save(Notification.builder()
                    .user(postAuthor)
                    .type(Notification.NotificationType.COMMENT)
                    .message(msg)
                    .avatar(commenter.getAvatar())
                    .link("/posts/" + post.getId() + "#comment-" + saved.getId())
                    .isRead(seed.hoursAgo() > 72)
                    .createdAt(commentTime)
                    .build());
        }

        // Notify parent comment author when someone replies directly to their comment
        if (parent != null) {
            User parentAuthor = parent.getAuthor();
            if (notifyRecipients.contains(parentAuthor.getEmail())
                    && !parentAuthor.getId().equals(commenter.getId())
                    && !parentAuthor.getId().equals(postAuthor.getId())) {
                notificationRepository.save(Notification.builder()
                        .user(parentAuthor)
                        .type(Notification.NotificationType.COMMENT)
                        .message(commenter.getName() + " đã trả lời bình luận của bạn")
                        .avatar(commenter.getAvatar())
                        .link("/posts/" + post.getId() + "#comment-" + saved.getId())
                        .isRead(false)
                        .createdAt(commentTime)
                        .build());
            }
        }

        Comment deepest = saved;
        if (seed.replies() != null) {
            for (CommentSeed replySeed : seed.replies()) {
                deepest = saveCommentNode(post, saved, replySeed, usersByEmail, fallbackAuthor, notifyRecipients);
            }
        }
        return deepest;
    }

    private void seedCommunityGroups(Map<String, User> usersByEmail) {
        User fallbackOwner = usersByEmail.getOrDefault("admin@vietblog.com",
                usersByEmail.values().iterator().next());
        LocalDateTime now = LocalDateTime.now();

        for (GroupSeed gSeed : CommunityAndSocialCatalog.groups()) {
            User owner = usersByEmail.getOrDefault(gSeed.ownerEmail(), fallbackOwner);
            Group group = groupRepository.save(Group.builder()
                    .name(gSeed.name())
                    .description(gSeed.description())
                    .avatar(gSeed.avatar())
                    .coverImage(gSeed.coverImage())
                    .publicGroup(gSeed.publicGroup())
                    .owner(owner)
                    .createdAt(now.minusDays(gSeed.createdDaysAgo()))
                    .build());

            Set<String> addedUserIds = new HashSet<>();
            groupMemberRepository.save(GroupMember.builder()
                    .group(group)
                    .user(owner)
                    .role(GroupMember.Role.OWNER)
                    .joinedAt(now.minusDays(gSeed.createdDaysAgo()))
                    .build());
            addedUserIds.add(owner.getId());

            List<String> memberEmails = new ArrayList<>(gSeed.memberEmails());
            memberEmails.addAll(PRESERVED_ADMIN_EMAILS);

            for (String mEmail : memberEmails) {
                User member = usersByEmail.get(mEmail);
                if (member != null && addedUserIds.add(member.getId())) {
                    GroupMember.Role mRole = member.getRole() == User.Role.MODERATOR
                            ? GroupMember.Role.MODERATOR
                            : GroupMember.Role.MEMBER;
                    groupMemberRepository.save(GroupMember.builder()
                            .group(group)
                            .user(member)
                            .role(mRole)
                            .joinedAt(now.minusDays(Math.max(1, gSeed.createdDaysAgo() - 2)))
                            .build());
                }
            }

            for (GroupPostSeed gpSeed : gSeed.posts()) {
                User gpAuthor = usersByEmail.getOrDefault(gpSeed.authorEmail(), owner);
                LocalDateTime gpTime = now.minusDays(gpSeed.daysAgo()).minusHours(2);

                GroupPost gp = groupPostRepository.save(GroupPost.builder()
                        .group(group)
                        .author(gpAuthor)
                        .title(gpSeed.title())
                        .excerpt(gpSeed.excerpt())
                        .content(gpSeed.content())
                        .coverImage(gpSeed.coverImage())
                        .createdAt(gpTime)
                        .updatedAt(gpTime)
                        .build());

                // Add likes from group members
                int likesAdded = 0;
                for (String mEmail : memberEmails) {
                    User liker = usersByEmail.get(mEmail);
                    if (liker != null && likesAdded < 6) {
                        groupPostLikeRepository.save(GroupPostLike.builder()
                                .post(gp)
                                .user(liker)
                                .createdAt(gpTime.plusMinutes(15L + likesAdded * 7L))
                                .build());
                        likesAdded++;
                    }
                }

                for (GroupCommentSeed gcSeed : gpSeed.comments()) {
                    User gcAuthor = usersByEmail.getOrDefault(gcSeed.authorEmail(), owner);
                    groupPostCommentRepository.save(GroupPostComment.builder()
                            .post(gp)
                            .author(gcAuthor)
                            .content(gcSeed.content())
                            .createdAt(now.minusHours(gcSeed.hoursAgo()))
                            .build());
                }
            }
        }
        groupRepository.flush();
    }

    private void seedChatConversations(Map<String, User> usersByEmail) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);

        // 1. Group Chats
        for (GroupChatSeed gcSeed : CommunityAndSocialCatalog.groupChats()) {
            User creator = usersByEmail.get(gcSeed.creatorEmail());
            if (creator == null) continue;

            Conversation conv = conversationRepository.save(Conversation.builder()
                    .name(gcSeed.name())
                    .groupChat(true)
                    .creator(creator)
                    .createdAt(now.minusDays(10))
                    .updatedAt(now)
                    .build());

            List<String> participantEmails = new ArrayList<>(gcSeed.memberEmails());
            participantEmails.addAll(PRESERVED_ADMIN_EMAILS);

            Set<String> addedIds = new HashSet<>();
            List<ConversationMember> savedMembers = new ArrayList<>();
            for (String email : participantEmails) {
                User member = usersByEmail.get(email);
                if (member != null && addedIds.add(member.getId())) {
                    boolean shouldPin = gcSeed.pinnedForPrimaryUsers() && (
                            "nguyenvanan@gmail.com".equals(email)
                            || "admin@vietblog.com".equals(email)
                            || PRESERVED_ADMIN_EMAILS.contains(email)
                    );
                    savedMembers.add(conversationMemberRepository.save(ConversationMember.builder()
                            .conversation(conv)
                            .user(member)
                            .pinned(shouldPin)
                            .lastReadAt(now)
                            .build()));
                }
            }

            List<ChatMessage> savedMsgs = saveMessagesForConversation(conv, gcSeed.messages(), usersByEmail, now);
            if (!savedMsgs.isEmpty()) {
                LocalDateTime latestMsgTime = savedMsgs.get(savedMsgs.size() - 1).getCreatedAt();
                conv.setUpdatedAt(latestMsgTime);
                conversationRepository.save(conv);
                for (ConversationMember cm : savedMembers) {
                    cm.setLastReadAt(latestMsgTime);
                }
                conversationMemberRepository.saveAll(savedMembers);
            }
        }

        // 2. Direct 1-on-1 Chats
        for (DirectChatSeed dcSeed : CommunityAndSocialCatalog.directChats()) {
            User u1 = usersByEmail.get(dcSeed.user1Email());
            User u2 = usersByEmail.get(dcSeed.user2Email());
            if (u1 == null || u2 == null || u1.getId().equals(u2.getId())) continue;

            Set<String> sortedIds = new TreeSet<>(List.of(u1.getId(), u2.getId()));
            String directKey = String.join(":", sortedIds);
            if (conversationRepository.findByDirectKey(directKey).isPresent()) continue;

            Conversation conv = conversationRepository.save(Conversation.builder()
                    .name(u2.getName())
                    .groupChat(false)
                    .directKey(directKey)
                    .creator(u1)
                    .createdAt(now.minusDays(6))
                    .updatedAt(now)
                    .build());

            List<ChatMessage> savedMsgs = saveMessagesForConversation(conv, dcSeed.messages(), usersByEmail, now);
            LocalDateTime latestTime = savedMsgs.isEmpty() ? now : savedMsgs.get(savedMsgs.size() - 1).getCreatedAt();
            conv.setUpdatedAt(latestTime);
            conversationRepository.save(conv);

            LocalDateTime u1LastRead = latestTime;
            if (dcSeed.unreadCountForUser1() > 0 && savedMsgs.size() > dcSeed.unreadCountForUser1()) {
                int readUpToIdx = savedMsgs.size() - 1 - dcSeed.unreadCountForUser1();
                u1LastRead = savedMsgs.get(readUpToIdx).getCreatedAt();
            }

            conversationMemberRepository.save(ConversationMember.builder()
                    .conversation(conv)
                    .user(u1)
                    .pinned(dcSeed.pinnedForUser1())
                    .lastReadAt(u1LastRead)
                    .build());

            conversationMemberRepository.save(ConversationMember.builder()
                    .conversation(conv)
                    .user(u2)
                    .nickname(dcSeed.nicknameForUser2())
                    .lastReadAt(latestTime)
                    .build());
        }

        // 3. Direct chat for preserved student accounts if present
        User an = usersByEmail.get("nguyenvanan@gmail.com");
        for (String studentEmail : PRESERVED_ADMIN_EMAILS) {
            User student = usersByEmail.get(studentEmail);
            if (student != null && an != null && !student.getId().equals(an.getId())) {
                Set<String> sortedIds = new TreeSet<>(List.of(student.getId(), an.getId()));
                String directKey = String.join(":", sortedIds);
                if (conversationRepository.findByDirectKey(directKey).isEmpty()) {
                    Conversation conv = conversationRepository.save(Conversation.builder()
                            .name(an.getName())
                            .groupChat(false)
                            .directKey(directKey)
                            .creator(an)
                            .createdAt(now.minusDays(4))
                            .updatedAt(now.minusMinutes(25))
                            .build());
                    List<ChatMsgSeed> msgs = List.of(
                            ChatMsgSeed.text("nguyenvanan@gmail.com",
                                    "Chào bạn, phần bản đồ địa điểm Leaflet và tính năng tóm tắt bài viết bằng TextRank trên hệ thống chạy mượt và trực quan lắm!",
                                    85),
                            ChatMsgSeed.text(studentEmail,
                                    "Cảm ơn An nhiều nhé! Mình cũng vừa kiểm tra lại luồng thông báo bình luận và trình xem ảnh toàn màn hình trên các bài Du lịch, mọi thứ đều hoạt động ổn định rồi.",
                                    25)
                    );
                    List<ChatMessage> saved = saveMessagesForConversation(conv, msgs, usersByEmail, now);
                    LocalDateTime lastT = saved.get(saved.size() - 1).getCreatedAt();
                    conv.setUpdatedAt(lastT);
                    conversationRepository.save(conv);

                    conversationMemberRepository.save(ConversationMember.builder()
                            .conversation(conv).user(student).pinned(true).lastReadAt(lastT).build());
                    conversationMemberRepository.save(ConversationMember.builder()
                            .conversation(conv).user(an).lastReadAt(lastT).build());
                }
            }
        }

        conversationRepository.flush();
    }

    private List<ChatMessage> saveMessagesForConversation(
            Conversation conv,
            List<ChatMsgSeed> msgSeeds,
            Map<String, User> usersByEmail,
            LocalDateTime now
    ) {
        List<ChatMessage> savedList = new ArrayList<>();
        for (ChatMsgSeed mSeed : msgSeeds) {
            User sender = usersByEmail.get(mSeed.senderEmail());
            if (sender == null) continue;

            ChatMessage replyTarget = null;
            if (mSeed.replyToIndex() != null
                    && mSeed.replyToIndex() >= 0
                    && mSeed.replyToIndex() < savedList.size()) {
                replyTarget = savedList.get(mSeed.replyToIndex());
            }

            LocalDateTime msgTime = now.minusMinutes(mSeed.minutesAgo()).truncatedTo(ChronoUnit.MICROS);
            ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                    .conversation(conv)
                    .sender(sender)
                    .content(mSeed.content())
                    .type(mSeed.type())
                    .imageUrl(mSeed.imageUrl())
                    .replyTo(replyTarget)
                    .pinned(mSeed.pinned())
                    .createdAt(msgTime)
                    .build());
            savedList.add(saved);
        }
        return savedList;
    }

    private void seedModerationReports(Map<String, User> usersByEmail, SeededPostsContext postsCtx) {
        User admin = usersByEmail.get("admin@vietblog.com");
        LocalDateTime now = LocalDateTime.now();

        for (ReportSeed rSeed : CommunityAndSocialCatalog.reports()) {
            User reporter = usersByEmail.get(rSeed.reporterEmail());
            if (reporter == null) continue;

            String targetId = null;
            String targetTitle = null;

            if (rSeed.targetType() == Report.TargetType.POST) {
                Post targetPost = postsCtx.postsByKey().get(rSeed.targetRefKey());
                if (targetPost != null) {
                    targetId = targetPost.getId();
                    targetTitle = targetPost.getTitle();
                }
            } else if (rSeed.targetType() == Report.TargetType.COMMENT) {
                Comment targetComment = postsCtx.targetCommentByPostKey().get(rSeed.targetRefKey());
                Post parentPost = postsCtx.postsByKey().get(rSeed.targetRefKey());
                if (targetComment != null) {
                    targetId = targetComment.getId();
                    targetTitle = parentPost != null
                            ? "Bình luận tại bài \"" + parentPost.getTitle() + "\""
                            : "Bình luận của " + targetComment.getAuthor().getName();
                }
            }

            if (targetId == null) continue;

            LocalDateTime createdAt = now.minusHours(rSeed.hoursAgo());
            boolean isHandled = rSeed.status() != Report.ReportStatus.PENDING;

            reportRepository.save(Report.builder()
                    .targetId(targetId)
                    .type(rSeed.targetType())
                    .targetTitle(targetTitle)
                    .reason(rSeed.reason())
                    .description(rSeed.description())
                    .status(rSeed.status())
                    .reporter(reporter)
                    .createdAt(createdAt)
                    .resolutionAction(rSeed.resolutionAction())
                    .resolvedAt(isHandled ? createdAt.plusHours(3) : null)
                    .resolvedBy(isHandled ? admin : null)
                    .build());
        }
        reportRepository.flush();
    }

    private void syncAllCounters() {
        for (Post post : postRepository.findAll()) {
            int commentCount = Math.toIntExact(commentRepository.countByPostId(post.getId()));
            post.setCommentsCount(commentCount);
            postDomainService.calculateTrendingScore(post);
            postRepository.save(post);
        }

        for (User user : userRepository.findAll()) {
            int postsCount = Math.toIntExact(postRepository.countByAuthorId(user.getId()));
            int followersCount = userRepository.findFollowersByUserId(user.getId()).size();
            int followingCount = userRepository.findFollowingByUserId(user.getId()).size();
            user.setPostsCount(postsCount);
            user.setFollowersCount(followersCount);
            user.setFollowingCount(followingCount);
            userRepository.save(user);
        }

        postRepository.flush();
        userRepository.flush();
    }
}
