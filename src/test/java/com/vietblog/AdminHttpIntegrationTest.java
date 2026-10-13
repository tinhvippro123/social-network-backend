package com.vietblog;

import com.vietblog.application.service.PostApplicationService;
import com.vietblog.domain.entity.*;
import com.vietblog.infrastructure.repository.*;
import com.vietblog.infrastructure.security.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class AdminHttpIntegrationTest extends VietblogBackendApplicationTests {
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired PostRepository posts;
    @Autowired CategoryRepository categories;
    @Autowired CommentRepository comments;
    @Autowired ReactionRepository reactions;
    @Autowired BookmarkRepository bookmarks;
    @Autowired JwtTokenProvider jwt;
    @Autowired PostApplicationService postService;
    MockMvc mvc;
    User owner;
    User reader;
    Post post;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = users.save(User.builder().name("Writer").email(UUID.randomUUID() + "@example.test").password("unused").build());
        reader = users.save(User.builder().name("Reader").email(UUID.randomUUID() + "@example.test").password("unused").build());
        Category category = categories.save(Category.builder().name("Test-" + UUID.randomUUID()).slug(UUID.randomUUID().toString()).build());
        post = posts.save(Post.builder().title("Unique searchable " + UUID.randomUUID()).content("Useful content about Java and Spring. ".repeat(3))
                .author(owner).category(category).tags(new ArrayList<>(List.of("java", "spring")))
                .latitude(10.8).longitude(106.7).status(Post.PostStatus.PUBLISHED).build());
        posts.flush();
    }

    private String token(User user) {
        var principal = UserPrincipal.create(user);
        return "Bearer " + jwt.generateToken(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test void moderatorCanReviewReportsButCannotAdministerUsers() throws Exception {
        reader.setRole(User.Role.MODERATOR);
        String bearer = token(reader);
        mvc.perform(get("/api/admin/reports").header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/users").header("Authorization", bearer)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/moderation/posts/{id}/status", post.getId()).header("Authorization", bearer)
                .contentType("application/json").content("{\"status\":\"HIDDEN\"}")).andExpect(status().isOk());
        assertThat(post.getStatus()).isEqualTo(Post.PostStatus.HIDDEN);
    }

    @Test void adminTagRenameDeduplicatesAssignments() throws Exception {
        reader.setRole(User.Role.ADMIN);
        mvc.perform(patch("/api/admin/tags/java").header("Authorization", token(reader))
                .contentType("application/json").content("{\"name\":\"spring\"}")).andExpect(status().isOk());
        posts.flush();
        assertThat(post.getTags()).containsExactly("spring");
        mvc.perform(delete("/api/admin/tags/spring").header("Authorization", token(reader))).andExpect(status().isOk());
        assertThat(post.getTags()).isEmpty();
    }
}
