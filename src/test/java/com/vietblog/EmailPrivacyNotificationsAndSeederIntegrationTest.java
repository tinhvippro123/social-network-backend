package com.vietblog;

import com.vietblog.application.service.ChatService;
import com.vietblog.application.service.TrendingRankingScheduler;
import com.vietblog.domain.entity.Category;
import com.vietblog.domain.entity.Notification;
import com.vietblog.domain.entity.Post;
import com.vietblog.domain.entity.User;
import com.vietblog.infrastructure.config.ExtendedDataSeeder;
import com.vietblog.infrastructure.repository.CategoryRepository;
import com.vietblog.infrastructure.repository.NotificationRepository;
import com.vietblog.infrastructure.repository.PostRepository;
import com.vietblog.infrastructure.repository.UserRepository;
import com.vietblog.support.CapturingJavaMailSender;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@Import(CapturingJavaMailSender.Config.class)
class EmailPrivacyNotificationsAndSeederIntegrationTest extends VietblogBackendApplicationTests {

    private static final String PASSWORD = "Password123!";

    @Autowired WebApplicationContext context;
    @Autowired CapturingJavaMailSender capturingMailSender;
    @Autowired UserRepository users;
    @Autowired PostRepository posts;
    @Autowired CategoryRepository categories;
    @Autowired NotificationRepository notifications;
    @Autowired ChatService chatService;
    @Autowired TrendingRankingScheduler trendingRankingScheduler;
    @Autowired ExtendedDataSeeder extendedDataSeeder;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;

    private MockMvc mvc;
    private User owner;
    private User visitor;
    private User admin;
    private Category category;
    private Post ownerPost;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        capturingMailSender.clear();

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        owner = users.saveAndFlush(User.builder()
                .name("Owner " + suffix)
                .email("owner-" + suffix + "@vietblog.test")
                .password(passwordEncoder.encode(PASSWORD))
                .bio("Owner secret bio")
                .role(User.Role.USER)
                .isActive(true)
                .emailVerified(true)
                .build());

        visitor = users.saveAndFlush(User.builder()
                .name("Visitor " + suffix)
                .email("visitor-" + suffix + "@vietblog.test")
                .password(passwordEncoder.encode(PASSWORD))
                .bio("Visitor bio")
                .role(User.Role.USER)
                .isActive(true)
                .emailVerified(true)
                .build());

        admin = users.saveAndFlush(User.builder()
                .name("Admin " + suffix)
                .email("admin-" + suffix + "@vietblog.test")
                .password(passwordEncoder.encode(PASSWORD))
                .role(User.Role.ADMIN)
                .isActive(true)
                .emailVerified(true)
                .build());

        category = categories.saveAndFlush(Category.builder()
                .name("Privacy-" + suffix)
                .slug("privacy-" + suffix)
                .description("Category for integration test")
                .build());

        ownerPost = posts.saveAndFlush(Post.builder()
                .title("Owner Published Post " + suffix)
                .content("Nội dung bài viết hợp lệ đủ dài để kiểm thử quyền riêng tư và thông báo trên VietBlog.".repeat(2))
                .excerpt("Tóm tắt bài viết")
                .author(owner)
                .category(category)
                .upvotesCount(10)
                .commentsCount(5)
                .viewsCount(50)
                .status(Post.PostStatus.PUBLISHED)
                .build());
    }

    private Cookie login(User user) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie("vietblog_access_token");
    }

    @Test
    void userPreferencesPersistAndProfileVisibilityIsEnforcedServerSide() throws Exception {
        Cookie ownerCookie = login(owner);
        Cookie visitorCookie = login(visitor);
        Cookie adminCookie = login(admin);

        // Default preferences loaded from DB
        mvc.perform(get("/api/users/me/preferences").cookie(ownerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileVisibility").value("public"))
                .andExpect(jsonPath("$.data.allowMessages").value("everyone"))
                .andExpect(jsonPath("$.data.showOnlineStatus").value(true))
                .andExpect(jsonPath("$.data.showReadReceipts").value(true))
                .andExpect(jsonPath("$.data.emailNewFollower").value(true))
                .andExpect(jsonPath("$.data.emailNewComment").value(true))
                .andExpect(jsonPath("$.data.emailNewMessage").value(false))
                .andExpect(jsonPath("$.data.pushNewFollower").value(true))
                .andExpect(jsonPath("$.data.pushNewComment").value(true))
                .andExpect(jsonPath("$.data.pushNewMessage").value(true))
                .andExpect(jsonPath("$.data.pushTrending").value(false));

        // Switch profile to PRIVATE
        mvc.perform(put("/api/users/me/preferences")
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileVisibility\":\"private\",\"allowMessages\":\"nobody\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileVisibility").value("private"))
                .andExpect(jsonPath("$.data.allowMessages").value("nobody"));

        // Re-fetching confirms PostgreSQL persistence and bio masking for visitor
        mvc.perform(get("/api/users/{id}", owner.getId()).cookie(visitorCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canViewProfile").value(false))
                .andExpect(jsonPath("$.data.canMessage").value(false))
                .andExpect(jsonPath("$.data.bio").doesNotExist());

        mvc.perform(get("/api/users/{id}/posts", owner.getId()).cookie(visitorCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PROFILE_PRIVATE"));
        mvc.perform(get("/api/users/{id}/followers", owner.getId()).cookie(visitorCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PROFILE_PRIVATE"));
        mvc.perform(get("/api/users/{id}/following", owner.getId()).cookie(visitorCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PROFILE_PRIVATE"));

        // Owner & Admin still see full profile
        mvc.perform(get("/api/users/{id}/posts", owner.getId()).cookie(ownerCookie)).andExpect(status().isOk());
        mvc.perform(get("/api/users/{id}/posts", owner.getId()).cookie(adminCookie)).andExpect(status().isOk());

        // Switch profile to FOLLOWERS-only
        mvc.perform(put("/api/users/me/preferences")
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileVisibility\":\"followers\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/users/{id}/posts", owner.getId()).cookie(visitorCookie))
                .andExpect(status().isForbidden());

        // After visitor follows owner, profile & posts unlock
        mvc.perform(post("/api/users/{id}/follow", owner.getId()).cookie(visitorCookie))
                .andExpect(status().isOk());

        mvc.perform(get("/api/users/{id}", owner.getId()).cookie(visitorCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canViewProfile").value(true))
                .andExpect(jsonPath("$.data.bio").value("Owner secret bio"));
        mvc.perform(get("/api/users/{id}/posts", owner.getId()).cookie(visitorCookie))
                .andExpect(status().isOk());
    }

    @Test
    void chatPrivacyOnlineStatusAndReadReceiptsAreEnforcedByBackend() throws Exception {
        Cookie ownerCookie = login(owner);
        Cookie visitorCookie = login(visitor);

        // 1. allowMessages = nobody blocks creating 1-1 chat
        mvc.perform(put("/api/users/me/preferences")
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"allowMessages\":\"nobody\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/chat/conversations")
                        .cookie(visitorCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participantIds\":[\"" + owner.getId() + "\"],\"isGroup\":false}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("MESSAGE_PRIVACY_RESTRICTED"));

        // 2. allowMessages = followers blocks non-follower, allows once followed
        mvc.perform(put("/api/users/me/preferences")
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"allowMessages\":\"followers\",\"showOnlineStatus\":false,\"showReadReceipts\":false,\"emailNewMessage\":true}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/chat/conversations")
                        .cookie(visitorCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participantIds\":[\"" + owner.getId() + "\"],\"isGroup\":false}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("MESSAGE_PRIVACY_RESTRICTED"));

        mvc.perform(post("/api/users/{id}/follow", owner.getId()).cookie(visitorCookie))
                .andExpect(status().isOk());

        MvcResult convRes = mvc.perform(post("/api/chat/conversations")
                        .cookie(visitorCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participantIds\":[\"" + owner.getId() + "\"],\"isGroup\":false}"))
                .andExpect(status().isOk())
                .andReturn();
        String convId = objectMapper.readTree(convRes.getResponse().getContentAsString()).path("data").path("id").asString();

        // 3. showOnlineStatus = false hides online state even when WS onlineChecker returns true
        chatService.setOnlineChecker(id -> true);
        try {
            mvc.perform(get("/api/chat/conversations").cookie(visitorCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].isOnline").value(false));
        } finally {
            chatService.setOnlineChecker(id -> false);
        }

        // 4. Visitor sends message -> Owner gets emailNewMessage asynchronously; Owner marks read with showReadReceipts = false
        capturingMailSender.clear();
        mvc.perform(post("/api/chat/conversations/{id}/messages", convId)
                        .cookie(visitorCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Xin chào bạn nhé!\",\"type\":\"text\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/chat/conversations/{id}/read", convId).cookie(ownerCookie))
                .andExpect(status().isOk());

        mvc.perform(get("/api/chat/conversations/{id}/messages", convId).cookie(visitorCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("sent"))
                .andExpect(jsonPath("$.data[0].readBy").isEmpty());

        awaitEmailFor(owner.getEmail(), 1);
        assertThat(capturingMailSender.forRecipient(owner.getEmail()))
                .anyMatch(mail -> mail.subject().contains("Tin nhắn mới"));
    }

    @Test
    void notificationPreferencesControlInAppAlertsEmailsAndTrendingAlerts() throws Exception {
        Cookie ownerCookie = login(owner);
        Cookie visitorCookie = login(visitor);

        // Disable in-app follow & comment alerts and email alerts first
        mvc.perform(put("/api/users/me/preferences")
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pushNewFollower\":false,\"pushNewComment\":false,\"emailNewFollower\":false,\"emailNewComment\":false,\"pushTrending\":true}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/users/{id}/follow", owner.getId()).cookie(visitorCookie))
                .andExpect(status().isOk());
        mvc.perform(post("/api/posts/{id}/comments", ownerPost.getId())
                        .cookie(visitorCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Bình luận thử nghiệm tắt thông báo\"}"))
                .andExpect(status().isCreated());

        List<Notification> suppressedNotifs = notifications.findByUserIdOrderByCreatedAtDesc(owner.getId());
        assertThat(suppressedNotifs).noneMatch(n ->
                n.getType() == Notification.NotificationType.FOLLOW || n.getType() == Notification.NotificationType.COMMENT);

        // Enable in-app & email alerts and unfollow/re-follow + comment
        mvc.perform(post("/api/users/{id}/follow", owner.getId()).cookie(visitorCookie))
                .andExpect(status().isOk());
        mvc.perform(put("/api/users/me/preferences")
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pushNewFollower\":true,\"pushNewComment\":true,\"emailNewFollower\":true,\"emailNewComment\":true}"))
                .andExpect(status().isOk());

        capturingMailSender.clear();
        mvc.perform(post("/api/users/{id}/follow", owner.getId()).cookie(visitorCookie))
                .andExpect(status().isOk());
        mvc.perform(post("/api/posts/{id}/comments", ownerPost.getId())
                        .cookie(visitorCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Bình luận thử nghiệm bật thông báo!\"}"))
                .andExpect(status().isCreated());

        List<Notification> enabledNotifs = notifications.findByUserIdOrderByCreatedAtDesc(owner.getId());
        assertThat(enabledNotifs).anyMatch(n -> n.getType() == Notification.NotificationType.FOLLOW);
        assertThat(enabledNotifs).anyMatch(n -> n.getType() == Notification.NotificationType.COMMENT);

        awaitEmailFor(owner.getEmail(), 2);

        // Trending scheduler sends deduplicated SYSTEM alert when pushTrending = true
        trendingRankingScheduler.recalculateTrendingScores();
        trendingRankingScheduler.recalculateTrendingScores();

        long trendingNotifCount = notifications.findByUserIdOrderByCreatedAtDesc(owner.getId())
                .stream()
                .filter(n -> n.getType() == Notification.NotificationType.SYSTEM
                        && ("/posts/" + ownerPost.getId()).equals(n.getLink()))
                .count();
        assertThat(trendingNotifCount).isEqualTo(1);
    }

    @Test
    void tokenPurposeIsolationAndExtendedDataSeederNonOverwriteWorkProperly() throws Exception {
        // 1. Token purpose isolation: password-reset OTP cannot verify an unverified email
        String unverifiedEmail = "isolate-" + UUID.randomUUID().toString().substring(0, 8) + "@vietblog.test";
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Isolate User\",\"email\":\"" + unverifiedEmail
                                + "\",\"password\":\"Password123!\",\"passwordConfirm\":\"Password123!\"}"))
                .andExpect(status().isOk());
        String verifyOtp = capturingMailSender.latestOtpFor(unverifiedEmail).orElseThrow();

        capturingMailSender.clear();
        mvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + unverifiedEmail + "\"}"))
                .andExpect(status().isOk());
        String resetOtp = capturingMailSender.latestOtpFor(unverifiedEmail).orElseThrow();

        // Using reset OTP against /verify-email must fail!
        mvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + unverifiedEmail + "\",\"otp\":\"" + resetOtp + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_VERIFICATION_TOKEN"));

        // Using verify OTP still works!
        mvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + unverifiedEmail + "\",\"otp\":\"" + verifyOtp + "\"}"))
                .andExpect(status().isOk());

        // 2. ExtendedDataSeeder must NOT overwrite existing user passwords or bio when seed version is current
        User seededAdmin = users.findByEmail("admin@vietblog.com").orElseThrow();
        String customEncodedPwd = passwordEncoder.encode("CustomDefensePwd99!");
        seededAdmin.setPassword(customEncodedPwd);
        seededAdmin.setBio("Custom preserved bio");
        users.saveAndFlush(seededAdmin);

        extendedDataSeeder.run();

        User afterRerun = users.findByEmail("admin@vietblog.com").orElseThrow();
        assertThat(passwordEncoder.matches("CustomDefensePwd99!", afterRerun.getPassword())).isTrue();
        assertThat(afterRerun.getBio()).isEqualTo("Custom preserved bio");
        assertThat(afterRerun.isEmailVerifiedEffective()).isTrue();
    }

    private void awaitEmailFor(String email, int expectedCount) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            if (capturingMailSender.forRecipient(email).size() >= expectedCount) {
                return;
            }
            Thread.sleep(50);
        }
        assertThat(capturingMailSender.forRecipient(email)).hasSizeGreaterThanOrEqualTo(expectedCount);
    }
}
