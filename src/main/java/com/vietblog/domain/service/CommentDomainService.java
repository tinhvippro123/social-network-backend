package com.vietblog.domain.service;

import com.vietblog.domain.entity.Comment;
import com.vietblog.domain.exception.BusinessRuleException;
import com.vietblog.domain.exception.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class CommentDomainService {

    /**
     * Validate nội dung bình luận (chống spam, độ dài, v.v.)
     */
    public void validateCommentContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessRuleException(ErrorCode.INVALID_INPUT);
        }
        if (content.length() > 2000) {
            throw new BusinessRuleException(ErrorCode.INVALID_INPUT);
        }
    }

    /**
     * Kiểm tra xem một bình luận có thể được reply không.
     * Ví dụ: Nếu hệ thống chỉ cho phép lồng tối đa 2 cấp (để khỏi vỡ UI),
     * ta có thể đếm cấp của comment hiện tại.
     */
    public void validateNestingLevel(Comment parentComment) {
        int depth = 1;
        Comment current = parentComment;
        while (current.getParent() != null) {
            depth++;
            current = current.getParent();
        }
        
        // Nếu đã lồng quá 2 cấp (1 parent -> 1 child -> 1 sub-child), cấm reply tiếp 
        // hoặc bắt buộc gán nó vào sub-child. (Tuỳ logic FE). Tạm thời cho tối đa 3 cấp.
        if (depth >= 3) {
            throw new BusinessRuleException(ErrorCode.INVALID_INPUT); // Cấp độ quá sâu
        }
    }
}
