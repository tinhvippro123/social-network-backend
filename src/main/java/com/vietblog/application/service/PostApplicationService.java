package com.vietblog.application.service;

import com.vietblog.application.dto.post.CreatePostRequest;
import com.vietblog.application.dto.post.PostResponse;
import com.vietblog.domain.Category;
import com.vietblog.domain.Post;
import com.vietblog.domain.User;
import com.vietblog.domain.service.PostDomainService;
import com.vietblog.infrastructure.repository.CategoryRepository;
import com.vietblog.infrastructure.repository.PostRepository;
import com.vietblog.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Application Service: Điều phối (Orchestration).
 * Lấy data từ DB -> Đưa cho Domain Service xử lý (nếu có) -> Lưu lại DB.
 */
@Service
@RequiredArgsConstructor
public class PostApplicationService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PostDomainService postDomainService;

    @Transactional
    public PostResponse createPost(String authorId, CreatePostRequest request) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        Post post = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .excerpt(request.getExcerpt())
                .coverImage(request.getCoverImage())
                .author(author)
                .category(category)
                .tags(request.getTags())
                .status(Post.PostStatus.valueOf(request.getStatus().toUpperCase()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        // Uỷ quyền cho Domain Service kiểm tra nghiệp vụ
        postDomainService.validatePostContent(post);

        // Lưu vào cơ sở hạ tầng
        Post savedPost = postRepository.save(post);
        
        return PostResponse.fromEntity(savedPost);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getLatestPosts(int page, int limit) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.findByStatus(Post.PostStatus.PUBLISHED, pageRequest)
                .map(PostResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getTrendingPosts(int limit) {
        // Cập nhật lại Trending Score trước khi lấy có thể chạy bằng Batch Job (Cron), 
        // ở đây ta lấy trực tiếp từ DB do đã được tính trước đó.
        PageRequest pageRequest = PageRequest.of(0, limit);
        return postRepository.findTrending(pageRequest)
                .stream()
                .map(PostResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public PostResponse getPostById(String id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        
        // Tăng view count
        post.setViewsCount(post.getViewsCount() + 1);
        
        // Nhờ Domain tính lại điểm trending vì view tăng
        postDomainService.calculateTrendingScore(post);
        
        postRepository.save(post);
        
        return PostResponse.fromEntity(post);
    }
}
