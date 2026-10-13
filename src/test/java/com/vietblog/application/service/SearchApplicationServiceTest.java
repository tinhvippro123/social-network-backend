package com.vietblog.application.service;

import com.vietblog.domain.entity.*;
import com.vietblog.domain.exception.*;
import com.vietblog.infrastructure.repository.*;
import com.vietblog.infrastructure.search.SearchFileReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SearchApplicationServiceTest {
    private final SearchPostRepository posts = mock(SearchPostRepository.class);
    private final PostImageFeaturesRepository images = mock(PostImageFeaturesRepository.class);
    private final SearchCommentRepository comments = mock(SearchCommentRepository.class);
    private final SearchFileReader files = mock(SearchFileReader.class);
    private final SearchApplicationService service = new SearchApplicationService(posts, images, comments, files);
    private Post post;

    @BeforeEach void setUp() {
        post = Post.builder().id("published").title("Java REST API").content("Java Spring Boot API guide.")
                .status(Post.PostStatus.PUBLISHED).author(User.builder().id("owner").name("Author").build())
                .category(Category.builder().id("category").name("Java").build()).latitude(0.0).longitude(1.0).build();
    }

    @Test void searchOnlyLoadsPublishedPostsAndUnknownQueriesAreEmpty() {
        when(posts.findByStatusOrderByCreatedAtDescIdAsc(eq(Post.PostStatus.PUBLISHED), any())).thenReturn(List.of(post));
        assertThat(service.search("REST", "semantic", 10)).extracting("id").containsExactly("published");
        assertThat(service.search("python", "semantic", 10)).isEmpty();
        assertThat(service.search("Java missing", "keyword", 10)).isEmpty();
        verify(posts, times(3)).findByStatusOrderByCreatedAtDescIdAsc(eq(Post.PostStatus.PUBLISHED), any());
    }

    @Test void radiusFiltersByRealDistanceAndRejectsInvalidCoordinates() {
        when(posts.findByStatusOrderByCreatedAtDescIdAsc(eq(Post.PostStatus.PUBLISHED), any())).thenReturn(List.of(post));
        assertThat(service.nearby(0, 0, 100, 10)).isEmpty();
        assertThat(service.nearby(0, 0, 120, 10)).hasSize(1);
        assertThatThrownBy(() -> service.nearby(Double.NaN, 0, 5, 10)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.nearby(91, 0, 5, 10)).isInstanceOf(BusinessRuleException.class);
    }

    @Test void preventsIndexingSomeoneElsesPostBeforeReadingTheFile() {
        when(posts.findLockedById(post.getId())).thenReturn(Optional.of(post));
        assertThatThrownBy(() -> service.indexImage(post.getId(), "stranger", false, null)).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(files, images);
    }

    @Test void doesNotExposeDraftSummariesOrCommentsFromAnotherPost() {
        post.setStatus(Post.PostStatus.DRAFT);
        when(posts.findById(post.getId())).thenReturn(Optional.of(post));
        assertThatThrownBy(() -> service.summary(post.getId(), 3)).isInstanceOf(ResourceNotFoundException.class);
        post.setStatus(Post.PostStatus.PUBLISHED);
        Comment other = Comment.builder().id("comment").post(Post.builder().id("other").build()).content("good").build();
        when(comments.findById("comment")).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.commentSentiment(post.getId(), "comment")).isInstanceOf(ResourceNotFoundException.class);
    }
}
