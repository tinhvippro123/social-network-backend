package com.vietblog.domain.service;

import com.vietblog.domain.entity.Post;
import com.vietblog.domain.exception.ErrorCode;
import com.vietblog.domain.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Domain Service: Chứa logic nghiệp vụ cốt lõi (Business Logic).
 * Không gọi trực tiếp Database ở đây, chỉ xử lý thuật toán và validate.
 */
@Service
public class PostDomainService {

    /**
     * Thuật toán tính Trending Score dựa trên:
     * - Reddit Hot Ranking (Thời gian phân rã)
     * - Lượng Upvote, Comment, Views
     */
    public void calculateTrendingScore(Post post) {
        // Công thức giả định dựa trên Hacker News Gravity + Reddit Hot
        double baseScore = (Math.max(0, post.getUpvotesCount() - post.getDownvotesCount()) * 2.0)
                         + (post.getCommentsCount() * 3.0) 
                         + (post.getViewsCount() * 0.1);
                         
        // Phân rã theo thời gian (Gravity decay)
        double hoursAge = Math.max(0, Duration.between(post.getCreatedAt(), LocalDateTime.now()).toSeconds() / 3600.0);
        double gravity = 1.8;
        
        // Tránh chia cho 0
        double agePenalty = Math.pow(hoursAge + 2, gravity);
        
        double finalScore = baseScore * (0.5 + wilsonLowerBound(post.getUpvotesCount(), post.getDownvotesCount())) / agePenalty;
        
        post.setTrendingScore(finalScore);
    }

    /** 95% Wilson lower confidence bound; avoids treating one positive vote like a large consensus. */
    public static double wilsonLowerBound(int positive, int negative) {
        double n = (double) Math.max(0, positive) + Math.max(0, negative);
        if (n == 0) return 0;
        double z = 1.96, p = Math.max(0, positive) / n;
        return (p + z * z / (2 * n) - z * Math.sqrt((p * (1 - p) + z * z / (4 * n)) / n)) / (1 + z * z / n);
    }

    /**
     * Validate nghiệp vụ khi đăng bài (Ví dụ: Chống spam từ ngữ, kiểm tra độ dài)
     */
    public void validatePostContent(Post post) {
        if (post.getContent() == null || post.getContent().trim().length() < 50) {
            throw new BusinessRuleException(ErrorCode.POST_CONTENT_TOO_SHORT);
        }
        
        // Có thể thêm logic gọi AI kiểm duyệt từ ngữ tục tĩu ở đây sau
    }
}
