package com.vietblog.domain.service;

import com.vietblog.domain.entity.Category;
import com.vietblog.domain.exception.BusinessRuleException;
import com.vietblog.domain.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class CategoryDomainService {

    /**
     * Tạo Slug từ tên danh mục. Ví dụ: "Lập trình Java" -> "lap-trinh-java"
     */
    public void generateSlug(Category category) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new BusinessRuleException(ErrorCode.CATEGORY_NAME_EMPTY);
        }

        String temp = Normalizer.normalize(category.getName(), Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String slug = pattern.matcher(temp).replaceAll("")
                .toLowerCase()
                .replaceAll("đ", "d")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();

        if (slug.startsWith("-")) slug = slug.substring(1);
        if (slug.endsWith("-")) slug = slug.substring(0, slug.length() - 1);
        if (slug.isBlank()) {
            slug = "danh-muc-" + UUID.randomUUID().toString().substring(0, 6);
        }

        category.setSlug(slug);
    }
}
