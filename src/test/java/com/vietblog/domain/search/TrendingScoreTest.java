package com.vietblog.domain.search;

import com.vietblog.domain.entity.Post;
import com.vietblog.domain.service.PostDomainService;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

class TrendingScoreTest {
    private final PostDomainService ranker = new PostDomainService();
    @Test void ageAndNegativeVotesReduceRank() {
        Post fresh = Post.builder().upvotesCount(20).createdAt(LocalDateTime.now()).build();
        Post old = Post.builder().upvotesCount(20).createdAt(LocalDateTime.now().minusDays(5)).build();
        Post disputed = Post.builder().upvotesCount(20).downvotesCount(19).createdAt(LocalDateTime.now()).build();
        ranker.calculateTrendingScore(fresh);
        ranker.calculateTrendingScore(old);
        ranker.calculateTrendingScore(disputed);
        assertThat(fresh.getTrendingScore()).isGreaterThan(old.getTrendingScore()).isGreaterThan(disputed.getTrendingScore());
    }
    @Test void futureTimestampAndNoVotesRemainFinite() {
        Post future = Post.builder().createdAt(LocalDateTime.now().plusDays(2)).build();
        ranker.calculateTrendingScore(future);
        assertThat(future.getTrendingScore()).isFinite().isZero();
        assertThat(PostDomainService.wilsonLowerBound(0, 0)).isZero();
        assertThat(PostDomainService.wilsonLowerBound(100, 0)).isGreaterThan(PostDomainService.wilsonLowerBound(1, 0));
    }
}
