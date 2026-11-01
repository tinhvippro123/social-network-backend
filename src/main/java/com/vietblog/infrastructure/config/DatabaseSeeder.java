package com.vietblog.infrastructure.config;

import com.vietblog.domain.entity.Category;
import com.vietblog.infrastructure.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Khởi tạo danh mục nền tảng cốt lõi trước khi bộ nạp dữ liệu mở rộng chạy.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(String... args) {
        ensureStandardCategories();
    }

    private void ensureStandardCategories() {
        record CategorySpec(String name, String slug, String description, String icon) {}

        List<CategorySpec> specs = List.of(
                new CategorySpec("Công nghệ", "cong-nghe", "Kiến trúc phần mềm, lập trình hiện đại, trí tuệ nhân tạo và hạ tầng đám mây", "cpu"),
                new CategorySpec("Du lịch", "du-lich", "Khám phá danh lam thắng cảnh, cung đường trải nghiệm và văn hóa vùng miền Việt Nam", "map"),
                new CategorySpec("Ẩm thực", "am-thuc", "Tinh hoa món ngon bản địa, văn hóa cà phê và góc nhìn ẩm thực ba miền", "utensils"),
                new CategorySpec("Đời sống", "doi-song", "Kỹ năng học tập, cân bằng công việc, công thái học và phát triển bản thân", "coffee"),
                new CategorySpec("Sự kiện", "su-kien", "Hội thảo công nghệ, ngày hội học thuật sinh viên và workshop chuyên đề", "calendar")
        );

        for (CategorySpec spec : specs) {
            categoryRepository.findBySlug(spec.slug()).ifPresentOrElse(existing -> {
                existing.setName(spec.name());
                existing.setDescription(spec.description());
                existing.setIcon(spec.icon());
                categoryRepository.save(existing);
            }, () -> categoryRepository.save(Category.builder()
                    .name(spec.name())
                    .slug(spec.slug())
                    .description(spec.description())
                    .icon(spec.icon())
                    .build()));
        }
        log.info("Ensured {} standard platform categories.", specs.size());
    }
}
