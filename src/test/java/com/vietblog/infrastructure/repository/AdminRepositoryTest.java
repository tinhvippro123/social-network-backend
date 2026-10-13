package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.Category;
import com.vietblog.domain.entity.Notification;
import com.vietblog.domain.entity.Post;
import com.vietblog.domain.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never",
    "spring.datasource.url=jdbc:h2:mem:admin_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AdminRepositoryTest {
    @Autowired EntityManager entityManager;
    @Autowired TagRepository tags;
    @Autowired AdminUserRepository users;
    @Autowired AdminPostRepository posts;

    @Test
    void tagCountsIncludeOnlyDistinctPublishedPosts() {
        User author = persistUser("author@example.test");
        Category category = Category.builder().name("Technology").slug("technology").build();
        entityManager.persist(category);
        persistPost(author, category, "One", Post.PostStatus.PUBLISHED, List.of("java", "java", "spring"));
        persistPost(author, category, "Two", Post.PostStatus.PUBLISHED, List.of("java"));
        persistPost(author, category, "Draft", Post.PostStatus.DRAFT, List.of("secret", "java"));
        persistPost(author, category, "Hidden", Post.PostStatus.HIDDEN, List.of("hidden"));
        entityManager.flush();

        var result = tags.findPopular("", PageRequest.of(0, 10));
        assertEquals(List.of("java", "spring"), result.stream().map(TagRepository.Usage::getName).toList());
        assertEquals(2, result.get(0).getPostsCount());
        assertEquals(1, tags.findPopular("SPR", PageRequest.of(0, 10)).size());
        assertEquals(1, posts.search(Post.PostStatus.DRAFT, "draft", PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void adminUserSearchCombinesRoleAndCaseInsensitiveSearch() {
        persistUser("member@example.test");
        User moderator = persistUser("moderator@example.test");
        moderator.setRole(User.Role.MODERATOR);
        entityManager.flush();
        assertEquals(1, users.search(User.Role.MODERATOR, "MODERATOR", PageRequest.of(0, 10)).getTotalElements());
        assertEquals(0, users.search(User.Role.USER, "MODERATOR", PageRequest.of(0, 10)).getTotalElements());
        assertEquals(2, users.search(null, "", PageRequest.of(0, 10)).getTotalElements());
    }

    private User persistUser(String email) {
        User user = User.builder().email(email).password("unused-test-password").name(email).build();
        entityManager.persist(user);
        return user;
    }

    private void persistPost(User author, Category category, String title, Post.PostStatus status, List<String> postTags) {
        entityManager.persist(Post.builder().author(author).category(category).title(title).content("Test content")
                .status(status).tags(postTags).build());
    }
}
