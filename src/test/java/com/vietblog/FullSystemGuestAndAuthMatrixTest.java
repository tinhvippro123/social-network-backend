package com.vietblog;

import com.vietblog.domain.entity.*;
import com.vietblog.infrastructure.repository.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class FullSystemGuestAndAuthMatrixTest extends VietblogBackendApplicationTests {

    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired PostRepository posts;
    @Autowired CategoryRepository categories;
    @Autowired CommentRepository comments;
    @Autowired GroupRepository groups;
    @Autowired GroupMemberRepository groupMembers;
    @Autowired GroupPostRepository groupPosts;
    @Autowired NotificationRepository notifications;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;

    private MockMvc mvc;
    private User adminUser;
    private User modUser;
    private User authorUser;
    private User peerUser;
    private Category category;
    private Post publishedPost;
    private Post draftPost;
    private Post hiddenPost;
    private Comment publishedComment;
    private Group publicGroup;
    private Group privateGroup;
    private GroupPost publicGroupPost;
    private GroupPost privateGroupPost;

    private static final String PASSWORD = "Password123!";

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        adminUser = saveUser("Admin Matrix", "admin-" + suffix + "@vietblog.test", User.Role.ADMIN);
        modUser = saveUser("Mod Matrix", "mod-" + suffix + "@vietblog.test", User.Role.MODERATOR);
        authorUser = saveUser("Author Matrix", "author-" + suffix + "@vietblog.test", User.Role.USER);
        peerUser = saveUser("Peer Matrix", "peer-" + suffix + "@vietblog.test", User.Role.USER);

        category = categories.saveAndFlush(Category.builder()
                .name("Tech-" + suffix)
                .slug("tech-" + suffix)
                .description("Công nghệ phần mềm")
                .build());

        String longContent = "Kiến trúc Clean Architecture kết hợp Spring Boot 3, PostGIS và pgvector giúp tối ưu hiệu năng truy vấn dữ liệu lớn. ".repeat(3);

        publishedPost = posts.saveAndFlush(Post.builder()
                .title("Bài viết công khai " + suffix)
                .content(longContent)
                .excerpt("Tóm tắt bài viết công khai")
                .author(authorUser)
                .category(category)
                .tags(new ArrayList<>(List.of("springboot", "cleanarch")))
                .latitude(22.3364)
                .longitude(103.8438)
                .locationAddress("Sa Pa, Lào Cai")
                .status(Post.PostStatus.PUBLISHED)
                .build());

        draftPost = posts.saveAndFlush(Post.builder()
                .title("Bản nháp riêng tư " + suffix)
                .content(longContent)
                .author(authorUser)
                .category(category)
                .latitude(22.3364)
                .longitude(103.8438)
                .status(Post.PostStatus.DRAFT)
                .build());

        hiddenPost = posts.saveAndFlush(Post.builder()
                .title("Bài viết bị ẩn " + suffix)
                .content(longContent)
                .author(authorUser)
                .category(category)
                .latitude(22.3364)
                .longitude(103.8438)
                .status(Post.PostStatus.HIDDEN)
                .build());

        publishedComment = comments.saveAndFlush(Comment.builder()
                .post(publishedPost)
                .author(peerUser)
                .content("Bài viết rất hay và trình bày cực kỳ dễ hiểu!")
                .build());

        publicGroup = groups.saveAndFlush(Group.builder()
                .owner(authorUser)
                .name("Nhóm Công Khai " + suffix)
                .description("Cộng đồng mở")
                .publicGroup(true)
                .build());
        groupMembers.saveAndFlush(GroupMember.builder()
                .group(publicGroup).user(authorUser).role(GroupMember.Role.OWNER).build());
        publicGroupPost = groupPosts.saveAndFlush(GroupPost.builder()
                .group(publicGroup).author(authorUser).title("Thảo luận mở").content("Nội dung thảo luận công khai").build());

        privateGroup = groups.saveAndFlush(Group.builder()
                .owner(authorUser)
                .name("Nhóm Riêng Tư " + suffix)
                .description("Chỉ thành viên")
                .publicGroup(false)
                .build());
        groupMembers.saveAndFlush(GroupMember.builder()
                .group(privateGroup).user(authorUser).role(GroupMember.Role.OWNER).build());
        privateGroupPost = groupPosts.saveAndFlush(GroupPost.builder()
                .group(privateGroup).author(authorUser).title("Thảo luận kín").content("Nội dung mật").build());
    }

    private User saveUser(String name, String email, User.Role role) {
        return users.saveAndFlush(User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .role(role)
                .isActive(true)
                .build());
    }

    private Cookie loginAccessCookie(User user) throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andReturn();
        Cookie access = login.getResponse().getCookie("vietblog_access_token");
        assertThat(access).isNotNull();
        assertThat(access.isHttpOnly()).isTrue();
        return access;
    }

    private static byte[] samplePngBytes() throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    private static byte[] sampleDocxBytes() throws Exception {
        String xml = "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                + "<w:body><w:p><w:r><w:t>Spring Boot Clean Architecture PostGIS pgvector</w:t></w:r></w:p></w:body></w:document>";
        var out = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(xml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    @Test
    void guestCanBrowseAllPublicAndAiEndpointsWithoutCookiesAndWithoutPrivateDataLeaks() throws Exception {
        // Posts, Trending, Votes/Reactions/Bookmarks safe Guest reads, Comments
        mvc.perform(get("/api/posts")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        mvc.perform(get("/api/posts/trending")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        mvc.perform(get("/api/posts/{id}", publishedPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(publishedPost.getId()));
        mvc.perform(get("/api/posts/{id}/vote", publishedPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
        mvc.perform(get("/api/posts/{id}/reactions/summary", publishedPost.getId()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/posts/{id}/reactions/me", publishedPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
        mvc.perform(get("/api/posts/{id}/bookmarks/check", publishedPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));
        mvc.perform(get("/api/posts/{id}/comments", publishedPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(publishedComment.getId()));

        // Unpublished DRAFT and HIDDEN posts are strictly hidden from Guest (404)
        mvc.perform(get("/api/posts/{id}", draftPost.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/posts/{id}", hiddenPost.getId())).andExpect(status().isNotFound());

        // Categories & Tags
        mvc.perform(get("/api/categories")).andExpect(status().isOk());
        mvc.perform(get("/api/categories/{id}", category.getSlug())).andExpect(status().isOk());
        mvc.perform(get("/api/tags")).andExpect(status().isOk());

        // All 6 AI / Search / Geo / NLP read endpoints work for Guest
        mvc.perform(get("/api/search").param("q", "Spring Boot")).andExpect(status().isOk());
        mvc.perform(get("/api/search/semantic").param("q", "tối ưu hiệu năng truy vấn dữ liệu")).andExpect(status().isOk());
        mvc.perform(multipart("/api/search/image")
                        .file(new MockMultipartFile("image", "sample.png", "image/png", samplePngBytes())))
                .andExpect(status().isOk());
        mvc.perform(multipart("/api/search/document")
                        .file(new MockMultipartFile("file", "proposal.docx",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", sampleDocxBytes())))
                .andExpect(status().isOk());
        mvc.perform(get("/api/posts/nearby")
                        .param("lat", "22.3364").param("lng", "103.8438").param("radius", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem(publishedPost.getId())))
                .andExpect(jsonPath("$.data[*].id", not(hasItem(draftPost.getId()))))
                .andExpect(jsonPath("$.data[*].id", not(hasItem(hiddenPost.getId()))));
        mvc.perform(get("/api/posts/{id}/summary", publishedPost.getId()).param("sentences", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary").isNotEmpty());
        mvc.perform(get("/api/posts/{postId}/comments/{commentId}/sentiment",
                        publishedPost.getId(), publishedComment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.label").value("positive"));

        // Public User Profiles & Privacy (email masked, drafts hidden)
        mvc.perform(get("/api/users/recommended")).andExpect(status().isOk());
        mvc.perform(get("/api/users/{id}", authorUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").doesNotExist());
        mvc.perform(get("/api/users/{id}/followers", authorUser.getId())).andExpect(status().isOk());
        mvc.perform(get("/api/users/{id}/following", authorUser.getId())).andExpect(status().isOk());
        mvc.perform(get("/api/users/{id}/posts", authorUser.getId())).andExpect(status().isOk());
        mvc.perform(get("/api/users/{id}/posts", authorUser.getId()).param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        // Public Groups browsing
        mvc.perform(get("/api/groups")).andExpect(status().isOk());
        mvc.perform(get("/api/groups").param("joined", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(get("/api/groups/{id}", publicGroup.getId())).andExpect(status().isOk());
        mvc.perform(get("/api/groups/{id}/members", publicGroup.getId())).andExpect(status().isOk());
        mvc.perform(get("/api/groups/{id}/posts", publicGroup.getId())).andExpect(status().isOk());
        mvc.perform(get("/api/groups/{id}/posts/{postId}", publicGroup.getId(), publicGroupPost.getId()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/groups/{id}/posts/{postId}/comments", publicGroup.getId(), publicGroupPost.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void guestIsStrictlyRejectedWith401OnAllProtectedEndpointsAndPrivateGroupContent() throws Exception {
        // Auth protected self endpoints (even with empty invalid JSON body!)
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/auth/me").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/auth/me/password").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        // Users protected
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/users/{id}/follow", authorUser.getId())).andExpect(status().isUnauthorized());

        // Posts, Votes, Reactions, Bookmarks, Comments writes
        mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/posts/{id}", publishedPost.getId()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/posts/{id}", publishedPost.getId())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/posts/{id}/upvote", publishedPost.getId())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/posts/{id}/downvote", publishedPost.getId())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/posts/{id}/reactions", publishedPost.getId())
                .contentType(MediaType.APPLICATION_JSON).content("{\"emoji\":\"👍\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/posts/{id}/bookmarks", publishedPost.getId())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/bookmarks")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/posts/{id}/comments", publishedPost.getId())
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Guest\"}")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/posts/{id}/comments/{cid}", publishedPost.getId(), publishedComment.getId()))
                .andExpect(status().isUnauthorized());

        // Categories writes & Search/Moderation protected
        mvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/categories/{id}", category.getId()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/categories/{id}", category.getId())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/moderation/sentiment")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/nlp/sentiment").contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"test\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/posts/{id}/search-image", publishedPost.getId())
                        .file(new MockMultipartFile("image", "s.png", "image/png", samplePngBytes())))
                .andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/moderation/posts/{id}/status", publishedPost.getId())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());

        // Groups writes & Private Group reads
        mvc.perform(post("/api/groups").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/groups/{id}/join", publicGroup.getId())).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/groups/{id}/members/me", publicGroup.getId())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/groups/{id}/posts", publicGroup.getId())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/groups/{id}/posts/{pid}/like", publicGroup.getId(), publicGroupPost.getId()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/groups/{id}/posts/{pid}/comments", publicGroup.getId(), publicGroupPost.getId())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/groups/{id}/members", privateGroup.getId())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/groups/{id}/posts", privateGroup.getId())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/groups/{id}/posts/{pid}", privateGroup.getId(), privateGroupPost.getId()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/groups/{id}/posts/{pid}/comments", privateGroup.getId(), privateGroupPost.getId()))
                .andExpect(status().isUnauthorized());

        // Chat, Notifications, Reports, Files, Admin
        mvc.perform(get("/api/chat/conversations")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/chat/conversations").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/chat/blocks")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/notifications/unread-count")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/reports").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", "s.png", "image/png", samplePngBytes())))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/reports")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/stats")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/posts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/settings")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/admin/tags/springboot").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"java\"}"))
                .andExpect(status().isUnauthorized());

        // Wrong HTTP method returns 405 (never 500!)
        mvc.perform(get("/api/auth/login")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void authenticatedUserLifecycleWorksViaHttpOnlyCookieAndEnforcesStrict403RoleBoundaries() throws Exception {
        // 1. Register + Forgot/Reset Password + Login via HttpOnly Cookie
        String newEmail = "newuser-" + UUID.randomUUID().toString().substring(0, 8) + "@vietblog.test";
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Thành Viên Mới\",\"email\":\"" + newEmail
                                + "\",\"password\":\"Password123!\",\"passwordConfirm\":\"Password123!\"}"))
                .andExpect(status().isOk());

        MvcResult forgotRes = mvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + newEmail + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String otp = objectMapper.readTree(forgotRes.getResponse().getContentAsString()).path("data").path("otp").asString();
        assertThat(otp).hasSize(6);

        mvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + otp + "\",\"newPassword\":\"NewPass123!\"}"))
                .andExpect(status().isOk());

        MvcResult loginNew = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + newEmail + "\",\"password\":\"NewPass123!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie newUserCookie = loginNew.getResponse().getCookie("vietblog_access_token");
        assertThat(newUserCookie).isNotNull();

        mvc.perform(put("/api/auth/me/password")
                        .cookie(newUserCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"NewPass123!\",\"newPassword\":\"Password123!\"}"))
                .andExpect(status().isOk());

        // 2. Profile update & Follow
        Cookie peerCookie = loginAccessCookie(peerUser);
        Cookie authorCookie = loginAccessCookie(authorUser);

        mvc.perform(put("/api/users/me")
                        .cookie(peerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Peer Updated\",\"bio\":\"Yêu lập trình Java\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Peer Updated"));

        mvc.perform(post("/api/users/{id}/follow", authorUser.getId()).cookie(peerCookie))
                .andExpect(status().isOk());

        // 3. Draft -> Publish lifecycle & Author cannot self-set HIDDEN
        String validBodyContent = "Nội dung bài viết chi tiết đủ trên năm mươi ký tự để vượt qua kiểm tra hợp lệ của Domain Service.";
        mvc.perform(post("/api/posts")
                        .cookie(peerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Thử set HIDDEN\",\"content\":\"" + validBodyContent
                                + "\",\"categoryId\":\"" + category.getId() + "\",\"status\":\"HIDDEN\"}"))
                .andExpect(status().isBadRequest());

        MvcResult createDraft = mvc.perform(post("/api/posts")
                        .cookie(peerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bài nháp của Peer\",\"content\":\"" + validBodyContent
                                + "\",\"categoryId\":\"" + category.getId() + "\",\"status\":\"DRAFT\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String peerDraftId = objectMapper.readTree(createDraft.getResponse().getContentAsString()).path("data").path("id").asString();

        mvc.perform(get("/api/posts/{id}", peerDraftId).cookie(peerCookie)).andExpect(status().isOk());
        mvc.perform(get("/api/posts/{id}", peerDraftId).cookie(authorCookie)).andExpect(status().isNotFound());

        mvc.perform(put("/api/posts/{id}", peerDraftId)
                        .cookie(peerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bài chính thức của Peer\",\"content\":\"" + validBodyContent
                                + "\",\"categoryId\":\"" + category.getId() + "\",\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        // 4. Vote, Reaction, Bookmark, Comment & Post-Owner Comment Deletion
        mvc.perform(post("/api/posts/{id}/upvote", publishedPost.getId()).cookie(peerCookie)).andExpect(status().isOk());
        mvc.perform(post("/api/posts/{id}/reactions", publishedPost.getId())
                        .cookie(peerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emoji\":\"❤️\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/posts/{id}/bookmarks", publishedPost.getId()).cookie(peerCookie)).andExpect(status().isOk());
        mvc.perform(get("/api/bookmarks").cookie(peerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(publishedPost.getId()));

        MvcResult newCmt = mvc.perform(post("/api/posts/{id}/comments", publishedPost.getId())
                        .cookie(peerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Bình luận thử nghiệm từ Peer\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String cmtId = objectMapper.readTree(newCmt.getResponse().getContentAsString()).path("data").path("id").asString();

        // Post author can delete someone else's comment on their own post
        mvc.perform(delete("/api/posts/{id}/comments/{cid}", publishedPost.getId(), cmtId).cookie(authorCookie))
                .andExpect(status().isOk());

        // 5. Group join, post, like, comment, leave
        mvc.perform(post("/api/groups/{id}/join", publicGroup.getId()).cookie(peerCookie)).andExpect(status().isOk());
        mvc.perform(post("/api/groups/{id}/posts/{pid}/like", publicGroup.getId(), publicGroupPost.getId()).cookie(peerCookie))
                .andExpect(status().isOk());
        mvc.perform(post("/api/groups/{id}/posts/{pid}/comments", publicGroup.getId(), publicGroupPost.getId())
                        .cookie(peerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Thảo luận nhóm tuyệt vời\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/groups/{id}/members/me", publicGroup.getId()).cookie(peerCookie)).andExpect(status().isOk());

        // 6. Chat 1:1 lifecycle (create, send, read, pin, mute, nickname, pin message, revoke, block/unblock)
        MvcResult convRes = mvc.perform(post("/api/chat/conversations")
                        .cookie(peerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participantIds\":[\"" + authorUser.getId() + "\"],\"isGroup\":false}"))
                .andExpect(status().isOk())
                .andReturn();
        String convId = objectMapper.readTree(convRes.getResponse().getContentAsString()).path("data").path("id").asString();

        MvcResult msgRes = mvc.perform(post("/api/chat/conversations/{id}/messages", convId)
                        .cookie(peerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Xin chào tác giả!\",\"type\":\"text\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String msgId = objectMapper.readTree(msgRes.getResponse().getContentAsString()).path("data").path("id").asString();

        mvc.perform(post("/api/chat/conversations/{id}/read", convId).cookie(authorCookie)).andExpect(status().isOk());
        mvc.perform(patch("/api/chat/conversations/{id}/pin", convId)
                .cookie(peerCookie).contentType(MediaType.APPLICATION_JSON).content("{\"value\":true}")).andExpect(status().isOk());
        mvc.perform(patch("/api/chat/conversations/{id}/mute", convId)
                .cookie(peerCookie).contentType(MediaType.APPLICATION_JSON).content("{\"value\":true}")).andExpect(status().isOk());
        mvc.perform(patch("/api/chat/conversations/{id}/nickname", convId)
                        .cookie(peerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":\"" + authorUser.getId() + "\",\"nickname\":\"Bạn thân\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/chat/conversations/{id}/messages/{mid}/pin", convId, msgId)
                .cookie(peerCookie).contentType(MediaType.APPLICATION_JSON).content("{\"value\":true}")).andExpect(status().isOk());
        mvc.perform(delete("/api/chat/conversations/{id}/messages/{mid}", convId, msgId).cookie(peerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isRevoked").value(true));

        mvc.perform(post("/api/chat/blocks/{uid}", authorUser.getId()).cookie(peerCookie)).andExpect(status().isOk());
        mvc.perform(post("/api/chat/conversations/{id}/messages", convId)
                        .cookie(authorCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Tin nhắn khi bị chặn\",\"type\":\"text\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/chat/blocks/{uid}", authorUser.getId()).cookie(peerCookie)).andExpect(status().isOk());

        // 7. Notifications & Report submission
        Notification notif = notifications.saveAndFlush(Notification.builder()
                .user(peerUser).type(Notification.NotificationType.SYSTEM).message("Chào mừng").link("/").build());
        mvc.perform(get("/api/notifications").cookie(peerCookie)).andExpect(status().isOk());
        mvc.perform(get("/api/notifications/unread-count").cookie(peerCookie)).andExpect(status().isOk());
        mvc.perform(patch("/api/notifications/{id}/read", notif.getId()).cookie(peerCookie)).andExpect(status().isOk());
        mvc.perform(patch("/api/notifications/read-all").cookie(peerCookie)).andExpect(status().isOk());
        mvc.perform(delete("/api/notifications/{id}", notif.getId()).cookie(peerCookie)).andExpect(status().isOk());

        mvc.perform(post("/api/reports")
                        .cookie(peerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetId\":\"" + publishedPost.getId() + "\",\"type\":\"POST\",\"reason\":\"Spam kiểm thử\"}"))
                .andExpect(status().isCreated());

        // 8. USER is strictly forbidden (403 JSON) on ALL MODERATOR & ADMIN endpoints (even with empty {})
        mvc.perform(post("/api/categories").cookie(peerCookie).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        mvc.perform(put("/api/categories/{id}", category.getId()).cookie(peerCookie).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/categories/{id}", category.getId()).cookie(peerCookie))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/moderation/posts/{id}/status", publishedPost.getId())
                        .cookie(peerCookie).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/moderation/sentiment").cookie(peerCookie)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/reports").cookie(peerCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        mvc.perform(get("/api/admin/posts").cookie(peerCookie)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/stats").cookie(peerCookie)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").cookie(peerCookie)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/settings").cookie(peerCookie)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/tags/springboot").cookie(peerCookie)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"java\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void moderatorAndAdminRoleMatrixWorksViaHttpOnlyCookie() throws Exception {
        Cookie modCookie = loginAccessCookie(modUser);
        Cookie adminCookie = loginAccessCookie(adminUser);
        Cookie peerCookie = loginAccessCookie(peerUser);

        // Create a report to moderate
        MvcResult repRes = mvc.perform(post("/api/reports")
                        .cookie(peerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetId\":\"" + publishedPost.getId() + "\",\"type\":\"POST\",\"reason\":\"Kiểm duyệt\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String reportId = objectMapper.readTree(repRes.getResponse().getContentAsString()).path("data").path("id").asString();

        // MODERATOR allowed endpoints
        mvc.perform(get("/api/admin/reports").cookie(modCookie)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/posts").cookie(modCookie)).andExpect(status().isOk());
        mvc.perform(get("/api/moderation/sentiment").cookie(modCookie)).andExpect(status().isOk());
        mvc.perform(patch("/api/admin/reports/{id}/dismiss", reportId).cookie(modCookie)).andExpect(status().isOk());
        mvc.perform(patch("/api/moderation/posts/{id}/status", publishedPost.getId())
                        .cookie(modCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HIDDEN\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/admin/posts/{id}/status", publishedPost.getId())
                        .cookie(modCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk());

        // MODERATOR forbidden on ADMIN-only endpoints (403 JSON)
        mvc.perform(get("/api/admin/stats").cookie(modCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        mvc.perform(get("/api/admin/users").cookie(modCookie)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/settings").cookie(modCookie)).andExpect(status().isForbidden());
        mvc.perform(post("/api/categories").cookie(modCookie).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/tags/springboot").cookie(modCookie)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"java\"}"))
                .andExpect(status().isForbidden());

        // ADMIN full control (stats, users, settings incl. allowRegistration enforcement, categories, tags, ban revocation)
        mvc.perform(get("/api/admin/stats").cookie(adminCookie)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/users").cookie(adminCookie)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/settings").cookie(adminCookie)).andExpect(status().isOk());
        mvc.perform(put("/api/admin/settings")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"siteName\":\"VietBlog Defense\",\"allowRegistration\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowRegistration").value(false));

        // Registration is blocked while allowRegistration == false
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Blocked\",\"email\":\"blocked@vietblog.test\",\"password\":\"Password123!\",\"passwordConfirm\":\"Password123!\"}"))
                .andExpect(status().isBadRequest());

        // Re-enable registration
        mvc.perform(put("/api/admin/settings")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"allowRegistration\":true}"))
                .andExpect(status().isOk());

        // Category CRUD as ADMIN
        MvcResult catRes = mvc.perform(post("/api/categories")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"AI Research\",\"description\":\"Trí tuệ nhân tạo\",\"icon\":\"🤖\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode catNode = objectMapper.readTree(catRes.getResponse().getContentAsString()).path("data");
        String newCatId = catNode.path("id").asString();
        mvc.perform(put("/api/categories/{id}", newCatId)
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"AI & ML\",\"description\":\"Học máy\",\"icon\":\"🤖\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/categories/{id}", newCatId).cookie(adminCookie)).andExpect(status().isOk());

        // Admin banning a user immediately revokes their active HttpOnly cookie!
        mvc.perform(patch("/api/admin/users/{id}/ban", peerUser.getId())
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"banned\":true}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/users/me").cookie(peerCookie)).andExpect(status().isUnauthorized());
    }
}

