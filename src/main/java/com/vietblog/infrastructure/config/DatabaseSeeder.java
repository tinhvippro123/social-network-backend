package com.vietblog.infrastructure.config;

import com.vietblog.domain.entity.Category;
import com.vietblog.domain.entity.Post;
import com.vietblog.domain.entity.User;
import com.vietblog.infrastructure.repository.CategoryRepository;
import com.vietblog.infrastructure.repository.PostRepository;
import com.vietblog.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking database for seed data...");
        
        if (categoryRepository.count() == 0) {
            log.info("Seeding Categories...");
            seedCategories();
        }
        
        if (userRepository.count() == 0) {
            log.info("Seeding Users...");
            seedUsers();
        }

        if (postRepository.count() == 0) {
            log.info("Seeding Posts...");
            seedPosts();
        }
    }

    private void seedCategories() {
        List<Category> categories = Arrays.asList(
            Category.builder().name("Công nghệ").slug("cong-nghe").description("Tin tức, xu hướng công nghệ mới nhất").icon("cpu").build(),
            Category.builder().name("Đời sống").slug("doi-song").description("Chia sẻ kinh nghiệm sống, mẹo vặt").icon("coffee").build(),
            Category.builder().name("Sự kiện").slug("su-kien").description("Các sự kiện nổi bật, workshop").icon("calendar").build(),
            Category.builder().name("Du lịch").slug("du-lich").description("Review địa điểm du lịch, cẩm nang").icon("map").build(),
            Category.builder().name("Ẩm thực").slug("am-thuc").description("Món ngon mỗi ngày, review quán ăn").icon("coffee").build()
        );
        categoryRepository.saveAll(categories);
    }

    private void seedUsers() {
        User admin = User.builder()
            .name("Admin System")
            .email("admin@vietblog.com")
            .password(passwordEncoder.encode("12345678"))
            .role(User.Role.ADMIN)
            .isActive(true)
            .build();
        userRepository.save(admin);
    }

    private void seedPosts() {
        User admin = userRepository.findByEmail("admin@vietblog.com").orElse(null);
        Category tech = categoryRepository.findBySlug("cong-nghe").orElse(null);
        Category life = categoryRepository.findBySlug("doi-song").orElse(null);

        if (admin != null && tech != null && life != null) {
            List<Post> posts = Arrays.asList(
                Post.builder()
                    .title("Chào mừng đến với VietBlog!")
                    .excerpt("Đây là bài viết đầu tiên trên nền tảng VietBlog. Nền tảng chia sẻ kiến thức cộng đồng số 1.")
                    .content("<p>Chào mừng bạn đến với <strong>VietBlog</strong>! Chúng tôi rất vui khi có bạn tham gia cộng đồng này.</p>")
                    .author(admin)
                    .category(tech)
                    .status(Post.PostStatus.PUBLISHED)
                    .tags(Arrays.asList("welcome", "vietblog", "tech"))
                    .build(),
                Post.builder()
                    .title("Top 10 kỹ năng sinh tồn cho Lập trình viên năm 2026")
                    .excerpt("AI đang phát triển mạnh mẽ, lập trình viên cần trang bị những gì để không bị đào thải?")
                    .content("<h2>1. Kỹ năng Prompt Engineering</h2><p>Điều này quan trọng hơn bạn nghĩ...</p>")
                    .author(admin)
                    .category(life)
                    .status(Post.PostStatus.PUBLISHED)
                    .tags(Arrays.asList("coding", "ai", "career"))
                    .build()
            );
            postRepository.saveAll(posts);
        }
    }
}
