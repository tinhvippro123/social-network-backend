package com.vietblog.application.service;

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

    @Transactional(readOnly = true)
    public List<TagResponse> list(String search, int limit) {
        if (limit < 1 || limit > 100 || search.length() > 100) throw new BusinessRuleException(ErrorCode.INVALID_INPUT);
        return tags.findPopular(search.trim(), PageRequest.of(0, limit)).stream()
                .map(tag -> new TagResponse(tag.getName(), tag.getPostsCount())).toList();
    }
}
