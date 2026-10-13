package com.vietblog.application.service;

import org.springframework.security.access.prepost.PreAuthorize;
import com.vietblog.domain.exception.BusinessRuleException;
import com.vietblog.domain.exception.ErrorCode;
import com.vietblog.infrastructure.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tags;
    public record TagResponse(String name, long postsCount) { }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public int rename(String name, String replacement) {
        if (name == null || name.isBlank() || replacement == null || replacement.isBlank() || replacement.trim().length() > 64)
            throw new BusinessRuleException(ErrorCode.INVALID_INPUT);
        var posts = tags.findTagged(name);
        for (var post : posts) {
            var updated = post.getTags().stream().map(tag -> tag.equals(name) ? replacement.trim() : tag).distinct().toList();
            post.getTags().clear();
            post.getTags().addAll(updated);
        }
        return posts.size();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public int delete(String name) {
        var posts = tags.findTagged(name);
        posts.forEach(post -> post.getTags().removeIf(name::equals));
        return posts.size();
    }

    @Transactional(readOnly = true)
    public List<TagResponse> list(String search, int limit) {
        if (limit < 1 || limit > 100 || search.length() > 100) throw new BusinessRuleException(ErrorCode.INVALID_INPUT);
        return tags.findPopular(search.trim(), PageRequest.of(0, limit)).stream()
                .map(tag -> new TagResponse(tag.getName(), tag.getPostsCount())).toList();
    }
}
