package com.vietblog.application.service;

import com.vietblog.application.dto.post.PostResponse;
import com.vietblog.application.dto.search.SummaryResponse;
import com.vietblog.application.port.SearchIndexGateway;
import com.vietblog.domain.entity.Post;
import com.vietblog.domain.entity.PostImageFeatures;
import com.vietblog.domain.exception.BusinessRuleException;
import com.vietblog.domain.exception.ErrorCode;
import com.vietblog.domain.exception.ForbiddenException;
import com.vietblog.domain.exception.ResourceNotFoundException;
import com.vietblog.domain.search.GeoAlgorithms;
import com.vietblog.domain.search.ImageAlgorithms;
import com.vietblog.domain.search.TextAlgorithms;
import com.vietblog.infrastructure.repository.BookmarkRepository;
import com.vietblog.infrastructure.repository.PostImageFeaturesRepository;
import com.vietblog.infrastructure.repository.SearchCommentRepository;
import com.vietblog.infrastructure.repository.SearchPostRepository;
import com.vietblog.infrastructure.search.SearchFileReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional(readOnly = true)
public class SearchApplicationService {
    public static final int MAX_CORPUS = 2000;
    private final SearchPostRepository posts;
    private final PostImageFeaturesRepository imageIndex;
    private final SearchCommentRepository comments;
    private final BookmarkRepository bookmarks;
    private final SearchFileReader files;
    private final SearchIndexGateway indexGateway;

    @Autowired
    public SearchApplicationService(
            SearchPostRepository posts,
            PostImageFeaturesRepository imageIndex,
            SearchCommentRepository comments,
            BookmarkRepository bookmarks,
            SearchFileReader files,
            SearchIndexGateway indexGateway) {
        this.posts = posts;
        this.imageIndex = imageIndex;
        this.comments = comments;
        this.bookmarks = bookmarks;
        this.files = files;
        this.indexGateway = indexGateway != null ? indexGateway : SearchIndexGateway.NOOP;
    }

    public SearchApplicationService(
            SearchPostRepository posts,
            PostImageFeaturesRepository imageIndex,
            SearchCommentRepository comments,
            BookmarkRepository bookmarks,
            SearchFileReader files) {
        this(posts, imageIndex, comments, bookmarks, files, SearchIndexGateway.NOOP);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public Map<String, Long> sentimentStats() {
        Map<String, Long> counts = new LinkedHashMap<>(Map.of("positive", 0L, "negative", 0L, "neutral", 0L));
        try (var texts = comments.streamContents()) {
            texts.forEach(text -> counts.merge(TextAlgorithms.sentiment(text).label(), 1L, Long::sum));
        }
        return counts;
    }

    private List<Post> corpus() {
        return posts.findByStatusOrderByCreatedAtDescIdAsc(Post.PostStatus.PUBLISHED, PageRequest.of(0, MAX_CORPUS));
    }

    public static String searchableText(Post post) {
        String tags = post.getTags() == null ? "" : String.join(" ", post.getTags());
        String locationAddress = Objects.toString(post.getLocationAddress(), "");
        String value = post.getTitle() + " " + post.getTitle() + " "
                + Objects.toString(post.getExcerpt(), "") + " "
                + Objects.toString(post.getContent(), "") + " "
                + tags + " " + locationAddress;
        return value.substring(0, Math.min(value.length(), 20_000));
    }

    public List<PostResponse> search(String query, String type, int limit) {
        return search(query, type, limit, null);
    }

    public List<PostResponse> search(String query, String type, int limit, String viewerId) {
        validateLimit(limit);
        if (query == null || query.isBlank() || query.length() > 1000) throw invalid();
        if (!"keyword".equals(type) && !"semantic".equals(type)) throw invalid();
        return textSearch(query, type, limit, viewerId);
    }

    private List<PostResponse> textSearch(String query, String type, int limit, String viewerId) {
        List<Post> corpus;
        if ("semantic".equals(type)) {
            double[] queryVector = TextAlgorithms.hashedTfIdfVector(query);
            Optional<List<String>> candidateIds = indexGateway.findSemanticCandidateIds(queryVector, Math.max(limit * 5, 100));
            corpus = candidateIds.filter(ids -> !ids.isEmpty())
                    .map(posts::findPublishedByIdIn)
                    .orElseGet(this::corpus);
        } else {
            corpus = corpus();
        }
        if (corpus.isEmpty()) return List.of();
        double[] scores;
        List<String> texts = corpus.stream().map(SearchApplicationService::searchableText).toList();
        if ("semantic".equals(type)) {
            scores = TextAlgorithms.similarities(texts, query);
        } else {
            Set<String> terms = new HashSet<>(TextAlgorithms.tokens(query));
            scores = texts.stream().map(TextAlgorithms::tokens).map(HashSet::new)
                    .mapToDouble(words -> terms.isEmpty() || !words.containsAll(terms) ? 0 : (double) terms.size() / Math.max(1, words.size())).toArray();
        }
        return IntStream.range(0, corpus.size()).boxed().filter(i -> scores[i] > 0)
                .sorted(Comparator.<Integer>comparingDouble(i -> scores[i]).reversed().thenComparingInt(i -> i)).limit(limit)
                .map(i -> toPostResponse(corpus.get(i), viewerId)).toList();
    }

    public List<PostResponse> searchDocument(MultipartFile document, int limit) {
        return searchDocument(document, limit, null);
    }

    public List<PostResponse> searchDocument(MultipartFile document, int limit, String viewerId) {
        validateLimit(limit);
        return textSearch(files.document(document), "semantic", limit, viewerId);
    }

    public List<PostResponse> searchImage(MultipartFile image, int limit) {
        return searchImage(image, limit, null);
    }

    public List<PostResponse> searchImage(MultipartFile image, int limit, String viewerId) {
        validateLimit(limit);
        double[] query = ImageAlgorithms.features(files.image(image));
        Optional<List<String>> candidateIds = indexGateway.findImageCandidateIds(query, Math.max(limit * 3, 50));
        List<Post> corpus = candidateIds.filter(ids -> !ids.isEmpty())
                .map(posts::findPublishedByIdIn)
                .orElseGet(this::corpus);
        if (corpus.isEmpty()) return List.of();
        Map<String, Post> byId = corpus.stream().collect(Collectors.toMap(Post::getId, post -> post));
        Map<String, Double> scores = new HashMap<>();
        imageIndex.findByPostIdIn(byId.keySet()).forEach(index -> scores.put(index.getPostId(), ImageAlgorithms.cosine(query, decode(index.getFeatures()))));
        return scores.entrySet().stream().filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed().thenComparing(Map.Entry::getKey)).limit(limit)
                .map(entry -> toPostResponse(byId.get(entry.getKey()), viewerId)).toList();
    }

    @Transactional
    public void indexImage(String postId, String userId, boolean admin, MultipartFile image) {
        Post post = posts.findLockedById(postId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND));
        if (!admin && !post.getAuthor().getId().equals(userId)) throw new ForbiddenException(ErrorCode.FORBIDDEN);
        double[] vector = ImageAlgorithms.features(files.image(image));
        PostImageFeatures index = imageIndex.findById(postId).orElseGet(PostImageFeatures::new);
        index.setPost(post);
        index.setFeatures(Arrays.stream(vector).mapToObj(Double::toString).collect(Collectors.joining(",")));
        imageIndex.saveAndFlush(index);
        indexGateway.upsertPostImageVector(postId, vector);
    }

    public List<PostResponse> nearby(double latitude, double longitude, double radiusKm, int limit) {
        return nearby(latitude, longitude, radiusKm, limit, null);
    }

    public List<PostResponse> nearby(double latitude, double longitude, double radiusKm, int limit, String viewerId) {
        validateLimit(limit);
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude) || !Double.isFinite(radiusKm)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180 || radiusKm <= 0 || radiusKm > 500) throw invalid();

        Optional<List<String>> spatialCandidates = indexGateway.findNearbyCandidateIds(latitude, longitude, radiusKm, limit);
        List<Post> candidates;
        if (spatialCandidates.isPresent()) {
            if (spatialCandidates.get().isEmpty()) {
                return List.of();
            }
            candidates = posts.findPublishedByIdIn(spatialCandidates.get());
        } else {
            candidates = corpus();
        }

        ToDoubleFunction<Post> distance = post -> GeoAlgorithms.distanceKm(latitude, longitude, post.getLatitude(), post.getLongitude());
        return candidates.stream()
                .filter(post -> post.getLatitude() != null && post.getLongitude() != null)
                .filter(post -> distance.applyAsDouble(post) <= radiusKm)
                .sorted(Comparator.comparingDouble(distance).thenComparing(Post::getId)).limit(limit)
                .map(post -> toPostResponse(post, viewerId)).toList();
    }

    private PostResponse toPostResponse(Post post, String viewerId) {
        int authorPostsCount = post.getAuthor() != null ? post.getAuthor().getPostsCount() : 0;
        boolean isBookmarked = viewerId != null && bookmarks.existsByUserIdAndPostId(viewerId, post.getId());
        return PostResponse.fromEntity(post, authorPostsCount, isBookmarked);
    }

    public SummaryResponse summary(String postId, int sentences) {
        if (sentences < 1 || sentences > 10) throw invalid();
        Post post = published(postId);
        String content = Objects.toString(post.getContent(), "");
        List<String> selected = TextAlgorithms.summarize(content.substring(0, Math.min(content.length(), SearchFileReader.MAX_TEXT)), sentences);
        return new SummaryResponse(postId, String.join(" ", selected), selected, "tfidf-textrank-v1");
    }

    public TextAlgorithms.Sentiment commentSentiment(String postId, String commentId) {
        published(postId);
        var comment = comments.findById(commentId).filter(item -> item.getPost().getId().equals(postId))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COMMENT_NOT_FOUND));
        return TextAlgorithms.sentiment(comment.getContent());
    }

    private Post published(String postId) {
        return posts.findById(postId).filter(post -> post.getStatus() == Post.PostStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND));
    }

    private static double[] decode(String features) {
        String[] fields = features.split(",");
        if (fields.length != ImageAlgorithms.DIMENSIONS) return new double[0];
        return Arrays.stream(fields).mapToDouble(Double::parseDouble).toArray();
    }

    private static void validateLimit(int limit) { if (limit < 1 || limit > 100) throw invalid(); }
    private static BusinessRuleException invalid() { return new BusinessRuleException(ErrorCode.INVALID_INPUT); }
}
