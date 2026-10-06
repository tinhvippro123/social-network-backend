package com.vietblog.application.service;

import com.vietblog.domain.entity.Bookmark;
import com.vietblog.domain.entity.Post;
import com.vietblog.domain.entity.User;
import com.vietblog.domain.exception.ErrorCode;
import com.vietblog.domain.exception.ResourceNotFoundException;
import com.vietblog.infrastructure.repository.BookmarkRepository;
import com.vietblog.infrastructure.repository.PostRepository;
import com.vietblog.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public boolean toggleBookmark(String postId, String userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        if (bookmarkRepository.existsByUserIdAndPostId(userId, postId)) {
            // Đã bookmark -> Bỏ bookmark
            bookmarkRepository.deleteByUserIdAndPostId(userId, postId);
            return false; // Trả về trạng thái hiện tại là: "Chưa bookmark"
        } else {
            // Chưa bookmark -> Tạo bookmark
            Bookmark bookmark = Bookmark.builder()
                    .user(user)
                    .post(post)
                    .build();
            bookmarkRepository.save(bookmark);
            return true; // Trả về trạng thái hiện tại là: "Đã bookmark"
        }
    }

    @Transactional(readOnly = true)
    public boolean checkIsBookmarked(String postId, String userId) {
        return bookmarkRepository.existsByUserIdAndPostId(userId, postId);
    }
}
