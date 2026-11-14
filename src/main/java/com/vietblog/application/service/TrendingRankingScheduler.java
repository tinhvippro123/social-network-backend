package com.vietblog.application.service;

import com.vietblog.domain.entity.Notification;
import com.vietblog.domain.entity.Post;
import com.vietblog.domain.service.PostDomainService;
import com.vietblog.infrastructure.repository.NotificationRepository;
import com.vietblog.infrastructure.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    @SuppressWarnings("unused")
    private final NotificationService notificationService;

    private static final double TRENDING_NOTIFY_THRESHOLD = 1.0;

    private record UserLinkKey(String userId, String link) {}

    @Scheduled(fixedRate = 300_000, initialDelay = 15_000) // 5 minutes
    @Transactional
    public void recalculateTrendingScores() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusHours(72);
            List<Post> publishedPosts = postRepository.findRecentPublishedWithAuthor(threshold);

            if (publishedPosts.isEmpty()) {
                return;
            }

            List<Post> trendingCandidates = new ArrayList<>();
            for (Post post : publishedPosts) {
                postDomainService.calculateTrendingScore(post);
                if (post.getTrendingScore() >= TRENDING_NOTIFY_THRESHOLD
                        && post.getAuthor() != null
                        && post.getAuthor().isNotifyTrendingEffective()) {
                    trendingCandidates.add(post);
                }
            }

            postRepository.saveAll(publishedPosts);

            if (!trendingCandidates.isEmpty()) {
                List<String> candidateLinks = trendingCandidates.stream()
                        .map(p -> "/posts/" + p.getId())
                        .distinct()
                        .toList();
                Set<UserLinkKey> existingKeys = new HashSet<>();
                for (int i = 0; i < candidateLinks.size(); i += 500) {
                    List<String> chunk = candidateLinks.subList(i, Math.min(candidateLinks.size(), i + 500));
                    for (Object[] row : notificationRepository.findUserIdsAndLinksByTypeAndLinkIn(
                            Notification.NotificationType.SYSTEM, chunk)) {
                        existingKeys.add(new UserLinkKey((String) row[0], (String) row[1]));
                    }
                }

                List<Notification> toNotify = new ArrayList<>();
                for (Post post : trendingCandidates) {
                    String link = "/posts/" + post.getId();
                    UserLinkKey key = new UserLinkKey(post.getAuthor().getId(), link);
                    if (existingKeys.add(key)) {
                        toNotify.add(Notification.builder()
                                .user(post.getAuthor())
                                .type(Notification.NotificationType.SYSTEM)
                                .message("Bài viết \"" + post.getTitle() + "\" của bạn đang thịnh hành trong cộng đồng!")
                                .avatar(post.getAuthor().getAvatar())
                                .link(link)
                                .build());
                    }
                }
                if (!toNotify.isEmpty()) {
                    notificationRepository.saveAll(toNotify);
                }
            }

            log.debug("Successfully recalculated trending scores for {} published posts (last 72h)", publishedPosts.size());
        } catch (Exception e) {
            log.error("Failed to run trending ranking scheduled task: {}", e.getMessage());
        }
    }
}
