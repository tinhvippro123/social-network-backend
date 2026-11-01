package com.vietblog.infrastructure.config.seed;

import com.vietblog.domain.search.TextAlgorithms;
import com.vietblog.infrastructure.config.seed.SeedModels.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class DefenseDatasetQualityTest {

    private static final List<String> FORBIDDEN_META_PHRASES = List.of(
            "hội đồng chấm điểm",
            "khoe với hội đồng",
            "lúc chấm điểm",
            "nhớ mở bài viết",
            "bấm vào ảnh hoặc ô",
            "điều này quan trọng hơn bạn nghĩ",
            "lorem ipsum",
            "commented on your post"
    );

    private static final List<String> CODING_PHOTO_IDS = List.of(
            "1555066931-4365d14bab8c",
            "1461749280684-dccba630e2f6",
            "1526374965328-7f61d4dc18c5"
    );

    @Test
    void allTwentyPostsAreSubstantialTopicMatchedAndSummarizable() {
        List<PostSeed> allPosts = new ArrayList<>();
        allPosts.addAll(TechAndEventPostsCatalog.posts());
        allPosts.addAll(LifestyleTravelFoodPostsCatalog.posts());

        assertThat(allPosts).hasSize(20);

        Set<String> keys = new HashSet<>();
        Set<String> categories = new HashSet<>();

        for (PostSeed post : allPosts) {
            assertThat(keys.add(post.key())).as("Unique post key: " + post.key()).isTrue();
            categories.add(post.categorySlug());

            assertThat(post.title()).isNotBlank().hasSizeGreaterThan(25);
            assertThat(post.excerpt()).isNotBlank().hasSizeGreaterThan(50);
            assertThat(post.content()).isNotBlank().hasSizeGreaterThan(900);
            assertThat(post.coverImage()).startsWith("https://images.unsplash.com/");

            // Valid Vietnam coordinates
            assertThat(post.latitude()).isBetween(8.0, 24.0);
            assertThat(post.longitude()).isBetween(102.0, 110.0);
            assertThat(post.locationAddress()).isNotBlank();

            // Event posts must carry future schedule metadata
            if ("su-kien".equals(post.categorySlug())) {
                assertThat(post.eventStartsInDays()).isNotNull().isPositive();
                assertThat(post.eventDurationHours()).isNotNull().isPositive();
            }

            // Travel & Food posts must not contain coding screen stock photos
            if ("du-lich".equals(post.categorySlug()) || "am-thuc".equals(post.categorySlug())) {
                for (String codeImg : CODING_PHOTO_IDS) {
                    assertThat(post.content()).doesNotContain(codeImg);
                }
                assertThat(post.content()).contains("<p><img ");
            }

            String lowerContent = post.content().toLowerCase(Locale.ROOT);
            for (String forbidden : FORBIDDEN_META_PHRASES) {
                assertThat(lowerContent).doesNotContain(forbidden);
            }

            // TextRank summarization produces 3 coherent sentences
            List<String> summarySentences = TextAlgorithms.summarize(post.content(), 3);
            assertThat(summarySentences).hasSize(3);
            summarySentences.forEach(s -> assertThat(s.length()).isGreaterThan(30));
        }

        assertThat(categories).containsExactlyInAnyOrder(
                "cong-nghe", "du-lich", "am-thuc", "doi-song", "su-kien"
        );
    }

    @Test
    void commentsCoverThreeLevelsAndAllSentimentPolarities() {
        List<PostSeed> allPosts = new ArrayList<>();
        allPosts.addAll(TechAndEventPostsCatalog.posts());
        allPosts.addAll(LifestyleTravelFoodPostsCatalog.posts());

        Map<String, Integer> sentimentCounts = new HashMap<>(Map.of(
                "positive", 0, "neutral", 0, "negative", 0
        ));
        int maxDepthObserved = 0;

        for (PostSeed post : allPosts) {
            assertThat(post.comments()).isNotEmpty();
            for (CommentSeed root : post.comments()) {
                maxDepthObserved = Math.max(maxDepthObserved, inspectCommentDepthAndSentiment(root, 1, sentimentCounts));
            }
        }

        assertThat(maxDepthObserved).isEqualTo(3);
        assertThat(sentimentCounts.get("positive")).isGreaterThan(15);
        assertThat(sentimentCounts.get("neutral")).isGreaterThan(0);
        assertThat(sentimentCounts.get("negative")).isGreaterThan(2);
    }

    private int inspectCommentDepthAndSentiment(CommentSeed node, int depth, Map<String, Integer> counts) {
        String label = TextAlgorithms.sentiment(node.content()).label();
        counts.merge(label, 1, Integer::sum);

        int deepest = depth;
        if (node.replies() != null) {
            for (CommentSeed child : node.replies()) {
                deepest = Math.max(deepest, inspectCommentDepthAndSentiment(child, depth + 1, counts));
            }
        }
        return deepest;
    }

    @Test
    void groupsAndChatsAreDistinctAndFreeOfFourthWallDemoText() {
        List<GroupSeed> groups = CommunityAndSocialCatalog.groups();
        assertThat(groups).hasSize(8);

        Set<String> groupPostTitles = new HashSet<>();
        for (GroupSeed group : groups) {
            assertThat(group.posts()).isNotEmpty();
            for (GroupPostSeed gp : group.posts()) {
                assertThat(groupPostTitles.add(gp.title()))
                        .as("Distinct group discussion title: " + gp.title())
                        .isTrue();
            }
        }

        for (GroupChatSeed gc : CommunityAndSocialCatalog.groupChats()) {
            assertThat(gc.messages()).isNotEmpty();
            for (ChatMsgSeed msg : gc.messages()) {
                String lower = msg.content().toLowerCase(Locale.ROOT);
                for (String forbidden : FORBIDDEN_META_PHRASES) {
                    assertThat(lower).doesNotContain(forbidden);
                }
            }
        }

        for (DirectChatSeed dc : CommunityAndSocialCatalog.directChats()) {
            assertThat(dc.messages()).isNotEmpty();
            for (ChatMsgSeed msg : dc.messages()) {
                String lower = msg.content().toLowerCase(Locale.ROOT);
                for (String forbidden : FORBIDDEN_META_PHRASES) {
                    assertThat(lower).doesNotContain(forbidden);
                }
            }
        }
    }
}

