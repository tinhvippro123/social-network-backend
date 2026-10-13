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
class PostInteractionIntegrationTest extends VietblogBackendApplicationTests {
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

    @Test void hiddenPostsCannotBeReadSearchedOrInteractedWith() throws Exception {
        post.setStatus(Post.PostStatus.HIDDEN);
        posts.flush();
        mvc.perform(get("/api/posts/{id}", post.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/posts/{id}/comments", post.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/posts/{id}/reactions/summary", post.getId())).andExpect(status().isNotFound());
        mvc.perform(post("/api/posts/{id}/upvote", post.getId()).header("Authorization", token(reader))).andExpect(status().isNotFound());
        mvc.perform(get("/api/search").param("q", post.getTitle())).andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        assertThat(posts.searchByKeyword("Useful content about Java", org.springframework.data.domain.PageRequest.of(0, 20)).getContent()).doesNotContain(post);
    }

    @Test void bookmarkListIsPrivateAndOmitsHiddenPosts() throws Exception {
        String bearer = token(reader);
        mvc.perform(post("/api/posts/{id}/bookmarks", post.getId()).header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(get("/api/bookmarks").header("Authorization", bearer)).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(post.getId()));
        mvc.perform(get("/api/bookmarks")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/posts/{id}/bookmarks/check", post.getId())).andExpect(status().is4xxClientError());
        post.setStatus(Post.PostStatus.HIDDEN);
        mvc.perform(get("/api/bookmarks").header("Authorization", bearer)).andExpect(jsonPath("$.data").isEmpty());
    }

    @Test void votesSwitchAndToggleWithoutCounterDrift() throws Exception {
        String bearer = token(reader);
        mvc.perform(post("/api/posts/{id}/upvote", post.getId()).header("Authorization", bearer)).andExpect(status().isOk());
        assertThat(post.getUpvotesCount()).isEqualTo(1);
        mvc.perform(post("/api/posts/{id}/downvote", post.getId()).header("Authorization", bearer)).andExpect(status().isOk());
        assertThat(post.getUpvotesCount()).isZero();
        assertThat(post.getDownvotesCount()).isEqualTo(1);
        mvc.perform(post("/api/posts/{id}/downvote", post.getId()).header("Authorization", bearer)).andExpect(status().isOk());
        assertThat(post.getDownvotesCount()).isZero();
        assertThat(reactions.findByPostId(post.getId())).isEmpty();
    }

    @Test void deletingPostRemovesDependentInteractions() {
        Comment parent = comments.save(Comment.builder().post(post).author(reader).content("Parent").build());
        comments.save(Comment.builder().post(post).author(reader).parent(parent).content("Reply").build());
        reactions.save(Reaction.builder().post(post).user(reader).emoji("UPVOTE").build());
        bookmarks.save(Bookmark.builder().post(post).user(reader).build());
        posts.flush();
        postService.deletePost(post.getId(), owner.getId());
        posts.flush();
        assertThat(posts.findById(post.getId())).isEmpty();
        assertThat(comments.countByPostId(post.getId())).isZero();
        assertThat(reactions.findByPostId(post.getId())).isEmpty();
        assertThat(bookmarks.existsByUserIdAndPostId(reader.getId(), post.getId())).isFalse();
    }

}
