package com.vietblog.application.service;

import com.vietblog.domain.entity.Post;
import com.vietblog.domain.entity.Reaction;
import com.vietblog.domain.entity.User;
import com.vietblog.domain.exception.BusinessRuleException;
import com.vietblog.domain.exception.ErrorCode;
import com.vietblog.domain.exception.ResourceNotFoundException;
import com.vietblog.infrastructure.repository.PostRepository;
import com.vietblog.infrastructure.repository.ReactionRepository;
import com.vietblog.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public void toggleReaction(String postId, String userId, String emoji) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        Optional<Reaction> existingReactionOpt = reactionRepository.findByUserIdAndPostId(userId, postId);

        if (existingReactionOpt.isPresent()) {
            Reaction existingReaction = existingReactionOpt.get();
            if (existingReaction.getEmoji().equals(emoji)) {
                // N?u ?n l?i emoji c?, th? l? toggle b? reaction (unlike)
                reactionRepository.delete(existingReaction);
                
                if (emoji.equals("UPVOTE")) post.setUpvotesCount(Math.max(0, post.getUpvotesCount() - 1));
                if (emoji.equals("DOWNVOTE")) post.setDownvotesCount(Math.max(0, post.getDownvotesCount() - 1));
            } else {
                // N?u ch?n emoji kh?c, ti?n h?nh h?y c?i c? v? l?u c?i m?i
                String oldEmoji = existingReaction.getEmoji();
                if (oldEmoji.equals("UPVOTE")) post.setUpvotesCount(Math.max(0, post.getUpvotesCount() - 1));
                if (oldEmoji.equals("DOWNVOTE")) post.setDownvotesCount(Math.max(0, post.getDownvotesCount() - 1));

                existingReaction.setEmoji(emoji);
                reactionRepository.save(existingReaction);

                if (emoji.equals("UPVOTE")) post.setUpvotesCount(post.getUpvotesCount() + 1);
                if (emoji.equals("DOWNVOTE")) post.setDownvotesCount(post.getDownvotesCount() + 1);
            }
        } else {
            // Ch?a c? reaction, t?o m?i
            Reaction reaction = Reaction.builder()
                    .user(user)
                    .post(post)
                    .emoji(emoji)
                    .build();
            reactionRepository.save(reaction);

            if (emoji.equals("UPVOTE")) post.setUpvotesCount(post.getUpvotesCount() + 1);
            if (emoji.equals("DOWNVOTE")) post.setDownvotesCount(post.getDownvotesCount() + 1);
        }
        
        postRepository.save(post);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getReactionSummary(String postId) {
        List<Object[]> results = reactionRepository.countByPostIdGroupByEmoji(postId);
        Map<String, Long> summary = new HashMap<>();
        for (Object[] result : results) {
            String emoji = (String) result[0];
            Long count = (Long) result[1];
            summary.put(emoji, count);
        }
        return summary;
    }
}
