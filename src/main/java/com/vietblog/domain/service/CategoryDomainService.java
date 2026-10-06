package com.vietblog.domain.service;

import com.vietblog.domain.entity.Category;
import com.vietblog.domain.exception.BusinessRuleException;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.regex.Pattern;

import com.vietblog.domain.exception.ErrorCode;

@Service
public class CategoryDomainService {

    /**
     * Tạo Slug từ tên danh mục. Ví dụ: "Lập trình Java" -> "lap-trinh-java"
     */
    public void generateSlug(Category category) {
        if (category.getName() == null || category.getName().isEmpty()) {
            throw new BusinessRuleException(ErrorCode.INVALID_INPUT);
        }
        
        String temp = Normalizer.normalize(category.getName(), Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String slug = pattern.matcher(temp).replaceAll("")
                .toLowerCase()
                .replaceAll("đ", "d")
                .replaceAll("[^a-z0-9\\s-]", "") // Xóa ký tự đặc biệt
                .replaceAll("\\s+", "-") // Thay khoảng trắng bằng dấu gạch ngang
                .replaceAll("-+", "-")   // Xóa các dấu gạch ngang liên tiếp
                .trim();
                
        // Cắt bỏ dấu gạch ngang ở đầu và cuối nếu có
        if (slug.startsWith("-")) slug = slug.substring(1);
        if (slug.endsWith("-")) slug = slug.substring(0, slug.length() - 1);
        
        category.setSlug(slug);
    }
}
