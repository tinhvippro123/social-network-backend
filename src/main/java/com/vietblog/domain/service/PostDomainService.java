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
        double baseScore = (post.getUpvotesCount() * 2.0) 
                         + (post.getCommentsCount() * 3.0) 
                         + (post.getViewsCount() * 0.1);
                         
        // Phân rã theo thời gian (Gravity decay)
        long hoursAge = Duration.between(post.getCreatedAt(), LocalDateTime.now()).toHours();
        double gravity = 1.8;
        
        // Tránh chia cho 0
        double agePenalty = Math.pow(hoursAge + 2, gravity);
        
        double finalScore = baseScore / agePenalty;
        
        post.setTrendingScore(finalScore);
    }

    /**
     * Validate nghiệp vụ khi đăng bài (Ví dụ: Chống spam từ ngữ, kiểm tra độ dài)
     */
    public void validatePostContent(Post post) {
        if (post.getContent().length() < 50) {
            throw new BusinessRuleException(ErrorCode.INVALID_INPUT);
        }
        
        // Có thể thêm logic gọi AI kiểm duyệt từ ngữ tục tĩu ở đây sau
    }
}
