package com.vietblog.application.service;

import com.vietblog.domain.entity.Notification;
import com.vietblog.domain.entity.Post;
import com.vietblog.domain.service.PostDomainService;
import com.vietblog.infrastructure.repository.NotificationRepository;
import com.vietblog.infrastructure.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Background Scheduler running periodically (every 5 minutes)
 * to recalculate trending scores for published posts, matching
 * the research specifications defined in algorithms.md.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TrendingRankingScheduler {

    private final PostRepository postRepository;
    private final PostDomainService postDomainService;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    private static final double TRENDING_NOTIFY_THRESHOLD = 1.0;

    @Scheduled(fixedRate = 300_000, initialDelay = 15_000) // 5 minutes
    @Transactional
    public void recalculateTrendingScores() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusHours(72);
            List<Post> publishedPosts = postRepository.findByStatus(Post.PostStatus.PUBLISHED, Pageable.unpaged())
                    .getContent()
                    .stream()
                    .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(threshold))
                    .collect(Collectors.toList());

            if (publishedPosts.isEmpty()) {
                return;
            }

            for (Post post : publishedPosts) {
                postDomainService.calculateTrendingScore(post);
            }

            postRepository.saveAll(publishedPosts);

            for (Post post : publishedPosts) {
                if (post.getTrendingScore() >= TRENDING_NOTIFY_THRESHOLD
                        && post.getAuthor() != null
                        && post.getAuthor().isNotifyTrendingEffective()) {
                    String link = "/posts/" + post.getId();
                    if (!notificationRepository.existsByUserIdAndTypeAndLink(
                            post.getAuthor().getId(), Notification.NotificationType.SYSTEM, link)) {
                        notificationService.notifyWithPreferences(
                                post.getAuthor(),
                                Notification.NotificationType.SYSTEM,
                                "Bài viết \"" + post.getTitle() + "\" của bạn đang thịnh hành trong cộng đồng!",
                                post.getAuthor().getAvatar(),
                                link,
                                null,
                                null
                        );
                    }
                }
            }

            log.debug("Successfully recalculated trending scores for {} published posts (last 72h)", publishedPosts.size());
        } catch (Exception e) {
            log.error("Failed to run trending ranking scheduled task: {}", e.getMessage());
        }
    }
}
