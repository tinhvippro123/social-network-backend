package com.vietblog.application.service;

import com.vietblog.application.port.SearchIndexGateway;
import com.vietblog.domain.entity.Post;
import com.vietblog.domain.entity.PostImageFeatures;
import com.vietblog.domain.search.ImageAlgorithms;
import com.vietblog.infrastructure.repository.PostImageFeaturesRepository;
import com.vietblog.infrastructure.repository.PostRepository;
import com.vietblog.infrastructure.search.SearchFileReader;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Extracts Color Histogram (HSV) + Sobel + GLCM feature vectors from post cover images
 * so that "Tìm kiếm bằng hình ảnh" has an index to compare against (stored in PostgreSQL + pgvector).
 */
@Service
public class CoverImageIndexer {
    private static final Logger log = LoggerFactory.getLogger(CoverImageIndexer.class);

    private final PostRepository posts;
    private final PostImageFeaturesRepository index;
    private final SearchFileReader files;
    private final SearchIndexGateway indexGateway;
    private final TransactionTemplate tx;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "cover-image-indexer");
        thread.setDaemon(true);
        return thread;
    });

    @Autowired
    public CoverImageIndexer(PostRepository posts, PostImageFeaturesRepository index,
                             SearchFileReader files, PlatformTransactionManager transactionManager,
                             SearchIndexGateway indexGateway) {
        this.posts = posts;
        this.index = index;
        this.files = files;
        this.tx = new TransactionTemplate(transactionManager);
        this.indexGateway = indexGateway != null ? indexGateway : SearchIndexGateway.NOOP;
    }

    public CoverImageIndexer(PostRepository posts, PostImageFeaturesRepository index,
                             SearchFileReader files, PlatformTransactionManager transactionManager) {
        this(posts, index, files, transactionManager, SearchIndexGateway.NOOP);
    }

    /** Schedule (re)indexing once the surrounding transaction commits. */
    public void scheduleAfterCommit(String postId) {
        if (postId == null) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { worker.submit(() -> indexNow(postId)); }
            });
        } else {
            worker.submit(() -> indexNow(postId));
        }
    }

    /** On startup, index every published post whose cover image is not indexed yet (e.g. after reseeding). */
    @EventListener(ApplicationReadyEvent.class)
    public void indexMissingOnStartup() {
        worker.submit(() -> {
            List<String> pending = tx.execute(status -> posts.findByStatus(Post.PostStatus.PUBLISHED, Pageable.unpaged())
                    .stream()
                    .filter(post -> post.getCoverImage() != null && !post.getCoverImage().isBlank())
                    .map(Post::getId)
                    .filter(id -> !index.existsById(id))
                    .toList());
            if (pending == null || pending.isEmpty()) return;
            int indexed = 0;
            for (String id : pending) if (indexNow(id)) indexed++;
            log.info("Image search index: {}/{} cover images analysed (Color Histogram + Sobel + GLCM).", indexed, pending.size());
        });
    }

    boolean indexNow(String postId) {
        try {
            String coverUrl = tx.execute(status -> posts.findById(postId).map(Post::getCoverImage).orElse(null));
            if (coverUrl == null || coverUrl.isBlank()) {
                tx.executeWithoutResult(status -> index.findById(postId).ifPresent(index::delete));
                return false;
            }
            BufferedImage image = files.imageFromUrl(coverUrl);
            if (image == null) return false;
            double[] rawVector = ImageAlgorithms.features(image);
            String vector = Arrays.stream(rawVector)
                    .mapToObj(Double::toString)
                    .collect(Collectors.joining(","));
            tx.executeWithoutResult(status -> posts.findById(postId).ifPresent(post -> {
                PostImageFeatures features = index.findById(postId).orElseGet(PostImageFeatures::new);
                features.setPost(post);
                features.setFeatures(vector);
                index.saveAndFlush(features);
            }));
            indexGateway.upsertPostImageVector(postId, rawVector);
            return true;
        } catch (RuntimeException ex) {
            log.debug("Skipped image indexing for post {}: {}", postId, ex.getMessage());
            return false;
        }
    }

    @PreDestroy
    public void shutdown() { worker.shutdownNow(); }
}
