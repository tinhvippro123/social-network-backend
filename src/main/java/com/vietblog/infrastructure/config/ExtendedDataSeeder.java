package com.vietblog.infrastructure.config;

import com.vietblog.domain.entity.*;
import com.vietblog.infrastructure.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.*;

@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class ExtendedDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupPostRepository groupPostRepository;
    private final GroupPostCommentRepository groupPostCommentRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking and seeding extended application data...");

        // 1. Ensure primary Admin accounts exist with known credentials
        ensureAdminAccounts();

        // 2. Seed more realistic Vietnamese users if needed
        seedMoreUsers();

        // 3. Seed active community groups with discussions and members
        seedCommunityGroups();

        // 4. Seed more rich blog posts with GPS coordinates and comments
        seedMorePostsAndComments();

        // 5. Seed multi-level branched comments (Level 1 -> multiple Level 2 -> multiple Level 3)
        seedRichNestedComments();

        // 6. Seed real chat conversations and messages
        seedChatConversations();

        // 7. Synchronize counters
        syncCounters();

        log.info("Extended data seeding complete! Users: {}, Posts: {}, Groups: {}, Conversations: {}",
                userRepository.count(), postRepository.count(), groupRepository.count(), conversationRepository.count());
    }

    private void ensureAdminAccounts() {
        String defaultPassword = passwordEncoder.encode("Password123!");

        // admin@vietblog.com
        userRepository.findByEmail("admin@vietblog.com").ifPresentOrElse(admin -> {
            admin.setRole(User.Role.ADMIN);
            admin.setPassword(defaultPassword);
            admin.setActive(true);
            userRepository.save(admin);
        }, () -> {
            User admin = User.builder()
                    .name("Quản trị viên Hệ thống")
                    .email("admin@vietblog.com")
                    .password(defaultPassword)
                    .avatar("https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80")
                    .bio("Tài khoản Quản trị viên cấp cao của VietBlog. Quản trị toàn diện người dùng, nội dung, nhóm và kiểm duyệt.")
                    .role(User.Role.ADMIN)
                    .isActive(true)
                    .build();
            userRepository.save(admin);
            log.info("Created primary admin: admin@vietblog.com");
        });

        // Ensure key developer / student accounts have ADMIN role
        List<String> adminEmails = List.of(
                "nguyenvanan@gmail.com",
                "lethanhtinh2004@gmail.com",
                "1250080199@sv.hcmunre.edu.vn"
        );
        for (String email : adminEmails) {
            userRepository.findByEmail(email).ifPresent(u -> {
                if (u.getRole() != User.Role.ADMIN) {
                    u.setRole(User.Role.ADMIN);
                    userRepository.save(u);
                    log.info("Promoted {} to ADMIN", email);
                }
            });
        }
    }

    private void seedMoreUsers() {
        if (userRepository.count() >= 35) return;

        String encodedPwd = passwordEncoder.encode("Password123!");
        List<User> newUsers = List.of(
                User.builder().name("Trần Đình Trọng").email("trandinhtrong@gmail.com").password(encodedPwd)
                        .avatar("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=400&q=80")
                        .bio("DevOps Engineer & Cloud Architect tại FPT Software. Đam mê Kubernetes, Docker và CI/CD.")
                        .role(User.Role.USER).isActive(true).build(),

                User.builder().name("Nguyễn Thu Trang").email("nguyenthutrang@gmail.com").password(encodedPwd)
                        .avatar("https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=400&q=80")
                        .bio("Product Designer & UI/UX Specialist. Từng tham gia thiết kế nhiều ứng dụng EdTech & FinTech.")
                        .role(User.Role.USER).isActive(true).build(),

                User.builder().name("Lê Hồng Nhung").email("lehongnhung@gmail.com").password(encodedPwd)
                        .avatar("https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=400&q=80")
                        .bio("Content Creator & Travel Blogger. Yêu thích khám phá các góc phố ẩm thực và văn hóa Việt Nam.")
                        .role(User.Role.USER).isActive(true).build(),

                User.builder().name("Vũ Quốc Tuấn").email("vuquoctuan@gmail.com").password(encodedPwd)
                        .avatar("https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=400&q=80")
                        .bio("Sinh viên năm cuối CNTT - Đại học Tài nguyên và Môi trường TP.HCM (HCMUNRE). Đang nghiên cứu AI & NLP.")
                        .role(User.Role.USER).isActive(true).build(),

                User.builder().name("Phạm Minh Đức").email("phamminhduc@gmail.com").password(encodedPwd)
                        .avatar("https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?auto=format&fit=crop&w=400&q=80")
                        .bio("Senior Backend Developer (Spring Boot, Go, PostgreSQL, Redis). Thích chia sẻ kiến thức Clean Architecture.")
                        .role(User.Role.MODERATOR).isActive(true).build(),

                User.builder().name("Đặng Thị Mỹ Linh").email("dangthimylinh@gmail.com").password(encodedPwd)
                        .avatar("https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=400&q=80")
                        .bio("Frontend Engineer (Vue 3, TypeScript, TailwindCSS). Đam mê Micro-interactions và Web Performance.")
                        .role(User.Role.USER).isActive(true).build(),

                User.builder().name("Đỗ Anh Khoa").email("doanhkhoa@gmail.com").password(encodedPwd)
                        .avatar("https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&w=400&q=80")
                        .bio("Data Scientist & Machine Learning Practitioner. Nghiên cứu NLP tiếng Việt và RecSys.")
                        .role(User.Role.USER).isActive(true).build(),

                User.builder().name("Bùi Hoàng Yến").email("buihoangyen@gmail.com").password(encodedPwd)
                        .avatar("https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=400&q=80")
                        .bio("Nhiếp ảnh gia nghiệp dư & Food Reviewer tại Sài Gòn. Thích review quán cà phê phong cách tối giản.")
                        .role(User.Role.USER).isActive(true).build(),

                User.builder().name("Ngô Thành Long").email("ngothanhlong@gmail.com").password(encodedPwd)
                        .avatar("https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=400&q=80")
                        .bio("Security Researcher & Ethical Hacker. Kiểm thử xâm nhập bảo mật cho các hệ thống Web Application.")
                        .role(User.Role.MODERATOR).isActive(true).build(),

                User.builder().name("Hoàng Diệu Anh").email("hoangdieuanh@gmail.com").password(encodedPwd)
                        .avatar("https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80")
                        .bio("HR Tech & Tech Recruiter. Kết nối các lập trình viên tài năng với các cơ hội nghề nghiệp tại Việt Nam.")
                        .role(User.Role.USER).isActive(true).build()
        );

        for (User u : newUsers) {
            if (userRepository.findByEmail(u.getEmail()).isEmpty()) {
                userRepository.save(u);
            }
        }
        log.info("Seeded additional realistic users.");
    }

    private void seedCommunityGroups() {
        if (groupRepository.count() >= 8) return;

        User owner = userRepository.findByEmail("admin@vietblog.com")
                .or(() -> userRepository.findByEmail("nguyenvanan@gmail.com"))
                .orElse(null);
        if (owner == null) return;

        List<User> allUsers = userRepository.findAll();

        record GroupSeed(String name, String desc, String avatar, String cover) {}
        List<GroupSeed> seeds = List.of(
                new GroupSeed("Cộng đồng Lập trình viên Fullstack Việt Nam",
                        "Nơi giao lưu, thảo luận kinh nghiệm lập trình từ Frontend (Vue, React) tới Backend (Java Spring Boot, Node.js, Go) và cơ sở dữ liệu.",
                        "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?auto=format&fit=crop&w=200&q=80",
                        "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=1200&q=80"),

                new GroupSeed("Spring Boot & Java Enterprise Việt Nam",
                        "Chuyên sâu về kiến trúc ứng dụng Spring Boot, Spring Security, Hibernate JPA, Microservices, Clean Architecture và tối ưu hiệu năng Enterprise.",
                        "https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=200&q=80",
                        "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?auto=format&fit=crop&w=1200&q=80"),

                new GroupSeed("Frontend Master: Vue.js, React & Tailwind",
                        "Cập nhật những xu hướng công nghệ Frontend mới nhất: Vue 3 Composition API, Vite, Pinia, TailwindCSS, TypeScript và tối ưu UX mượt mà.",
                        "https://images.unsplash.com/photo-1633356122544-f134324a6cee?auto=format&fit=crop&w=200&q=80",
                        "https://images.unsplash.com/photo-1507238691740-187a5b1d37b8?auto=format&fit=crop&w=1200&q=80"),

                new GroupSeed("Cộng đồng AI & Machine Learning Việt Nam",
                        "Nghiên cứu và ứng dụng Trí tuệ nhân tạo, Xử lý ngôn ngữ tự nhiên (NLP tiếng Việt), Tóm tắt văn bản TextRank, Word Embeddings và Generative AI.",
                        "https://images.unsplash.com/photo-1677442136019-21780ecad995?auto=format&fit=crop&w=200&q=80",
                        "https://images.unsplash.com/photo-1620712943543-bcc4688e7485?auto=format&fit=crop&w=1200&q=80"),

                new GroupSeed("CLB Công nghệ Thông tin HCMUNRE",
                        "Nhóm chính thức dành cho sinh viên và cựu sinh viên Khoa Công nghệ Thông tin - Trường Đại học Tài nguyên và Môi trường TP.HCM giao lưu, học tập.",
                        "https://images.unsplash.com/photo-1523240795612-9a054b0db644?auto=format&fit=crop&w=200&q=80",
                        "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?auto=format&fit=crop&w=1200&q=80"),

                new GroupSeed("DevOps, Cloud & System Architecture",
                        "Chia sẻ kinh nghiệm xây dựng hệ thống phân tán, containerization với Docker, Kubernetes, AWS, Nginx reverse proxy, CI/CD và Monitoring.",
                        "https://images.unsplash.com/photo-1607799279861-4dd421887fb3?auto=format&fit=crop&w=200&q=80",
                        "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&w=1200&q=80"),

                new GroupSeed("Góc Cà phê & Chia sẻ Đời sống Lập trình",
                        "Không gian thư giãn để các developer tâm sự chuyện nghề, văn hóa công ty, cân bằng giữa công việc và cuộc sống (Work-Life Balance).",
                        "https://images.unsplash.com/photo-1511920170033-f8396924c348?auto=format&fit=crop&w=200&q=80",
                        "https://images.unsplash.com/photo-1442512595331-e89e73853f31?auto=format&fit=crop&w=1200&q=80"),

                new GroupSeed("UI/UX Design & Product Mindset",
                        "Dành cho các UI/UX Designer và Product Manager chia sẻ case study thực tế, phương pháp Design System, Wireframing và phỏng vấn người dùng.",
                        "https://images.unsplash.com/photo-1581291518857-4e27b48ff24e?auto=format&fit=crop&w=200&q=80",
                        "https://images.unsplash.com/photo-1531403009284-440f080d1e12?auto=format&fit=crop&w=1200&q=80")
        );

        for (GroupSeed seed : seeds) {
            Group group = Group.builder()
                    .name(seed.name())
                    .description(seed.desc())
                    .avatar(seed.avatar())
                    .coverImage(seed.cover())
                    .publicGroup(true)
                    .owner(owner)
                    .createdAt(LocalDateTime.now().minusDays(new Random().nextInt(30) + 5))
                    .build();
            group = groupRepository.save(group);

            // Add owner and members
            groupMemberRepository.save(GroupMember.builder()
                    .group(group).user(owner).role(GroupMember.Role.OWNER).build());

            int memberCount = Math.min(allUsers.size(), 8);
            for (int i = 0; i < memberCount; i++) {
                User member = allUsers.get(i);
                if (!member.getId().equals(owner.getId())) {
                    try {
                        groupMemberRepository.save(GroupMember.builder()
                                .group(group).user(member).role(GroupMember.Role.MEMBER).build());
                    } catch (Exception ignored) {}
                }
            }

            // Seed 2-3 group discussion posts
            GroupPost gp1 = groupPostRepository.save(GroupPost.builder()
                    .group(group)
                    .author(owner)
                    .title("Chào mừng các thành viên mới gia nhập " + group.getName() + "!")
                    .content("<p>Chào mừng mọi người đến với nhóm! Hãy cùng nhau thảo luận, đặt câu hỏi và chia sẻ những bài học thực tế nhé.</p>")
                    .excerpt("Lời chào và quy tắc sinh hoạt trong nhóm cộng đồng...")
                    .createdAt(LocalDateTime.now().minusDays(3))
                    .build());

            groupPostCommentRepository.save(GroupPostComment.builder()
                    .post(gp1)
                    .author(allUsers.get(1))
                    .content("Chào cả nhà ạ! Rất vui được tham gia nhóm cùng các anh chị em.")
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .build());

            GroupPost gp2 = groupPostRepository.save(GroupPost.builder()
                    .group(group)
                    .author(allUsers.get(2))
                    .title("Thảo luận: Những kinh nghiệm hữu ích khi thực hiện Đồ án Chuyên ngành")
                    .content("<p>Mọi người khi làm đồ án tốt nghiệp hoặc chuyên ngành thường mất nhiều thời gian ở khâu nào nhất? Thiết kế database, viết API hay tối ưu UI?</p>")
                    .excerpt("Giao lưu chia sẻ kinh nghiệm vượt qua giai đoạn đồ án áp lực...")
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .build());

            groupPostCommentRepository.save(GroupPostComment.builder()
                    .post(gp2)
                    .author(owner)
                    .content("Theo mình khâu chốt Use-case và làm rõ yêu cầu nghiệp vụ với giảng viên là quan trọng nhất!")
                    .createdAt(LocalDateTime.now().minusHours(5))
                    .build());
        }
        log.info("Seeded community groups with posts and comments.");
    }

    private void seedMorePostsAndComments() {
        if (postRepository.count() >= 35) return;

        Category tech = categoryRepository.findBySlug("cong-nghe").orElse(null);
        Category life = categoryRepository.findBySlug("doi-song").orElse(null);
        Category event = categoryRepository.findBySlug("su-kien").orElse(null);
        Category travel = categoryRepository.findBySlug("du-lich").orElse(null);
        Category food = categoryRepository.findBySlug("am-thuc").orElse(null);

        if (tech == null || life == null) return;

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) return;

        User author1 = users.get(0);
        User author2 = users.size() > 1 ? users.get(1) : author1;
        User author3 = users.size() > 2 ? users.get(2) : author1;
        User author4 = users.size() > 3 ? users.get(3) : author1;

        record PostSeed(String title, String excerpt, String content, String cover, Category cat, User author,
                        List<String> tags, Double lat, Double lng, String address) {}

        List<PostSeed> postSeeds = List.of(
                new PostSeed(
                        "Giải thuật Lexicon-based kết hợp Word Embeddings trong Phân tích cảm xúc tiếng Việt",
                        "Khám phá phương pháp xây dựng từ điển cảm xúc tiếng Việt, xử lý từ phủ định và tính toán điểm số bình luận trong VietBlog.",
                        "<h2>1. Đặt vấn đề trong phân tích cảm xúc tiếng Việt</h2>" +
                                "<p>Tiếng Việt có cấu trúc ngữ pháp phong phú với nhiều từ ghép, từ láy và cách diễn đạt cảm xúc gián tiếp. Để xây dựng chức năng kiểm duyệt bình luận tự động, đồ án áp dụng giải thuật Lexicon-based kết hợp quy tắc xử lý từ phủ định (negation handling).</p>" +
                                "<h2>2. Xử lý từ phủ định: 'không tốt', 'chẳng hay'</h2>" +
                                "<p>Một từ tích cực như <em>'tốt'</em> hoặc <em>'hay'</em> khi đi sau các từ phủ định như <em>'không', 'chẳng', 'chưa'</em> sẽ được đảo ngược trọng số cảm xúc sang tiêu cực. Nhờ đó, mô hình giảm thiểu đáng kể tỷ lệ nhận diện sai (False Positives).</p>" +
                                "<h2>3. Đánh giá kết quả trên tập dữ liệu VietBlog</h2>" +
                                "<p>Thuật toán đạt độ chính xác hơn 85% trên tập dữ liệu bình luận thử nghiệm và phản hồi kết quả trong thời gian dưới 10ms.</p>",
                        "https://images.unsplash.com/photo-1555949963-ff9fe0c870eb?auto=format&fit=crop&w=1200&q=80",
                        tech, author1, List.of("ai", "nlp", "sentiment-analysis", "vietnamese"),
                        10.8016, 106.6575, "Trường Đại học Tài nguyên và Môi trường TP.HCM (HCMUNRE), Tân Bình"
                ),

                new PostSeed(
                        "Tổng quan thuật toán TextRank: Tóm tắt bài viết tự động không cần huấn luyện mô hình nặng",
                        "Ứng dụng mô hình đồ thị đồ thị văn bản và PageRank để trích xuất những câu đắt giá nhất trong bài viết dài trên VietBlog.",
                        "<h2>1. Nguyên lý hoạt động của TextRank</h2>" +
                                "<p>TextRank là giải thuật tóm tắt văn bản trích xuất (extractive summarization) dựa trên mô hình đồ thị. Mỗi câu trong bài viết được biểu diễn như một đỉnh (node), và trọng số cạnh giữa hai đỉnh thể hiện mức độ tương đồng từ vựng (Jaccard / Cosine similarity).</p>" +
                                "<h2>2. Ưu điểm vượt trội trong hệ thống Web</h2>" +
                                "<ul><li>Hoạt động hoàn toàn cục bộ trên máy chủ Java Spring Boot mà không cần gọi API bên thứ ba có phí.</li><li>Tốc độ xử lý cực nhanh: tóm tắt bài viết 2000 từ chỉ trong 15 miligiây.</li><li>Đảm bảo giữ nguyên câu gốc của tác giả mà không bị ảo giác (hallucination).</li></ul>",
                        "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1200&q=80",
                        tech, author2, List.of("ai", "textrank", "nlp", "summarization"),
                        10.7719, 106.7044, "Tòa nhà Bitexco Financial Tower, Quận 1, TP.HCM"
                ),

                new PostSeed(
                        "Kinh nghiệm bảo vệ Đồ án Chuyên ngành đạt điểm tối đa trước Hội đồng",
                        "Những bí quyết thuyết trình, chuẩn bị slide, kịch bản demo hệ thống và cách trả lời câu hỏi phản biện của giảng viên.",
                        "<h2>1. Chuẩn bị kịch bản Demo mượt mà</h2>" +
                                "<p>Đừng bao giờ để thời gian chết trong lúc demo. Hãy mở sẵn các tab trình duyệt, đăng nhập sẵn tài khoản Admin và tài khoản User thông thường trên hai cửa sổ ẩn danh khác nhau.</p>" +
                                "<h2>2. Nhấn mạnh vào đóng góp kỹ thuật cốt lõi</h2>" +
                                "<p>Tập trung làm nổi bật các chức năng AI và thuật toán tự cài đặt (TextRank, Sentiment Analysis, Tìm kiếm ngữ nghĩa) thay vì chỉ nói về các chức năng CRUD cơ bản.</p>",
                        "https://images.unsplash.com/photo-1523240795612-9a054b0db644?auto=format&fit=crop&w=1200&q=80",
                        life, author3, List.of("hcmunre", "do-an", "sinh-vien", "kinh-nghiem"),
                        10.8016, 106.6575, "Hội trường A - HCMUNRE, 236 Lê Trọng Tấn, Tây Thạnh, Tân Phú, TP.HCM"
                ),

                new PostSeed(
                        "Hành trình một ngày khám phá Phố cổ Hà Nội: Cà phê trứng và Hồ Gươm",
                        "Cẩm nang dạo quanh 36 phố phường, thưởng thức tô phở bò Bát Đàn nóng hổi và ngắm hoàng hôn bên Hồ Hoàn Kiếm.",
                        "<h2>1. Buổi sáng se lạnh bên Hồ Gươm</h2>" +
                                "<p>Không khí Hà Nội buổi sớm luôn mang đến một cảm giác rất đỗi thanh bình. Ngồi nhâm nhi tách cà phê Giảng thơm nồng mùi trứng béo ngậy giữa lòng phố cổ là trải nghiệm không thể bỏ qua.</p>" +
                                "<h2>2. Ẩm thực phố cổ nức tiếng</h2>" +
                                "<p>Từ bún chả Hàng Quạt, chả cá Lã Vọng cho đến nộm bò khô bờ hồ, mỗi món ăn đều gắn liền với ký ức văn hóa hàng trăm năm của người Tràng An.</p>",
                        "https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=1200&q=80",
                        travel != null ? travel : life, author4, List.of("hanoi", "du-lich", "ho-guom", "am-thuc"),
                        21.0285, 105.8542, "Hồ Hoàn Kiếm, Quận Hoàn Kiếm, Hà Nội"
                ),

                new PostSeed(
                        "Check-in Cầu Rồng và Biển Mỹ Khê: Đà Nẵng - Thành phố đáng sống nhất Việt Nam",
                        "Review lịch trình 3 ngày 2 đêm tại Đà Nẵng, ngắm cầu Rồng phun lửa vào tối cuối tuần và tận hưởng bãi biển xanh ngắt.",
                        "<h2>1. Bãi biển Mỹ Khê - Top bãi biển đẹp nhất hành tinh</h2>" +
                                "<p>Bờ cát trắng mịn trải dài cùng làn nước trong xanh khiến Mỹ Khê luôn là điểm đến hàng đầu của du khách trong và ngoài nước.</p>" +
                                "<h2>2. Cầu Rồng phun lửa và phun nước</h2>" +
                                "<p>Đúng 21h00 tối thứ Bảy và Chủ Nhật hàng tuần, hàng ngàn người dân và khách du lịch tụ họp hai bên bờ sông Hàn để chiêm ngưỡng màn trình diễn ngoạn mục.</p>",
                        "https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?auto=format&fit=crop&w=1200&q=80",
                        travel != null ? travel : life, author1, List.of("da-nang", "cau-rong", "my-khe", "du-lich"),
                        16.0611, 108.2274, "Cầu Rồng, Phước Ninh, Hải Châu, Đà Nẵng"
                ),

                new PostSeed(
                        "Top 5 quán cà phê phong cách Acoustic cực chill tại Sài Gòn cho cuối tuần",
                        "Những góc nhỏ bình yên giữa lòng thành phố náo nhiệt để bạn thưởng thức âm nhạc mộc mạc và nhâm nhi tách trà thơm.",
                        "<h2>1. Không gian mộc mạc và âm nhạc chân thực</h2>" +
                                "<p>Sau những giờ làm việc căng thẳng trước màn hình máy tính, được ngồi lắng nghe những giai điệu guitar mộc mạc là cách nạp lại năng lượng tuyệt vời nhất.</p>" +
                                "<h2>2. Những gợi ý đáng thử</h2>" +
                                "<ul><li>Say Cafe - Bình Thạnh</li><li>Yoko Cafe - Quận 3</li><li>Acoustic Bar - Ngô Thời Nhiệm</li></ul>",
                        "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=1200&q=80",
                        life, author2, List.of("saigon", "ca-phe", "acoustic", "lifestyle"),
                        10.7950, 106.7218, "Landmark 81, Vinhomes Central Park, Bình Thạnh, TP.HCM"
                ),

                new PostSeed(
                        "Review ẩm thực miền Tây: Về Bến Ninh Kiều ăn bánh cống và lẩu mắm Cần Thơ",
                        "Trải nghiệm nét ẩm thực độc đáo của vùng sông nước miền Tây Nam Bộ với đĩa bánh cống giòn rụm và nồi lẩu mắm đậm đà.",
                        "<h2>1. Nét đặc trưng của lẩu mắm Cần Thơ</h2>" +
                                "<p>Nồi lẩu mắm sôi sùng sục tỏa hương thơm nức mũi, ăn kèm với hơn 10 loại rau đồng nội như bông điên điển, rau nhút, so đũa và bắp chuối thái mỏng.</p>" +
                                "<h2>2. Dạo bến Ninh Kiều về đêm</h2>" +
                                "<p>Ngắm du thuyền lộng lẫy ánh đèn trên dòng sông Hậu hiền hòa là trải nghiệm khó quên của bất kỳ ai từng ghé thăm Tây Đô.</p>",
                        "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1200&q=80",
                        food != null ? food : life, author3, List.of("can-tho", "am-thuc", "mien-tay", "mon-ngon"),
                        10.0335, 105.7876, "Bến Ninh Kiều, Tân An, Ninh Kiều, Cần Thơ"
                ),

                new PostSeed(
                        "Workshop: Ứng dụng Cloud Native và Docker trong triển khai hệ thống Web",
                        "Sự kiện công nghệ thường niên dành cho sinh viên IT thực hành đóng gói Docker image, cấu hình Docker Compose và CI/CD GitHub Actions.",
                        "<h2>1. Nội dung workshop thực hành</h2>" +
                                "<p>Các diễn giả sẽ hướng dẫn trực tiếp quy trình Containerize một ứng dụng Fullstack gồm Vue 3 Frontend, Spring Boot Backend, PostgreSQL và Redis Cache.</p>" +
                                "<h2>2. Thời gian và địa điểm</h2>" +
                                "<p>Sự kiện diễn ra vào sáng thứ Bảy tuần tới tại Hội trường Công nghệ thông tin.</p>",
                        "https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=1200&q=80",
                        event != null ? event : tech, author4, List.of("workshop", "docker", "cloud-native", "su-kien"),
                        10.8016, 106.6575, "Khoa CNTT - HCMUNRE, TP.HCM"
                )
        );

        for (PostSeed s : postSeeds) {
            Post post = Post.builder()
                    .title(s.title())
                    .excerpt(s.excerpt())
                    .content(s.content())
                    .coverImage(s.cover())
                    .category(s.cat())
                    .author(s.author())
                    .tags(s.tags())
                    .status(Post.PostStatus.PUBLISHED)
                    .latitude(s.lat())
                    .longitude(s.lng())
                    .locationAddress(s.address())
                    .viewsCount(new Random().nextInt(500) + 50)
                    .upvotesCount(new Random().nextInt(50) + 10)
                    .downvotesCount(new Random().nextInt(3))
                    .createdAt(LocalDateTime.now().minusDays(new Random().nextInt(14) + 1))
                    .build();
            post = postRepository.save(post);

            // Add 3-5 realistic comments for sentiment analysis variety
            List<String> positiveComments = List.of(
                    "Bài viết chia sẻ rất chi tiết và dễ hiểu, cảm ơn tác giả nhiều!",
                    "Kiến thức rất bổ ích, mình đã áp dụng thành công vào đồ án của mình rồi.",
                    "Tuyệt vời quá, giao diện và cách trình bày rất trực quan và chuyên nghiệp!",
                    "Rất thích các ví dụ minh họa của bài viết, tác giả viết rất có tâm."
            );
            List<String> neutralComments = List.of(
                    "Cho mình hỏi phiên bản Spring Boot hiện tại đang dùng là bản mấy vậy bạn?",
                    "Bài viết có repo mã nguồn mẫu trên GitHub không bạn ơi?",
                    "Mình đang tìm hiểu thêm về cách tích hợp Redis Cache vào Use Case này."
            );
            List<String> negativeComments = List.of(
                    "Đoạn giải thích về thuật toán còn hơi sơ sài, mong tác giả bổ sung thêm code mẫu.",
                    "Nội dung này mình thấy chưa được cập nhật theo cú pháp mới nhất của phiên bản 2026."
            );

            // Save positive comment
            commentRepository.save(Comment.builder()
                    .post(post).author(users.get(new Random().nextInt(users.size())))
                    .content(positiveComments.get(new Random().nextInt(positiveComments.size())))
                    .upvotes(4).createdAt(LocalDateTime.now().minusDays(1)).build());

            // Save neutral comment
            commentRepository.save(Comment.builder()
                    .post(post).author(users.get(new Random().nextInt(users.size())))
                    .content(neutralComments.get(new Random().nextInt(neutralComments.size())))
                    .upvotes(2).createdAt(LocalDateTime.now().minusHours(12)).build());

            // Save feedback comment
            if (new Random().nextBoolean()) {
                commentRepository.save(Comment.builder()
                        .post(post).author(users.get(new Random().nextInt(users.size())))
                        .content(negativeComments.get(new Random().nextInt(negativeComments.size())))
                        .upvotes(1).createdAt(LocalDateTime.now().minusHours(4)).build());
            }
        }
        log.info("Seeded rich posts with GPS coordinates and comments.");
    }

    private void syncCounters() {
        // Sync post counts for users
        userRepository.findAll().forEach(u -> {
            int pCount = (int) postRepository.countByAuthorId(u.getId());
            if (u.getPostsCount() != pCount) {
                u.setPostsCount(pCount);
                userRepository.save(u);
            }
        });

        // Sync comment counts for posts
        postRepository.findAll().forEach(p -> {
            int cCount = (int) commentRepository.countByPostId(p.getId());
            if (p.getCommentsCount() != cCount) {
                p.setCommentsCount(cCount);
                postRepository.save(p);
            }
        });
    }

    private void seedRichNestedComments() {
        List<Post> posts = postRepository.findAll();
        List<User> users = userRepository.findAll();
        if (posts.isEmpty() || users.size() < 5) return;

        User an = users.stream().filter(u -> "nguyenvanan@gmail.com".equals(u.getEmail())).findFirst().orElse(users.get(0));
        User admin = users.stream().filter(u -> "admin@vietblog.com".equals(u.getEmail())).findFirst().orElse(users.get(1));
        User trong = users.stream().filter(u -> "trandinhtrong@gmail.com".equals(u.getEmail())).findFirst().orElse(users.get(2));
        User trang = users.stream().filter(u -> "nguyenthutrang@gmail.com".equals(u.getEmail())).findFirst().orElse(users.get(3));
        User nam = users.stream().filter(u -> "lehoangnam@gmail.com".equals(u.getEmail())).findFirst().orElse(users.get(4));
        User duy = users.stream().filter(u -> "phamquocduy@gmail.com".equals(u.getEmail())).findFirst().orElse(users.get(5 % users.size()));

        // For the first 6 posts, inject deep multi-level comments if not already enriched
        for (int i = 0; i < Math.min(6, posts.size()); i++) {
            Post post = posts.get(i);
            long count = commentRepository.countByPostId(post.getId());
            if (count >= 8) continue; // Already has abundant comments

            // Level 1: Topic discussion
            Comment l1_1 = commentRepository.save(Comment.builder()
                    .post(post).author(duy)
                    .content("Bài viết rất hay và thực tế! Mình đặc biệt ấn tượng với cách thiết kế Domain Layer độc lập hoàn toàn với Framework, giúp dự án dễ viết Unit Test và bảo trì lâu dài.")
                    .upvotes(12).createdAt(LocalDateTime.now().minusDays(2).minusHours(3)).build());

            // Level 2 Reply A (from Nam)
            Comment l2_1 = commentRepository.save(Comment.builder()
                    .post(post).author(nam).parent(l1_1)
                    .content("Chuẩn luôn bạn! Nhiều dự án gộp chung JPA Entity vào Domain Model dẫn tới việc dính chặt vào database, sau này migrate hay nâng cấp rất vất vả.")
                    .upvotes(8).createdAt(LocalDateTime.now().minusDays(2).minusHours(1)).build());

            // Level 3 Reply A.1 (from An)
            commentRepository.save(Comment.builder()
                    .post(post).author(an).parent(l2_1)
                    .content("Đúng vậy anh Nam, việc áp dụng Dependency Inversion Principle và Port-Adapter ở đây giải quyết triệt để vấn đề coupling giữa Core Business và Database.")
                    .upvotes(5).createdAt(LocalDateTime.now().minusDays(2)).build());

            // Level 3 Reply A.2 (from Trong)
            commentRepository.save(Comment.builder()
                    .post(post).author(trong).parent(l2_1)
                    .content("Bên team mình cũng từng bị tech debt tương tự, sau khi refactor theo Clean Architecture thì coverage Unit Test tăng từ 35% lên hơn 85%.")
                    .upvotes(4).createdAt(LocalDateTime.now().minusDays(1).minusHours(18)).build());

            // Level 2 Reply B (from Trang)
            Comment l2_2 = commentRepository.save(Comment.builder()
                    .post(post).author(trang).parent(l1_1)
                    .content("Về mặt UI/UX thì mình thấy các luồng tương tác và phân cấp dữ liệu trong bài viết cũng được tổ chức rất mạch lạc, người dùng dễ tiếp cận.")
                    .upvotes(6).createdAt(LocalDateTime.now().minusDays(1).minusHours(15)).build());

            // Level 3 Reply B.1 (from Admin)
            commentRepository.save(Comment.builder()
                    .post(post).author(admin).parent(l2_2)
                    .content("Cảm ơn Trang! Đội ngũ phát triển đã đầu tư rất nhiều vào Design System và trải nghiệm người dùng responsive trên cả Mobile lẫn Desktop.")
                    .upvotes(7).createdAt(LocalDateTime.now().minusDays(1).minusHours(12)).build());

            // Level 2 Reply C (from Trong)
            commentRepository.save(Comment.builder()
                    .post(post).author(trong).parent(l1_1)
                    .content("Cho mình hỏi thêm là phần Caching với Redis thì bạn đang cấu hình TTL cho từng entity hay cache theo Page kết quả vậy?")
                    .upvotes(3).createdAt(LocalDateTime.now().minusDays(1).minusHours(8)).build());

            // Level 1: Deployment & Architecture
            Comment l1_2 = commentRepository.save(Comment.builder()
                    .post(post).author(nam)
                    .content("Tác giả có thể chia sẻ thêm về phương án deploy thực tế bằng Docker Compose hoặc Kubernetes cho hệ thống này không?")
                    .upvotes(9).createdAt(LocalDateTime.now().minusDays(1).minusHours(6)).build());

            // Level 2 Reply under l1_2 (from Trong)
            Comment l2_3 = commentRepository.save(Comment.builder()
                    .post(post).author(trong).parent(l1_2)
                    .content("Hiện tại hệ thống được đóng gói thành các Docker Container độc lập: Backend Spring Boot, Frontend Vite Nginx, PostgreSQL và Redis, có script bash tự động build & deploy chỉ với 1 lệnh.")
                    .upvotes(6).createdAt(LocalDateTime.now().minusDays(1).minusHours(4)).build());

            // Level 3 Reply under l2_3 (from An)
            commentRepository.save(Comment.builder()
                    .post(post).author(an).parent(l2_3)
                    .content("Anh Trọng có thể tham khảo thêm file docker-compose.yml mẫu trong thư mục root của dự án nhé, có sẵn cấu hình mạng và volume mount đầy đủ.")
                    .upvotes(5).createdAt(LocalDateTime.now().minusDays(1).minusHours(2)).build());

            // Level 2 Reply 2 under l1_2 (from Admin)
            commentRepository.save(Comment.builder()
                    .post(post).author(admin).parent(l1_2)
                    .content("Hệ thống cũng đã sẵn sàng scale ngang (horizontal scaling) nhờ thiết kế Stateless với JWT và Redis Token Store.")
                    .upvotes(8).createdAt(LocalDateTime.now().minusHours(2)).build());
        }
        log.info("Seeded rich multi-level nested comments across posts.");
    }

    private void seedChatConversations() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) return;

        User admin = users.stream().filter(u -> "admin@vietblog.com".equals(u.getEmail())).findFirst().orElse(null);
        User an = users.stream().filter(u -> "nguyenvanan@gmail.com".equals(u.getEmail())).findFirst().orElse(null);
        User trong = users.stream().filter(u -> "trandinhtrong@gmail.com".equals(u.getEmail())).findFirst().orElse(null);
        User trang = users.stream().filter(u -> "nguyenthutrang@gmail.com".equals(u.getEmail())).findFirst().orElse(null);
        User duc = users.stream().filter(u -> "phamminhduc@gmail.com".equals(u.getEmail())).findFirst().orElse(null);
        User linh = users.stream().filter(u -> "dangthimylinh@gmail.com".equals(u.getEmail())).findFirst().orElse(null);
        User tuan = users.stream().filter(u -> "vuquoctuan@gmail.com".equals(u.getEmail())).findFirst().orElse(null);
        User khoa = users.stream().filter(u -> "doanhkhoa@gmail.com".equals(u.getEmail())).findFirst().orElse(null);
        User nhung = users.stream().filter(u -> "lehongnhung@gmail.com".equals(u.getEmail())).findFirst().orElse(null);
        User yen = users.stream().filter(u -> "buihoangyen@gmail.com".equals(u.getEmail())).findFirst().orElse(null);
        User longUser = users.stream().filter(u -> "ngothanhlong@gmail.com".equals(u.getEmail())).findFirst().orElse(null);
        User dieuAnh = users.stream().filter(u -> "hoangdieuanh@gmail.com".equals(u.getEmail())).findFirst().orElse(null);

        if (admin == null || an == null) return;

        // ========================================================
        // 1. GROUP CHAT: Nhóm Đồ án Chuyên ngành VietBlog 2026 (12 thành viên)
        // ========================================================
        List<User> group1Members = new ArrayList<>();
        for (User u : List.of(admin, an, trong != null ? trong : an, trang != null ? trang : an,
                duc != null ? duc : an, linh != null ? linh : an, tuan != null ? tuan : an,
                khoa != null ? khoa : an, nhung != null ? nhung : an, yen != null ? yen : an,
                longUser != null ? longUser : an, dieuAnh != null ? dieuAnh : an)) {
            if (!group1Members.contains(u)) group1Members.add(u);
        }

        ensureGroupChat("Nhóm Đồ án Chuyên ngành VietBlog 2026", admin, group1Members, List.of(
                new ChatMsgData(admin, "Chào cả nhóm! Tuần này chúng ta tổng duyệt toàn bộ tính năng của đồ án VietBlog trước buổi bảo vệ nhé.", LocalDateTime.now().minusHours(8)),
                new ChatMsgData(an, "Dạ vâng thầy! Nhóm em đã chuẩn bị đầy đủ slide, demo tính năng và báo cáo chi tiết Word theo đúng chuẩn HCMUNRE ạ.", LocalDateTime.now().minusHours(7).minusMinutes(30)),
                new ChatMsgData(trang != null ? trang : an, "Giao diện và trải nghiệm người dùng responsive từ mobile đến desktop đều đã được tối ưu chỉn chu, bảng màu HSL và Dark Mode rất dịu mắt.", LocalDateTime.now().minusHours(6)),
                new ChatMsgData(trong != null ? trong : an, "Phần backend Spring Boot 4, kiến trúc Clean Architecture và caching Redis cũng đã pass 100% test, latency trung bình chỉ dưới 15ms.", LocalDateTime.now().minusHours(5).minusMinutes(20)),
                new ChatMsgData(duc != null ? duc : an, "Anh vừa review pull request phần bảo mật JWT và Google OAuth 2.0, code xử lý token và phân quyền RBAC rất chuẩn chỉ rồi nhé.", LocalDateTime.now().minusHours(4).minusMinutes(45)),
                new ChatMsgData(linh != null ? linh : an, "Phần UI tương tác thả cảm xúc bài viết và reaction tin nhắn em đã test trên cả Safari, Chrome và Edge, hiệu ứng micro-animation mượt mà lắm ạ!", LocalDateTime.now().minusHours(4)),
                new ChatMsgData(khoa != null ? khoa : an, "Giải thuật TextRank tóm tắt tự động và Sentiment Analysis phân tích cảm xúc bình luận hoạt động rất chính xác với bộ từ điển tiếng Việt mới cập nhật.", LocalDateTime.now().minusHours(3).minusMinutes(15)),
                new ChatMsgData(tuan != null ? tuan : an, "Em đang hoàn thiện thêm biểu đồ phân tích tương tác trên trang Admin Dashboard để thầy cô dễ quan sát lúc chấm điểm.", LocalDateTime.now().minusHours(2).minusMinutes(50)),
                new ChatMsgData(longUser != null ? longUser : an, "Em đã scan qua các lỗ hổng OWASP Top 10 như SQL Injection, XSS, CSRF, hệ thống đều được phòng vệ an toàn tuyệt đối.", LocalDateTime.now().minusHours(2).minusMinutes(10)),
                new ChatMsgData(nhung != null ? nhung : an, "Mọi người xem qua slide thuyết trình em vừa gửi trong drive nhé, bố cục phân chia các phần rất logic và mạch lạc.", LocalDateTime.now().minusHours(1).minusMinutes(35)),
                new ChatMsgData(yen != null ? yen : an, "Hình ảnh demo và video mockup giao diện em cũng đã render chất lượng cao sắc nét để chiếu lên màn hình lớn.", LocalDateTime.now().minusHours(1)),
                new ChatMsgData(dieuAnh != null ? dieuAnh : an, "Phần tài liệu hướng dẫn triển khai Docker Compose và hướng dẫn sử dụng cho người dùng em đã tổng hợp xong file PDF rồi nha.", LocalDateTime.now().minusMinutes(45)),
                new ChatMsgData(an, "Tuyệt vời quá! Tầm 14h chiều mai nhóm mình họp online một buổi để dợt thử thuyết trình demo toàn bộ luồng nhé mọi người.", LocalDateTime.now().minusMinutes(30)),
                new ChatMsgData(trong != null ? trong : an, "Nhất trí nha An, mai mình sẽ phụ trách phần trình bày kiến trúc backend và benchmark hiệu năng.", LocalDateTime.now().minusMinutes(20)),
                new ChatMsgData(trang != null ? trang : an, "Ok An ơi, mình sẽ demo luồng trải nghiệm người dùng và thiết kế UI/UX trên Figma.", LocalDateTime.now().minusMinutes(10)),
                new ChatMsgData(admin, "Rất xuất sắc! Thầy đánh giá rất cao tinh thần làm việc nhóm và chất lượng sản phẩm của các em. Chúc nhóm đạt điểm 10 tuyệt đối! 🎉🚀", LocalDateTime.now().minusMinutes(2))
        ));

        // ========================================================
        // 2. GROUP CHAT: Cộng đồng Lập trình viên Fullstack Việt Nam (9 thành viên)
        // ========================================================
        List<User> group2Members = new ArrayList<>();
        for (User u : List.of(an, trong != null ? trong : an, duc != null ? duc : an, linh != null ? linh : an,
                khoa != null ? khoa : an, tuan != null ? tuan : an, longUser != null ? longUser : an, admin, trang != null ? trang : an)) {
            if (!group2Members.contains(u)) group2Members.add(u);
        }

        ensureGroupChat("🔥 Cộng đồng Lập trình viên Fullstack Việt Nam", an, group2Members, List.of(
                new ChatMsgData(duc != null ? duc : an, "Chào anh em fullstack! Mọi người ở đây có ai đang dùng Vue 3 với Spring Boot 4 không, chia sẻ kinh nghiệm kết nối WebSocket STOMP với?", LocalDateTime.now().minusHours(12)),
                new ChatMsgData(trong != null ? trong : an, "Bên mình dùng @stomp/stompjs trên Vue 3, kết hợp SockJS fallback. Backend dùng Spring WebSocket message broker, chạy cực kỳ ổn định anh ạ.", LocalDateTime.now().minusHours(10)),
                new ChatMsgData(linh != null ? linh : an, "Đúng rồi anh Đức, phía frontend chỉ cần gói vào 1 Composable useChatSocket, quản lý reconnect tự động khi mất mạng là bao mượt.", LocalDateTime.now().minusHours(8)),
                new ChatMsgData(tuan != null ? tuan : an, "Cho em hỏi phần state management chat thì nên dùng Pinia hay Vueuse state cục bộ vậy mọi người?", LocalDateTime.now().minusHours(5)),
                new ChatMsgData(an, "Dùng Pinia lưu danh sách conversation và tin nhắn chưa đọc, còn tin nhắn chi tiết của từng phòng chat thì load dynamic theo pagination là tối ưu RAM nhất Tuấn ơi.", LocalDateTime.now().minusHours(3)),
                new ChatMsgData(khoa != null ? khoa : an, "Chuẩn luôn, kết hợp thêm Virtual Scroll nếu phòng chat có trên 1000 tin nhắn thì bao mượt mà, không bị giật lag DOM.", LocalDateTime.now().minusHours(2)),
                new ChatMsgData(duc != null ? duc : an, "Cảm ơn An và Trọng nhiều nhé, giải pháp này chuẩn chỉ quá! 👍", LocalDateTime.now().minusMinutes(40))
        ));

        // ========================================================
        // 3. GROUP CHAT: CLB Thiết kế UI/UX & Frontend HCMUNRE (8 thành viên)
        // ========================================================
        List<User> group3Members = new ArrayList<>();
        for (User u : List.of(trang != null ? trang : an, linh != null ? linh : an, nhung != null ? nhung : an,
                yen != null ? yen : an, an, dieuAnh != null ? dieuAnh : an, tuan != null ? tuan : an, admin)) {
            if (!group3Members.contains(u)) group3Members.add(u);
        }

        ensureGroupChat("🎨 CLB Thiết kế UI/UX & Frontend HCMUNRE", trang != null ? trang : an, group3Members, List.of(
                new ChatMsgData(trang != null ? trang : an, "Chào các bạn! Mình vừa cập nhật design system mới trên Figma với phong cách Modern Clean, bo góc rounded-2xl và shadow mịn.", LocalDateTime.now().minusHours(9)),
                new ChatMsgData(linh != null ? linh : an, "Đẹp xịn quá Trang ơi! Các component bên Vue 3 mình đã ánh xạ đúng 100% theo các biến Tailwind CSS rồi nhé.", LocalDateTime.now().minusHours(7)),
                new ChatMsgData(nhung != null ? nhung : an, "Thích nhất là chế độ Dark Mode, độ tương phản vừa đủ không gây mỏi mắt lúc code ban đêm.", LocalDateTime.now().minusHours(5)),
                new ChatMsgData(yen != null ? yen : an, "Mình vừa export thêm bộ icon SVG phong cách mới cho VietBlog, mọi người pull code về dùng thử nhé.", LocalDateTime.now().minusHours(3)),
                new ChatMsgData(an, "Các hiệu ứng hover và active trên nút bấm nảy nhẹ (scale-95 to scale-100) cảm giác tương tác rất thích tay.", LocalDateTime.now().minusHours(1)),
                new ChatMsgData(trang != null ? trang : an, "Cảm ơn cả nhà nhiều, đồ án kỳ này giao diện đảm bảo ăn điểm tuyệt đối! ✨", LocalDateTime.now().minusMinutes(25))
        ));

        // ========================================================
        // 4. GROUP CHAT: Nhóm Nghiên cứu AI & NLP tiếng Việt (8 thành viên)
        // ========================================================
        List<User> group4Members = new ArrayList<>();
        for (User u : List.of(khoa != null ? khoa : an, tuan != null ? tuan : an, duc != null ? duc : an,
                longUser != null ? longUser : an, an, admin, trong != null ? trong : an, linh != null ? linh : an)) {
            if (!group4Members.contains(u)) group4Members.add(u);
        }

        ensureGroupChat("💡 Nhóm Nghiên cứu AI & Xử lý ngôn ngữ tự nhiên", khoa != null ? khoa : an, group4Members, List.of(
                new ChatMsgData(khoa != null ? khoa : an, "Mọi người ơi, phần thuật toán phân tích cảm xúc (Sentiment Analysis) mình đã mở rộng bộ từ vựng Lexicon tiếng Việt rất phong phú rồi nhé.", LocalDateTime.now().minusHours(11)),
                new ChatMsgData(tuan != null ? tuan : an, "Hay quá Khoa! Giờ các từ lóng hay từ ngữ tiêu cực/tích cực đời thường như 'ngu', 'dốt', 'đỉnh', 'xịn' đều được phân loại chuẩn xác.", LocalDateTime.now().minusHours(8)),
                new ChatMsgData(an, "Cơ chế xử lý từ phủ định (negation window 2 tokens) như 'không tệ', 'chẳng dở' đảo chiều cảm xúc sang tích cực hoạt động rất thông minh.", LocalDateTime.now().minusHours(5)),
                new ChatMsgData(admin, "Thuật toán TextRank tóm tắt trích xuất câu quan trọng dựa trên đồ thị tương đồng câu cũng là một điểm sáng lớn trong báo cáo đó.", LocalDateTime.now().minusHours(2)),
                new ChatMsgData(khoa != null ? khoa : an, "Dạ vâng thầy, thuật toán chạy hoàn toàn deterministic, không phụ thuộc thư viện nặng của bên thứ 3 nên tốc độ phản hồi chỉ vài mili-giây ạ!", LocalDateTime.now().minusMinutes(15))
        ));

        // ========================================================
        // 5. DIRECT CHATS (1-ON-1)
        // ========================================================
        // Admin & Nguyen Van An
        createDirectChat(admin, an, List.of(
                new ChatMsgData(admin, "Chào bạn An, tiến độ hoàn thiện đồ án frontend tới đâu rồi?", LocalDateTime.now().minusHours(10)),
                new ChatMsgData(an, "Dạ chào thầy, phần giao diện Vue 3, tìm kiếm AI, bản đồ số Leaflet và bình luận đa cấp em đã hoàn thiện mượt mà rồi ạ! 🚀", LocalDateTime.now().minusHours(9)),
                new ChatMsgData(admin, "Tuyệt vời! Nhớ kiểm tra kỹ phần phân quyền admin và xuất báo cáo Word nhé.", LocalDateTime.now().minusHours(5)),
                new ChatMsgData(an, "Dạ vâng, em đã kiểm tra kỹ rồi, hệ thống chạy rất ổn định và mượt mà ạ!", LocalDateTime.now().minusHours(2)),
                new ChatMsgData(admin, "Tốt lắm, mai nhớ gửi thầy bản demo mới nhất để thầy xem trước.", LocalDateTime.now().minusMinutes(40)),
                new ChatMsgData(an, "Dạ vâng thầy, em gửi link demo ngay đây ạ!", LocalDateTime.now().minusMinutes(15))
        ));

        // Admin & Tran Dinh Trong
        if (trong != null) {
            createDirectChat(admin, trong, List.of(
                    new ChatMsgData(trong, "Anh ơi, cụm Docker PostgreSQL và Redis trên production em đã cấu hình xong healthcheck.", LocalDateTime.now().minusHours(12)),
                    new ChatMsgData(admin, "Tốt lắm Trọng, nhớ kiểm tra cấu hình connection pool HikariCP và backup tự động mỗi đêm nhé!", LocalDateTime.now().minusHours(9)),
                    new ChatMsgData(trong, "Vâng anh, em đã thiết lập cron backup định kỳ 0h mỗi ngày rồi ạ.", LocalDateTime.now().minusHours(4)),
                    new ChatMsgData(admin, "Cấu hình Redis cache TTL cho các bài viết trending thế nào em?", LocalDateTime.now().minusHours(1)),
                    new ChatMsgData(trong, "Dạ TTL là 30 phút, có cache invalidation tự động khi có bài viết mới ạ.", LocalDateTime.now().minusMinutes(35))
            ));
        }

        // Admin & Nguyen Thu Trang
        if (trang != null) {
            createDirectChat(admin, trang, List.of(
                    new ChatMsgData(trang, "Chào anh, em vừa hoàn thiện bộ thiết kế design system cho VietBlog trên Figma.", LocalDateTime.now().minusHours(14)),
                    new ChatMsgData(admin, "Anh đã xem qua, bảng màu HSL và giao diện Dark Mode nhìn rất sang trọng và hiện đại!", LocalDateTime.now().minusHours(11)),
                    new ChatMsgData(trang, "Dạ cảm ơn anh nhiều ạ! Em có bổ sung thêm các icon và micro-animation cho các nút tương tác nữa.", LocalDateTime.now().minusHours(6)),
                    new ChatMsgData(admin, "Trang kiểm tra thêm độ tương phản màu chữ trên nền tối đạt chuẩn WCAG AA chưa nhé.", LocalDateTime.now().minusHours(2)),
                    new ChatMsgData(trang, "Dạ em đã test qua plugin Stark trên Figma, tất cả đều đạt chuẩn AA và AAA ạ!", LocalDateTime.now().minusMinutes(50))
            ));
        }

        // Admin & Pham Minh Duc
        if (duc != null) {
            createDirectChat(admin, duc, List.of(
                    new ChatMsgData(duc, "Thầy ơi, cấu trúc package Clean Architecture theo Domain - Application - Infrastructure - Presentation đã được chuẩn hóa đồng bộ.", LocalDateTime.now().minusHours(10)),
                    new ChatMsgData(admin, "Rất tốt Đức, nhớ viết đầy đủ unit test cho các service quan trọng như AuthService và PostService nhé.", LocalDateTime.now().minusHours(7)),
                    new ChatMsgData(duc, "Dạ code coverage hiện tại đạt trên 85% rồi ạ, các case ngoại lệ đều có assert rõ ràng.", LocalDateTime.now().minusHours(3)),
                    new ChatMsgData(admin, "Xuất sắc! Báo cáo chuyên ngành viết phần kiến trúc này làm điểm nhấn nhé.", LocalDateTime.now().minusMinutes(45))
            ));
        }

        // Admin & Dang Thi My Linh
        if (linh != null) {
            createDirectChat(admin, linh, List.of(
                    new ChatMsgData(linh, "Em chào thầy ạ! Phần tối ưu kích thước bundle Vue 3 với Vite em đã cấu hình code splitting theo từng route.", LocalDateTime.now().minusHours(8)),
                    new ChatMsgData(admin, "Kích thước bundle ban đầu sau khi nén gzip là bao nhiêu em?", LocalDateTime.now().minusHours(5)),
                    new ChatMsgData(linh, "Dạ chunk chính chỉ khoảng 65KB, các view nặng như PostEditor hay Dashboard đều load async nên trang mở cực nhanh ạ.", LocalDateTime.now().minusHours(2)),
                    new ChatMsgData(admin, "Tốc độ load trang dưới 1 giây là đạt chuẩn xuất sắc rồi đó em!", LocalDateTime.now().minusMinutes(30))
            ));
        }

        // Nguyen Van An & Tran Dinh Trong
        if (trong != null) {
            createDirectChat(an, trong, List.of(
                    new ChatMsgData(an, "Trọng ơi, phần WebSocket STOMP kết nối chat real-time chạy mượt chưa?", LocalDateTime.now().minusHours(6)),
                    new ChatMsgData(trong, "Chạy cực kỳ mượt luôn An ơi, latency dưới 10ms, gửi tin nhắn nhận tức thì!", LocalDateTime.now().minusHours(4)),
                    new ChatMsgData(an, "Ok quá đỉnh, phần avatar nhóm chat và danh sách thành viên mình cũng vừa gắn API xong.", LocalDateTime.now().minusHours(2)),
                    new ChatMsgData(trong, "Tối nay rảnh vào làm ván game xả stress rồi mai tổng duyệt đồ án nhé!", LocalDateTime.now().minusMinutes(40)),
                    new ChatMsgData(an, "Haha ok bạn ơi, 9h tối chiến nhé! 🎮", LocalDateTime.now().minusMinutes(25))
            ));
        }

        // Nguyen Van An & Nguyen Thu Trang
        if (trang != null) {
            createDirectChat(an, trang, List.of(
                    new ChatMsgData(trang, "An ơi, icon reaction 3 chấm trên thanh chat hôm nay nhìn ưng ý chưa?", LocalDateTime.now().minusHours(5)),
                    new ChatMsgData(an, "Đẹp lắm Trang, nút 3 chấm rê chuột vào mới hiện, bấm vào xổ menu ghim và xóa đoạn chat chuẩn Telegram luôn.", LocalDateTime.now().minusHours(3)),
                    new ChatMsgData(trang, "Thế thì xịn sò quá! Mai demo nhớ bấm vào chỗ này khoe với hội đồng chấm điểm nha.", LocalDateTime.now().minusHours(1)),
                    new ChatMsgData(an, "Chắc chắn rồi Trang ơi, cảm ơn bạn nhiều nha! 🌸", LocalDateTime.now().minusMinutes(20))
            ));
        }

        // Nguyen Van An & Dang Thi My Linh
        if (linh != null) {
            createDirectChat(an, linh, List.of(
                    new ChatMsgData(linh, "An ơi, cho mình hỏi composable useChat bạn đang dùng watchEffect hay watch explicit thế?", LocalDateTime.now().minusHours(7)),
                    new ChatMsgData(an, "Phần lọc danh sách hội thoại mình dùng computed, còn lúc switch cuộc trò chuyện thì dùng watch conversation.id Linh nhé.", LocalDateTime.now().minusHours(4)),
                    new ChatMsgData(linh, "À thảo nào không bị re-render thừa. Mình áp dụng cho bên view Notifications thấy mượt hẳn!", LocalDateTime.now().minusHours(2)),
                    new ChatMsgData(an, "Vue 3 Composition API quản lý reactivity tách biệt như thế tiện cực kỳ luôn.", LocalDateTime.now().minusMinutes(35))
            ));
        }

        // Nguyen Van An & Do Anh Khoa
        if (khoa != null) {
            createDirectChat(an, khoa, List.of(
                    new ChatMsgData(khoa, "An ơi, phần thuật toán TextRank tóm tắt bài viết mình vừa tinh chỉnh ngưỡng hội tụ delta xuống 1e-8 rồi nhé.", LocalDateTime.now().minusHours(8)),
                    new ChatMsgData(an, "Quá chuẩn Khoa! Nhờ vậy mà bài viết dài 2000 từ tóm tắt còn 3 câu văn cực kỳ đắt giá.", LocalDateTime.now().minusHours(5)),
                    new ChatMsgData(khoa, "Ừ, mai nhớ mở bài viết về Clean Architecture để demo chức năng tóm tắt AI này nhé.", LocalDateTime.now().minusHours(1)),
                    new ChatMsgData(an, "Ok Khoa, bài đó nội dung chuẩn chỉ nhất để khoe thuật toán luôn!", LocalDateTime.now().minusMinutes(15))
            ));
        }

        log.info("Seeded comprehensive chat conversations with multiple groups, vibrant members, and realistic message histories.");
    }

    private Conversation ensureGroupChat(String name, User creator, List<User> members, List<ChatMsgData> messages) {
        Conversation groupConv = conversationRepository.findAll().stream()
                .filter(c -> c.isGroupChat() && name.equals(c.getName()))
                .findFirst()
                .orElseGet(() -> conversationRepository.save(Conversation.builder()
                        .name(name)
                        .groupChat(true)
                        .creator(creator)
                        .createdAt(LocalDateTime.now().minusDays(5))
                        .updatedAt(LocalDateTime.now())
                        .build()));

        for (User member : members) {
            if (member != null && !conversationMemberRepository.existsByConversationIdAndUserId(groupConv.getId(), member.getId())) {
                conversationMemberRepository.save(ConversationMember.builder()
                        .conversation(groupConv)
                        .user(member)
                        .lastReadAt(LocalDateTime.now().minusMinutes(10))
                        .build());
            }
        }

        long existingCount = chatMessageRepository.findByConversationId(groupConv.getId(), PageRequest.of(0, 1)).getTotalElements();
        if (existingCount < 8) {
            for (ChatMsgData msg : messages) {
                if (msg.sender != null) {
                    chatMessageRepository.save(ChatMessage.builder()
                            .conversation(groupConv)
                            .sender(msg.sender)
                            .content(msg.content)
                            .type("text")
                            .createdAt(msg.createdAt)
                            .build());
                }
            }
        }
        return groupConv;
    }

    private void createDirectChat(User u1, User u2, List<ChatMsgData> messages) {
        if (u1 == null || u2 == null) return;
        Set<String> sortedIds = new TreeSet<>(List.of(u1.getId(), u2.getId()));
        String directKey = String.join(":", sortedIds);

        Conversation conv = conversationRepository.findByDirectKey(directKey).orElseGet(() ->
                conversationRepository.save(Conversation.builder()
                        .name(u2.getName())
                        .groupChat(false)
                        .directKey(directKey)
                        .creator(u1)
                        .createdAt(LocalDateTime.now().minusDays(3))
                        .updatedAt(LocalDateTime.now())
                        .build())
        );

        if (!conversationMemberRepository.existsByConversationIdAndUserId(conv.getId(), u1.getId())) {
            conversationMemberRepository.save(ConversationMember.builder().conversation(conv).user(u1).lastReadAt(LocalDateTime.now()).build());
        }
        if (!conversationMemberRepository.existsByConversationIdAndUserId(conv.getId(), u2.getId())) {
            conversationMemberRepository.save(ConversationMember.builder().conversation(conv).user(u2).lastReadAt(LocalDateTime.now().minusMinutes(5)).build());
        }

        long count = chatMessageRepository.findByConversationId(conv.getId(), PageRequest.of(0, 1)).getTotalElements();
        if (count < 3) {
            for (ChatMsgData msg : messages) {
                if (msg.sender != null) {
                    chatMessageRepository.save(ChatMessage.builder()
                            .conversation(conv)
                            .sender(msg.sender)
                            .content(msg.content)
                            .type("text")
                            .createdAt(msg.createdAt)
                            .build());
                }
            }
        }
    }

    private record ChatMsgData(User sender, String content, LocalDateTime createdAt) {}
}
