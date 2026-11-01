package com.vietblog.infrastructure.config.seed;

import com.vietblog.infrastructure.config.seed.SeedModels.CommentSeed;
import com.vietblog.infrastructure.config.seed.SeedModels.PostSeed;

import java.util.List;

import static com.vietblog.infrastructure.config.seed.SeedModels.gallery;
import static com.vietblog.infrastructure.config.seed.SeedModels.img;

/**
 * Danh mục bài viết chuyên sâu thuộc hai mảng Công nghệ (6 bài) và Sự kiện (3 bài).
 * Tất cả hình ảnh, đoạn mã nguồn và cây bình luận 3 cấp đều khớp 100% ngữ cảnh từng bài.
 */
public final class TechAndEventPostsCatalog {

    private TechAndEventPostsCatalog() {}

    public static List<PostSeed> posts() {
        return List.of(
                cleanArchitectureSpringBoot(),
                vietnameseNlpTextRankAndSentiment(),
                vue3CompositionPerformance(),
                websocketStompRedisRealtime(),
                jwtSecurityAndRateLimiting(),
                dockerMultiStageAndCicd(),
                hcmunreTechDayEvent(),
                vietnamWebAiSummitEvent(),
                designSystemWorkshopEvent()
        );
    }

    private static PostSeed cleanArchitectureSpringBoot() {
        String content = """
                <h2>Bài toán bảo trì khi ứng dụng Backend mở rộng quy mô</h2>
                <p>Trong giai đoạn khởi đầu của một dự án Web, mô hình ba lớp truyền thống thường giúp đội ngũ phát triển ra mắt tính năng rất nhanh chóng. Tuy nhiên, khi số lượng nghiệp vụ tăng lên hàng chục phân hệ từ quản lý bài viết, bình luận đa cấp, thông báo thời gian thực cho đến kiểm duyệt nội dung, việc đặt trực tiếp logic nghiệp vụ vào tầng truy xuất cơ sở dữ liệu khiến mã nguồn trở nên chằng chịt và khó kiểm thử độc lập.</p>
                <p>Kiến trúc Clean Architecture giải quyết triệt để bài toán này thông qua quy tắc phụ thuộc một chiều hướng vào tâm. Mọi quy tắc nghiệp vụ cốt lõi nằm gọn trong tầng Domain hoàn toàn thuần Java, không phụ thuộc vào cơ sở dữ liệu quan hệ hay bất kỳ thư viện giao tiếp ngoại vi nào.</p>
                """
                + gallery(
                        img("https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=1200&q=80", "Cấu trúc phân tầng mã nguồn Backend Java trên môi trường phát triển"),
                        img("https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=1200&q=80", "Thiết kế mô hình miền nghiệp vụ độc lập với cơ sở dữ liệu")
                ) + """
                <h2>Tổ chức bốn tầng trách nhiệm trong dự án Spring Boot 3</h2>
                <p>Để cân bằng giữa tính học thuật của kiến trúc sạch và sự gọn gàng thực tiễn trong hệ sinh thái Spring Boot, cấu trúc gói mã nguồn được phân chia mạch lạc thành bốn tầng rõ rệt.</p>
                <ul>
                    <li><strong>Tầng Domain:</strong> Lưu giữ các thực thể cốt lõi, các ngoại lệ nghiệp vụ và những thuật toán miền như tính điểm xu hướng bài viết hay kiểm tra độ sâu cây bình luận.</li>
                    <li><strong>Tầng Application:</strong> Điều phối luồng thực thi của từng ca sử dụng, quản lý ranh giới giao dịch và chuyển đổi dữ liệu đầu ra qua các đối tượng DTO bất biến.</li>
                    <li><strong>Tầng Infrastructure:</strong> Đảm nhận chi tiết kỹ thuật gồm Spring Data JPA, bộ nhớ đệm Redis, cấu hình WebSocket, lưu trữ đám mây Cloudinary và bộ lọc bảo mật JWT.</li>
                    <li><strong>Tầng Presentation:</strong> Tập trung hoàn toàn vào định tuyến RESTful API, kiểm tra hợp lệ dữ liệu đầu vào và chuẩn hóa cấu trúc phản hồi JSON thống nhất.</li>
                </ul>
                <h3>Cài đặt thuật toán xếp hạng xu hướng tại tầng Domain</h3>
                <p>Một ví dụ điển hình cho sức mạnh của tầng Domain là thuật toán tính điểm bài viết thịnh hành kết hợp khoảng tin cậy Wilson Lower Bound cùng hệ số phân rã thời gian.</p>
                <pre><code class="language-java">public void calculateTrendingScore(Post post) {
                    double baseScore = Math.max(0, post.getUpvotesCount() - post.getDownvotesCount()) * 2.0
                            + post.getCommentsCount() * 3.0
                            + post.getViewsCount() * 0.1;

                    double hoursAge = Math.max(0,
                            Duration.between(post.getCreatedAt(), LocalDateTime.now()).toSeconds() / 3600.0);
                    double agePenalty = Math.pow(hoursAge + 2.0, 1.8);
                    double confidence = 0.5 + wilsonLowerBound(post.getUpvotesCount(), post.getDownvotesCount());

                    post.setTrendingScore((baseScore * confidence) / agePenalty);
                }</code></pre>
                <blockquote>
                    <p>Nhờ tách biệt hoàn toàn công thức xếp hạng khỏi truy vấn SQL, nhóm phát triển có thể viết hàng chục kịch bản kiểm thử đơn vị chạy xong trong vài phần nghìn giây mà không cần khởi tạo kết nối cơ sở dữ liệu.</p>
                </blockquote>
                <h2>Kinh nghiệm thực tế khi áp dụng cho dự án nhóm</h2>
                <p>Điểm mấu chốt khi áp dụng Clean Architecture không nằm ở việc tạo ra thật nhiều lớp trung gian máy móc, mà ở kỷ luật giữ cho các quy tắc nghiệp vụ không bị rò rỉ ra tầng Controller hay Repository. Khi ranh giới giữa các tầng được định nghĩa rõ ràng, nhiều thành viên trong nhóm có thể phát triển song song cả giao diện lẫn dịch vụ phía máy chủ mà hầu như không gặp xung đột mã nguồn.</p>
                """;

        return new PostSeed(
                "clean-architecture-spring-boot",
                "Thiết kế hệ thống Backend quy mô lớn với Spring Boot 3 và Clean Architecture",
                "Phân tích cách tổ chức bốn tầng Domain, Application, Infrastructure và Presentation giúp hệ thống Spring Boot dễ kiểm thử, dễ bảo trì và mở rộng bền vững.",
                content,
                "https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=1200&q=80",
                "cong-nghe",
                "phamminhduc@gmail.com",
                List.of("spring-boot", "clean-architecture", "java", "backend", "system-design"),
                10.8411, 106.8099, "Khu Công nghệ cao TP.HCM, TP. Thủ Đức, TP.HCM",
                2840, 94, 2, 1, null, null,
                List.of(
                        CommentSeed.of("nguyenvanan@gmail.com",
                                "Bài viết phân tích cực kỳ mạch lạc và thực tế! Nhờ tách riêng thuật toán Wilson Lower Bound sang PostDomainService mà lúc viết Unit Test tụi mình chạy nhanh và sạch hơn hẳn.",
                                16, 18,
                                List.of(
                                        CommentSeed.of("phamminhduc@gmail.com",
                                                "Cảm ơn An nhé! Đúng là khi tách phần tính toán khỏi JPA Repository thì việc tinh chỉnh trọng số thời gian hay kiểm tra biên số liệu trở nên nhẹ nhàng hơn rất nhiều.",
                                                9, 15,
                                                List.of(
                                                        CommentSeed.of("trandinhtrong@gmail.com",
                                                                "Đồng ý hai tay luôn anh Đức, lúc chạy pipeline CI trên GitHub Actions bộ test tầng Domain chạy vèo cái chưa tới 2 giây là xanh mượt.",
                                                                6, 12)
                                                )
                                        ),
                                        CommentSeed.of("vuquoctuan@gmail.com",
                                                "Cho em hỏi giữa tầng Domain và tầng Infrastructure anh có tách riêng một bộ JPA Entity khác hẳn hay dùng trực tiếp annotation trên thực thể miền để giảm boilerplate ạ?",
                                                5, 10,
                                                List.of(
                                                        CommentSeed.of("phamminhduc@gmail.com",
                                                                "Với quy mô nhóm từ 3 đến 5 thành viên thì giữ thực thể gọn gàng và tách bạch logic sang DomainService là điểm cân bằng hợp lý nhất, tránh việc phải map đi map lại quá nhiều lớp giống hệt nhau Tuấn nhé.",
                                                                8, 8)
                                                )
                                        )
                                )
                        ),
                        CommentSeed.of("ngothanhlong@gmail.com",
                                "Nếu áp dụng cứng nhắc cho các phân hệ CRUD đơn giản thì việc viết quá nhiều lớp chuyển đổi khá chậm và cồng kềnh, dễ làm tiến độ ban đầu bị tệ đi nếu nhóm chưa quen.",
                                3, 6)
                )
        );
    }

    private static PostSeed vietnameseNlpTextRankAndSentiment() {
        String content = """
                <h2>Thách thức khi xử lý văn bản tiếng Việt trên nền tảng nội dung số</h2>
                <p>Trên các nền tảng chia sẻ tri thức hiện đại, khối lượng bài viết chuyên sâu và bình luận tăng lên từng giờ khiến người đọc khó nắm bắt nhanh ý chính, đồng thời tạo áp lực lớn cho đội ngũ kiểm duyệt cộng đồng. Thay vì phụ thuộc vào các dịch vụ đám mây trả phí có độ trễ mạng cao, việc xây dựng các thuật toán xử lý ngôn ngữ tự nhiên chạy nội cục ngay trên máy chủ Java mang lại tính chủ động tuyệt đối về hiệu năng và quyền riêng tư dữ liệu.</p>
                <h2>Tóm tắt bài viết trích xuất với đồ thị TF-IDF TextRank</h2>
                <p>Thuật toán TextRank mô hình hóa mỗi bài viết thành một đồ thị vô hướng có trọng số, trong đó mỗi câu văn hoàn chỉnh đóng vai trò là một đỉnh của đồ thị. Mức độ liên kết ngữ nghĩa giữa hai câu bất kỳ được xác định bằng độ tương đồng Cosine trên không gian vector TF-IDF sau khi đã chuẩn hóa Unicode NFC và loại bỏ từ dừng tiếng Việt.</p>
                """
                + gallery(
                        img("https://images.unsplash.com/photo-1555949963-ff9fe0c870eb?auto=format&fit=crop&w=1200&q=80", "Trực quan hóa thuật toán phân tích văn bản và ma trận trọng số"),
                        img("https://images.unsplash.com/photo-1504639725590-34d0984388bd?auto=format&fit=crop&w=1200&q=80", "Kiểm thử độ hội tụ của giải thuật TextRank trên tập văn bản tiếng Việt"),
                        img("https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1200&q=80", "Đánh giá chất lượng câu tóm tắt trích xuất cùng nhóm nghiên cứu")
                ) + """
                <p>Trong quá trình lặp tính điểm xếp hạng với hệ số cản 0.85, các câu cô lập không có từ vựng chung với phần còn lại được phân phối lại trọng số đồng đều nhằm bảo toàn tổng xác suất toàn đồ thị. Quá trình lặp dừng lại ngay khi độ lệch chuẩn giữa hai vòng liên tiếp nhỏ hơn ngưỡng một phần trăm triệu, sau đó các câu đạt điểm cao nhất được sắp xếp lại theo đúng trình tự xuất hiện gốc để giữ trọn mạch văn tự nhiên của tác giả.</p>
                <h2>Phân tích sắc thái bình luận kết hợp xử lý từ phủ định tiếng Việt</h2>
                <p>Đối với bài toán thống kê cảm xúc cộng đồng, phương pháp từ điển Lexicon được mở rộng thêm bước quét cụm từ ghép hai âm tiết và cửa sổ ngữ cảnh phủ định phía trước từ khóa cảm xúc.</p>
                <pre><code class="language-java">// Đảo chiều sắc thái khi gặp từ phủ định trong khoảng cách gần
                for (int j = Math.max(0, i - 2); j &lt; i; j++) {
                    if (NEGATION.contains(words.get(j))) {
                        sign = -sign;
                        break;
                    }
                }</code></pre>
                <p>Nhờ cơ chế kiểm tra cửa sổ hai từ đứng trước, những cách diễn đạt quen thuộc trong tiếng Việt như không tốt, chưa mượt hay chẳng hay đều được nhận diện chính xác sang nhóm tiêu cực thay vì bị bắt nhầm theo từ đơn lẻ. Kết quả thử nghiệm thực tế cho thấy thời gian phản hồi trung bình cho cả tóm tắt bài viết dài hai nghìn từ lẫn thống kê hàng nghìn bình luận chỉ tốn chưa đầy mười lăm miligiây.</p>
                """;

        return new PostSeed(
                "vietnamese-nlp-textrank-sentiment",
                "Xử lý ngôn ngữ tự nhiên tiếng Việt: Kết hợp TF-IDF TextRank và Lexicon trong phân tích nội dung",
                "Chi tiết cách xây dựng mô-đun tóm tắt bài viết tự động bằng đồ thị TF-IDF TextRank và bộ phân tích cảm xúc tiếng Việt có xử lý cấu trúc phủ định.",
                content,
                "https://images.unsplash.com/photo-1555949963-ff9fe0c870eb?auto=format&fit=crop&w=1200&q=80",
                "cong-nghe",
                "doanhkhoa@gmail.com",
                List.of("nlp", "textrank", "ai", "sentiment-analysis", "tieng-viet"),
                10.7965, 106.6668, "Trường Đại học Tài nguyên và Môi trường TP.HCM, 236B Lê Văn Sỹ, Tân Bình",
                1960, 82, 1, 0, null, null,
                List.of(
                        CommentSeed.of("vuquoctuan@gmail.com",
                                "Phần xử lý đỉnh treo dangling-node trong TextRank quá hay anh Khoa ơi! Trước đây em tự cài đặt hay bị hụt tổng điểm khi gặp các câu tiêu đề ngắn, đọc bài này sáng tỏ hẳn.",
                                14, 9,
                                List.of(
                                        CommentSeed.of("doanhkhoa@gmail.com",
                                                "Chuẩn đó Tuấn, nếu không phân phối lại phần trọng số của các câu có bậc bằng 0 thì ma trận chuyển trạng thái sẽ bị thất thoát điểm sau vài chục vòng lặp.",
                                                10, 7,
                                                List.of(
                                                        CommentSeed.of("nguyenvanan@gmail.com",
                                                                "Mình cũng ấn tượng nhất ở chỗ giữ lại thứ tự câu theo mạch gốc của bài viết, đọc đoạn tóm tắt cảm giác liền mạch và tự nhiên cực kỳ!",
                                                                7, 5)
                                                )
                                        )
                                )
                        ),
                        CommentSeed.of("admin@vietblog.com",
                                "Giải pháp chạy hoàn toàn nội bộ không phụ thuộc khóa API bên ngoài giúp hệ thống luôn ổn định và kiểm soát tốt chi phí vận hành.",
                                11, 4),
                        CommentSeed.of("phamminhduc@gmail.com",
                                "Cho mình hỏi bộ từ điển từ ghép hai âm tiết hiện tại đang nạp từ tệp tài nguyên tĩnh trong classpath đúng không Khoa?",
                                4, 2)
                )
        );
    }

    private static PostSeed vue3CompositionPerformance() {
        String content = """
                <h2>Tư duy tổ chức mã nguồn Frontend theo miền tính năng</h2>
                <p>Khi xây dựng một nền tảng cộng đồng tích hợp cả trình soạn thảo văn bản giàu định dạng, bản đồ tương tác, nhắn tin thời gian thực và bảng điều khiển quản trị, việc dồn toàn bộ trạng thái lẫn xử lý giao diện vào bên trong tệp thành phần Vue sẽ nhanh chóng tạo ra những tệp mã nguồn khổng lồ khó đọc hiểu. Composition API trong Vue 3 cho phép tách biệt trọn vẹn luồng dữ liệu phản ứng ra các hàm composable chuyên biệt theo từng nghiệp vụ.</p>
                """
                + gallery(
                        img("https://images.unsplash.com/photo-1633356122544-f134324a6cee?auto=format&fit=crop&w=1200&q=80", "Phát triển giao diện hiện đại với hệ sinh thái Vue 3 và TypeScript"),
                        img("https://images.unsplash.com/photo-1507238691740-187a5b1d37b8?auto=format&fit=crop&w=1200&q=80", "Tinh chỉnh bố cục đáp ứng đa thiết bị và hệ thống màu sắc đồng nhất")
                ) + """
                <h2>Chiến lược chia nhỏ gói tải và tối ưu vòng đời cập nhật DOM</h2>
                <p>Thay vì đóng gói toàn bộ thư viện biểu đồ quản trị, bản đồ địa lý và trình soạn thảo Tiptap vào gói tải khởi động ban đầu, hệ thống định tuyến sử dụng cơ chế nhập động theo từng trang. Người đọc khi mở trang chủ hoặc xem bài viết chỉ tải đúng phần mã thiết yếu nhẹ nhàng, giúp thời gian hiển thị nội dung đầu tiên luôn đạt tốc độ tức thì ngay cả trên mạng di động.</p>
                <pre><code class="language-typescript">// Tách biệt xử lý điều hướng bất đồng bộ và phân tách chunk theo route
                const routes: RouteRecordRaw[] = [
                  {
                    path: '/posts/:id',
                    name: 'post-detail',
                    component: () =&gt; import('@/views/posts/PostDetailView.vue'),
                  },
                  {
                    path: '/admin/analytics',
                    name: 'admin-analytics',
                    component: () =&gt; import('@/views/admin/AnalyticsView.vue'),
                  },
                ]</code></pre>
                <h3>Ủy quyền sự kiện thông minh cho nội dung bài viết động</h3>
                <p>Đối với vùng nội dung HTML của bài viết có chứa nhiều cụm lưới ảnh nghệ thuật, việc gắn trình lắng nghe sự kiện riêng lẻ lên từng thẻ ảnh dễ gây rò rỉ bộ nhớ khi chuyển trang liên tục. Kỹ thuật ủy quyền sự kiện trực tiếp trên phần tử khung chứa kết hợp thuộc tính định danh nhóm ảnh giúp trình xem ảnh toàn màn hình nhận diện chính xác bộ sưu tập đang mở với chi phí xử lý bằng không.</p>
                <blockquote>
                    <p>Hiệu năng giao diện tốt không chỉ đo bằng điểm số công cụ kiểm tra, mà thể hiện ở cảm giác phản hồi mượt mà trong từng thao tác cuộn trang, chuyển đổi chế độ tối và phóng to hình ảnh của người đọc.</p>
                </blockquote>
                """;

        return new PostSeed(
                "vue3-composition-performance",
                "Tối ưu hiệu năng ứng dụng Vue 3 với Composition API, Code Splitting và Quản lý trạng thái",
                "Kinh nghiệm phân tách Composable, cấu hình tải lười theo định tuyến với Vite và kỹ thuật ủy quyền sự kiện DOM cho nền tảng nội dung giàu hình ảnh.",
                content,
                "https://images.unsplash.com/photo-1633356122544-f134324a6cee?auto=format&fit=crop&w=1200&q=80",
                "cong-nghe",
                "dangthimylinh@gmail.com",
                List.of("vuejs", "typescript", "frontend", "vite", "web-performance"),
                10.7744, 106.7035, "Phố đi bộ Nguyễn Huệ, Quận 1, TP.HCM",
                2150, 76, 1, 3, null, null,
                List.of(
                        CommentSeed.of("nguyenthutrang@gmail.com",
                                "Bài viết chia sẻ cực kỳ tâm huyết Linh ơi! Phần chuyển cảnh Lightbox và chế độ Dark Mode trên giao diện chạy mượt thật sự, đứng ở góc độ thiết kế mình nhìn rất ưng mắt.",
                                12, 48,
                                List.of(
                                        CommentSeed.of("dangthimylinh@gmail.com",
                                                "Cảm ơn Trang nhiều nha, cũng nhờ bộ biến màu ngữ nghĩa bên thiết kế của Trang chuẩn chỉ sẵn nên lúc lên CSS với Tailwind nhàn hơn bao nhiêu!",
                                                7, 42)
                                )
                        ),
                        CommentSeed.of("nguyenvanan@gmail.com",
                                "Cách dùng Event Delegation trên khung .prose để bắt click cho toàn bộ cụm ảnh gallery vừa gọn code vừa tránh phải gắn listener lại mỗi lần nội dung cập nhật.",
                                9, 30),
                        CommentSeed.of("vuquoctuan@gmail.com",
                                "Mình từng lạm dụng watch sâu trên mảng dữ liệu lớn khiến trang cuộn khá giật và kém mượt, chuyển sang dùng shallowRef theo gợi ý như bài viết thấy cải thiện rõ rệt.",
                                5, 20)
                )
        );
    }

    private static PostSeed websocketStompRedisRealtime() {
        String content = """
                <h2>Kiến trúc giao tiếp song công cho tính năng Nhắn tin cộng đồng</h2>
                <p>Trong một mạng xã hội chia sẻ tri thức hiện đại, người dùng kỳ vọng tin nhắn trao đổi chuyên môn, trạng thái đã xem cũng như thông báo tương tác phải xuất hiện ngay lập tức mà không cần tải lại trình duyệt. Giao thức WebSocket kết hợp định dạng khung tin STOMP cung cấp kênh truyền hai chiều bền vững với độ trễ chỉ tính bằng vài phần nghìn giây giữa trình duyệt và máy chủ.</p>
                """
                + gallery(
                        img("https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&w=1200&q=80", "Hạ tầng máy chủ phân tán xử lý kết nối thời gian thực"),
                        img("https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=1200&q=80", "Mô hình truyền bản tin sự kiện thời gian thực qua trung gian Redis"),
                        img("https://images.unsplash.com/photo-1461749280684-dccba630e2f6?auto=format&fit=crop&w=1200&q=80", "Theo dõi nhật ký luồng sự kiện STOMP và đồng bộ trạng thái đọc tin nhắn")
                ) + """
                <h2>Giải quyết bài toán mở rộng ngang bằng Redis Pub/Sub</h2>
                <p>Nếu chỉ sử dụng bộ điều phối tin nhắn nằm trong bộ nhớ RAM của một tiến trình Spring Boot đơn lẻ, người dùng kết nối vào máy chủ thứ nhất sẽ không thể nhận được tin nhắn từ người dùng đang kết nối ở máy chủ thứ hai phía sau bộ cân bằng tải. Việc đặt Redis Pub/Sub làm trục phát tán sự kiện ở giữa giúp mọi nút ứng dụng nhận và đẩy gói tin tới đúng phiên kết nối đích một cách đồng bộ.</p>
                <pre><code class="language-java">@Override
                public void configureMessageBroker(MessageBrokerRegistry registry) {
                    registry.enableSimpleBroker("/topic", "/queue");
                    registry.setApplicationDestinationPrefixes("/app");
                    registry.setUserDestinationPrefix("/user");
                }</code></pre>
                <h3>Xác thực phiên kết nối và trải nghiệm phản hồi tức thì</h3>
                <p>Mỗi yêu cầu bắt tay STOMP đều đi qua bộ chặn xác thực mã thông báo JWT ngay ở khung CONNECT để gắn danh tính người dùng vào phiên làm việc an toàn. Ở phía giao diện, tin nhắn gửi đi được hiển thị lạc quan ngay lập tức kèm mã đối chiếu tạm thời, sau đó tự động khớp nối với bản ghi chính thức từ máy chủ để loại bỏ hoàn toàn hiện tượng nhấp nháy hay nhân đôi dòng chat.</p>
                """;

        return new PostSeed(
                "websocket-stomp-redis-realtime",
                "Xây dựng hệ thống Nhắn tin và Thông báo thời gian thực với WebSocket STOMP và Redis Pub/Sub",
                "Giải pháp kiến trúc kết hợp WebSocket STOMP, xác thực JWT ở tầng kết nối và trục sự kiện Redis Pub/Sub cho tính năng chat nhóm và chat trực tiếp.",
                content,
                "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&w=1200&q=80",
                "cong-nghe",
                "nguyenvanan@gmail.com",
                List.of("websocket", "redis", "spring-boot", "realtime", "chat-system"),
                10.8538, 106.6278, "Công viên Phần mềm Quang Trung, Quận 12, TP.HCM",
                2490, 88, 2, 5, null, null,
                List.of(
                        CommentSeed.of("trandinhtrong@gmail.com",
                                "Kiến trúc dùng Redis Pub/Sub làm cầu nối giữa các container Spring Boot cực kỳ chuẩn bài An ơi! Mình đã thử dựng 3 replica sau Nginx mà tin nhắn nhóm vẫn đồng bộ tức thì.",
                                15, 72,
                                List.of(
                                        CommentSeed.of("nguyenvanan@gmail.com",
                                                "Cảm ơn Trọng đã hỗ trợ phần cấu hình nâng cấp kết nối Upgrade header trên Nginx nhé, nhờ thông số timeout chuẩn mà kết nối giữ ổn định suốt nhiều giờ liền.",
                                                8, 65,
                                                List.of(
                                                        CommentSeed.of("phamminhduc@gmail.com",
                                                                "Hai anh em phối hợp ăn ý quá! Nhớ bổ sung thêm nhịp heartbeat hai chiều để phát hiện sớm các kết nối mạng chập chờn trên thiết bị di động nhé.",
                                                                10, 58)
                                                )
                                        )
                                )
                        ),
                        CommentSeed.of("dangthimylinh@gmail.com",
                                "Cơ chế Optimistic UI bên khung chat nhỏ góc phải hoạt động rất mượt, bấm Enter gửi ảnh hay trả lời tin nhắn đều lên ngay không bị khựng nhịp nào.",
                                9, 36),
                        CommentSeed.of("ngothanhlong@gmail.com",
                                "Việc kiểm tra quyền thành viên cuộc trò chuyện trước khi cho phép subscribe vào kênh chat rất quan trọng để chống nghe lén trái phép giữa các phòng.",
                                7, 19)
                )
        );
    }

    private static PostSeed jwtSecurityAndRateLimiting() {
        String content = """
                <h2>Thiết kế lớp phòng thủ nhiều tầng cho nền tảng nội dung mở</h2>
                <p>Các hệ thống cho phép người dùng xuất bản bài viết định dạng HTML, tải tệp đa phương tiện và bình luận công khai luôn là mục tiêu hàng đầu của các cuộc tấn công chèn mã độc XSS, vét cạn mật khẩu và phát tán nội dung rác tự động. Một kiến trúc bảo mật đáng tin cậy cần kết hợp chặt chẽ giữa xác thực phi trạng thái, làm sạch dữ liệu đầu vào và kiểm soát lưu lượng truy cập.</p>
                <p><img src="https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?auto=format&fit=crop&w=1200&q=80" alt="Giám sát an toàn thông tin và bảo vệ tầng giao tiếp API" /></p>
                <h2>Cơ chế cặp khóa Access Token ngắn hạn và Refresh Token xoay vòng</h2>
                <p>Thay vì cấp phát một mã JWT có thời hạn kéo dài nhiều tuần tiềm ẩn rủi ro khi bị đánh cắp, máy chủ phát hành Access Token có vòng đời ngắn để xác thực nhanh trên từng yêu cầu API kèm theo Refresh Token dùng riêng cho việc gia hạn phiên. Khi phát hiện tài khoản vi phạm quy tắc cộng đồng và bị khóa bởi quản trị viên, bộ lọc bảo mật kiểm tra trạng thái hoạt động để thu hồi quyền truy cập ngay lập tức.</p>
                <h2>Làm sạch mã HTML và giới hạn tần suất gửi yêu cầu</h2>
                <p>Mọi nội dung bài viết gửi lên từ trình soạn thảo đều đi qua bộ lọc danh sách trắng chỉ giữ lại các thẻ định dạng văn bản an toàn như tiêu đề, đoạn văn, danh sách, trích dẫn, khối mã nguồn và thẻ hình ảnh hợp lệ, loại bỏ hoàn toàn các thuộc tính thực thi kịch bản ngầm. Song song đó, bộ giới hạn tốc độ theo cửa sổ trượt ngăn chặn hiệu quả hành vi gửi hàng loạt yêu cầu đăng nhập hoặc bình luận tự động trong thời gian ngắn.</p>
                """;

        return new PostSeed(
                "jwt-security-rate-limiting",
                "Bảo mật ứng dụng Web hiện đại: Cơ chế xoay vòng JWT Refresh Token, XSS Sanitization và Rate Limiting",
                "Tổng hợp các lớp bảo vệ thiết yếu cho nền tảng blog cộng đồng từ xác thực JWT, phân quyền RBAC, lọc mã độc HTML đến chống lạm dụng API.",
                content,
                "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?auto=format&fit=crop&w=1200&q=80",
                "cong-nghe",
                "ngothanhlong@gmail.com",
                List.of("security", "jwt", "spring-security", "owasp", "rate-limiting"),
                21.0307, 105.7828, "Phố Duy Tân, Quận Cầu Giấy, Hà Nội",
                1740, 64, 1, 9, null, null,
                List.of(
                        CommentSeed.of("phamminhduc@gmail.com",
                                "Phần làm sạch HTML bằng danh sách thẻ trắng cực kỳ cần thiết cho các nền tảng dùng trình soạn thảo Rich Text, vừa giữ được định dạng đẹp vừa chặn triệt để XSS.",
                                11, 120),
                        CommentSeed.of("trandinhtrong@gmail.com",
                                "Lưu ý nhỏ là khi đặt ứng dụng sau Reverse Proxy mà chưa cấu hình đọc chuẩn X-Forwarded-For thì bộ giới hạn tần suất theo IP dễ gom nhầm nhiều người dùng chung mạng văn phòng, trải nghiệm sẽ khá tệ.",
                                8, 96,
                                List.of(
                                        CommentSeed.of("ngothanhlong@gmail.com",
                                                "Góp ý quá chuẩn luôn Trọng! Mình đã bổ sung cấu hình chỉ tin cậy header chuyển tiếp từ dải mạng nội bộ của cổng proxy để lấy đúng địa chỉ IP thực.",
                                                6, 90)
                                )
                        )
                )
        );
    }

    private static PostSeed dockerMultiStageAndCicd() {
        String content = """
                <h2>Chuẩn hóa môi trường chạy từ máy cá nhân lên máy chủ đám mây</h2>
                <p>Tình trạng ứng dụng chạy trơn tru trên máy lập trình viên nhưng phát sinh lỗi thiếu thư viện hoặc sai khác phiên bản Java khi đưa lên máy chủ thử nghiệm là nỗi ám ảnh quen thuộc của nhiều dự án phần mềm. Kỹ thuật đóng gói Docker Multi-stage Build tách riêng giai đoạn biên dịch nặng nề khỏi ảnh chạy thực tế, giúp giảm kích thước gói triển khai từ hơn tám trăm megabyte xuống chỉ còn chưa tới hai trăm megabyte.</p>
                """
                + gallery(
                        img("https://images.unsplash.com/photo-1605745341112-85968b19335b?auto=format&fit=crop&w=1200&q=80", "Mô hình đóng gói dịch vụ bằng container hóa đồng nhất"),
                        img("https://images.unsplash.com/photo-1607799279861-4dd421887fb3?auto=format&fit=crop&w=1200&q=80", "Thiết lập quy trình tích hợp và triển khai tự động trên hạ tầng đám mây")
                ) + """
                <h2>Cấu hình phối hợp đa dịch vụ với kiểm tra sức khỏe tự động</h2>
                <p>Trong tệp điều phối hệ thống, dịch vụ Backend chỉ bắt đầu khởi chạy sau khi PostgreSQL và Redis đã phản hồi thành công lệnh kiểm tra trạng thái sẵn sàng, loại bỏ hoàn toàn lỗi từ chối kết nối lúc khởi động đồng thời.</p>
                <pre><code class="language-yaml">services:
                  postgres:
                    image: postgres:16-alpine
                    healthcheck:
                      test: ["CMD-SHELL", "pg_isready -U postgres -d vietblog"]
                      interval: 5s
                      retries: 5
                  backend:
                    build: ./backend
                    depends_on:
                      postgres:
                        condition: service_healthy</code></pre>
                <p>Kết hợp cùng quy trình kiểm thử tự động trên GitHub Actions mỗi khi có nhánh tính năng mới gửi yêu cầu gộp mã, đội ngũ phát triển luôn phát hiện sớm các sai sót biên dịch ngay từ vòng đánh giá đầu tiên.</p>
                """;

        return new PostSeed(
                "docker-multistage-cicd",
                "Đóng gói và triển khai hệ thống Fullstack lên Cloud với Docker Multi-stage Build và GitHub Actions",
                "Hướng dẫn tối ưu dung lượng Docker Image cho Spring Boot 3 cùng Vue 3 Nginx, thiết lập Healthcheck PostgreSQL và tự động hóa quy trình CI/CD.",
                content,
                "https://images.unsplash.com/photo-1605745341112-85968b19335b?auto=format&fit=crop&w=1200&q=80",
                "cong-nghe",
                "trandinhtrong@gmail.com",
                List.of("docker", "devops", "cicd", "postgresql", "cloud"),
                15.9785, 108.2615, "Khu đô thị Công nghệ FPT City, Ngũ Hành Sơn, Đà Nẵng",
                1890, 71, 2, 14, null, null,
                List.of(
                        CommentSeed.of("nguyenvanan@gmail.com",
                                "Thêm điều kiện service_healthy cho PostgreSQL cứu cánh thật sự, trước đây chạy docker compose up lần đầu hay bị lỗi kết nối do database chưa kịp mở cổng.",
                                10, 210),
                        CommentSeed.of("vuquoctuan@gmail.com",
                                "Anh Trọng cho em hỏi mình dùng bản JRE Alpine hay Eclipse Temurin Slim cho Java 21 thì ổn định hơn khi xử lý ảnh với AWT ImageIO ạ?",
                                5, 180,
                                List.of(
                                        CommentSeed.of("trandinhtrong@gmail.com",
                                                "Nếu có xử lý trích xuất đặc trưng ảnh bằng thư viện đồ họa chuẩn của Java thì Temurin Jammy hoặc Debian Slim sẽ đầy đủ thư viện phông chữ và đồ họa gốc ổn định hơn Alpine nhiều em nhé!",
                                                9, 172)
                                )
                        )
                )
        );
    }

    private static PostSeed hcmunreTechDayEvent() {
        String content = """
                <h2>Không gian hội tụ sáng tạo của sinh viên Công nghệ Thông tin HCMUNRE</h2>
                <p>Ngày hội Công nghệ Thông tin và Kết nối Doanh nghiệp HCMUNRE TechDay 2026 là sự kiện học thuật thường niên quy mô lớn dành cho sinh viên Khoa Công nghệ Thông tin, Trường Đại học Tài nguyên và Môi trường TP.HCM. Chương trình năm nay quy tụ hơn hai mươi gian trưng bày sản phẩm nghiên cứu khoa học, đồ án chuyên ngành tiêu biểu cùng sự góp mặt của các chuyên gia kỹ nghệ phần mềm đến từ nhiều doanh nghiệp công nghệ hàng đầu tại TP.HCM.</p>
                """
                + gallery(
                        img("https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=1200&q=80", "Toàn cảnh hội trường lớn diễn ra phiên khai mạc Ngày hội Công nghệ HCMUNRE"),
                        img("https://images.unsplash.com/photo-1523240795612-9a054b0db644?auto=format&fit=crop&w=1200&q=80", "Nhóm sinh viên thảo luận và giới thiệu kiến trúc sản phẩm tại bàn trưng bày"),
                        img("https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=1200&q=80", "Phiên trao đổi kỹ thuật trực tiếp giữa kỹ sư doanh nghiệp và sinh viên năm cuối"),
                        img("https://images.unsplash.com/photo-1531403009284-440f080d1e12?auto=format&fit=crop&w=1200&q=80", "Trình diễn trực tiếp các ứng dụng Web, Trí tuệ nhân tạo và Bản đồ số"),
                        img("https://images.unsplash.com/photo-1523050854058-8df90110c9f1?auto=format&fit=crop&w=1200&q=80", "Không gian khuôn viên trường rực rỡ trong ngày hội học thuật và hướng nghiệp")
                ) + """
                <h2>Các hoạt động trọng tâm trong khuôn khổ chương trình</h2>
                <ul>
                    <li><strong>Triển lãm sản phẩm công nghệ sinh viên:</strong> Trải nghiệm thực tế các đề tài nền tảng cộng đồng thông minh, hệ thống thông tin địa lý WebGIS, ứng dụng xử lý ngôn ngữ tự nhiên tiếng Việt và giải pháp giám sát môi trường IoT.</li>
                    <li><strong>Tọa đàm Định hướng Kỹ sư Phần mềm thời đại AI:</strong> Lắng nghe góc nhìn thực chiến từ các kiến trúc sư hệ thống về những kỹ năng cốt lõi giúp sinh viên mới ra trường tạo dấu ấn chuyên môn vững chắc.</li>
                    <li><strong>Góc tư vấn hồ sơ năng lực và phỏng vấn thử:</strong> Các chuyên viên tuyển dụng nhân sự công nghệ trực tiếp góp ý cách trình bày dự án cá nhân trên GitHub và thực hành phỏng vấn kỹ thuật một kèm một.</li>
                </ul>
                <h2>Thông tin đăng ký tham dự</h2>
                <p>Sự kiện mở cửa tự do cho toàn thể sinh viên trong và ngoài trường tại khu vực Hội trường chính cơ sở Lê Văn Sỹ. Các bạn sinh viên tham gia đầy đủ phiên tọa đàm chuyên đề buổi sáng sẽ được cấp giấy chứng nhận hoạt động học thuật và nhận nhiều phần quà công nghệ thiết thực từ ban tổ chức.</p>
                """;

        return new PostSeed(
                "hcmunre-it-career-techday-2026",
                "Ngày hội Công nghệ Thông tin và Kết nối Doanh nghiệp HCMUNRE TechDay 2026",
                "Sự kiện triển lãm sản phẩm công nghệ sinh viên, tọa đàm định hướng nghề nghiệp kỹ sư phần mềm và phỏng vấn thử trực tiếp cùng doanh nghiệp tại HCMUNRE.",
                content,
                "https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=1200&q=80",
                "su-kien",
                "admin@vietblog.com",
                List.of("hcmunre", "su-kien", "techday", "cntt", "huong-nghiep"),
                10.7965, 106.6668, "Hội trường lớn HCMUNRE, 236B Lê Văn Sỹ, Quận Tân Bình, TP.HCM",
                3120, 112, 0, 2, 5, 8,
                List.of(
                        CommentSeed.of("vuquoctuan@gmail.com",
                                "Háo hức quá ạ! Nhóm em đã đăng ký một bàn trưng bày sản phẩm ứng dụng Web kết hợp xử lý ngôn ngữ tự nhiên, hẹn gặp mọi người tại sảnh triển lãm sáng hôm đó nhé!",
                                18, 36,
                                List.of(
                                        CommentSeed.of("hoangdieuanh@gmail.com",
                                                "Tuyệt quá Tuấn ơi! Bên đoàn doanh nghiệp khách mời tụi chị rất mong được ghé xem trực tiếp các dự án thực tế của sinh viên HCMUNRE năm nay.",
                                                12, 30,
                                                List.of(
                                                        CommentSeed.of("nguyenvanan@gmail.com",
                                                                "Chị Diệu Anh ơi, phiên sửa CV và phỏng vấn thử buổi chiều mình đăng ký trước qua biểu mẫu hay lấy số thứ tự trực tiếp tại bàn đón tiếp vậy ạ?",
                                                                6, 24)
                                                )
                                        )
                                )
                        ),
                        CommentSeed.of("dangthimylinh@gmail.com",
                                "Năm ngoái mình tham gia TechDay học được bao nhiêu kinh nghiệm trình bày kiến trúc dự án từ các anh chị cựu sinh viên, các bạn khóa dưới nhất định đừng bỏ lỡ nha!",
                                14, 18)
                )
        );
    }

    private static PostSeed vietnamWebAiSummitEvent() {
        String content = """
                <h2>Bức tranh toàn cảnh về kỹ nghệ Web và Trí tuệ nhân tạo ứng dụng</h2>
                <p>Diễn đàn Vietnam Web &amp; AI Summit quy tụ hơn một nghìn kỹ sư phát triển sản phẩm, nhà nghiên cứu dữ liệu và quản lý kỹ thuật tại TP.HCM để cùng mổ xẻ các bài toán thực tế khi đưa mô hình thông minh vào sản phẩm phục vụ hàng triệu người dùng. Không dừng ở các khái niệm lý thuyết, toàn bộ mười hai phiên chia sẻ chuyên sâu đều xoay quanh kiến trúc vận hành thực chiến, tối ưu độ trễ và kiểm soát chất lượng đầu ra.</p>
                """
                + gallery(
                        img("https://images.unsplash.com/photo-1515378791036-0648a3ef77b2?auto=format&fit=crop&w=1200&q=80", "Diễn giả trình bày kiến trúc tìm kiếm đa phương thức tại sân khấu chính"),
                        img("https://images.unsplash.com/photo-1551434678-e076c223a692?auto=format&fit=crop&w=1200&q=80", "Phiên thảo luận bàn tròn về tối ưu chi phí hạ tầng cho sản phẩm AI nội địa"),
                        img("https://images.unsplash.com/photo-1542744173-8e7e53415bb0?auto=format&fit=crop&w=1200&q=80", "Khu vực thực hành mã nguồn trực tiếp cùng các kỹ sư trưởng"),
                        img("https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=1200&q=80", "Không gian kết nối giao lưu giữa cộng đồng lập trình viên trẻ"),
                        img("https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=1200&q=80", "Trưng bày giải pháp tìm kiếm ngữ nghĩa và bản đồ số thông minh"),
                        img("https://images.unsplash.com/photo-1552664730-d307ca884978?auto=format&fit=crop&w=1200&q=80", "Tổng kết phiên hỏi đáp chuyên môn sôi nổi cuối buổi sáng")
                ) + """
                <h2>Ba chuyên đề song song đáng chú ý nhất</h2>
                <p>Người tham dự có thể linh hoạt lựa chọn phòng chuyên đề phù hợp với định hướng công việc hiện tại của mình trong suốt cả ngày diễn ra sự kiện.</p>
                <ul>
                    <li><strong>Chuyên đề Kiến trúc Backend &amp; Dữ liệu lớn:</strong> Tập trung vào tối ưu chỉ mục PostgreSQL, thiết kế hàng đợi sự kiện bất đồng bộ và chiến lược bộ nhớ đệm đa tầng.</li>
                    <li><strong>Chuyên đề Tìm kiếm thông minh &amp; NLP tiếng Việt:</strong> Trình bày phương pháp kết hợp tìm kiếm từ khóa cổ điển với độ tương đồng ngữ nghĩa và trích xuất đặc trưng hình ảnh.</li>
                    <li><strong>Chuyên đề Giao diện Web thế hệ mới:</strong> Khai thác khả năng kết xuất mượt mà, kiến trúc thành phần tái sử dụng cao và trải nghiệm tương tác giàu cảm xúc.</li>
                </ul>
                """;

        return new PostSeed(
                "vietnam-web-ai-summit",
                "Diễn đàn Kỹ nghệ Phần mềm và Ứng dụng Trí tuệ Nhân tạo Vietnam Web & AI Summit",
                "Hội nghị công nghệ quy mô lớn tại SECC Quận 7 với 3 chuyên đề chuyên sâu về Kiến trúc hệ thống chịu tải cao, NLP tiếng Việt và Giao diện Web hiện đại.",
                content,
                "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=1200&q=80",
                "su-kien",
                "hoangdieuanh@gmail.com",
                List.of("summit", "web-engineering", "ai", "su-kien", "saigon"),
                10.7294, 106.7217, "Trung tâm Hội chợ và Triển lãm Sài Gòn (SECC), Quận 7, TP.HCM",
                2280, 79, 1, 6, 10, 9,
                List.of(
                        CommentSeed.of("doanhkhoa@gmail.com",
                                "Chuyên đề kết hợp tìm kiếm từ khóa và tương đồng ngữ nghĩa đúng mảng mình đang nghiên cứu sâu luôn, đã đặt lịch có mặt từ 8 giờ sáng!",
                                11, 95),
                        CommentSeed.of("phamminhduc@gmail.com",
                                "Khu vực hầm gửi xe bên SECC vào giờ cao điểm buổi sáng thường khá đông và kẹt chậm, anh em đi nhóm nên tới sớm tầm 20 phút cho thong thả nhé.",
                                7, 84)
                )
        );
    }

    private static PostSeed designSystemWorkshopEvent() {
        String content = """
                <h2>Từ bản vẽ Figma rời rạc đến hệ thống giao diện nhất quán</h2>
                <p>Nhiều đội ngũ phát triển sản phẩm gặp tình trạng mỗi trang mới ra mắt lại xuất hiện thêm vài sắc độ xám khác nhau, khoảng cách lề thiếu đồng bộ và chế độ nền tối bị chói mắt khó đọc. Buổi thực hành chuyên sâu cuối tuần này được thiết kế dành riêng cho các nhà thiết kế sản phẩm và kỹ sư Frontend muốn xây dựng một ngôn ngữ thiết kế chung chặt chẽ ngay từ gốc.</p>
                """
                + gallery(
                        img("https://images.unsplash.com/photo-1561070791-2526d30994b5?auto=format&fit=crop&w=1200&q=80", "Xây dựng bảng màu ngữ nghĩa và thang đo tỷ lệ chữ chuẩn mực"),
                        img("https://images.unsplash.com/photo-1581291518857-4e27b48ff24e?auto=format&fit=crop&w=1200&q=80", "Phác thảo cấu trúc thành phần giao diện tái sử dụng trên Figma"),
                        img("https://images.unsplash.com/photo-1558655146-9f40138edfeb?auto=format&fit=crop&w=1200&q=80", "Kiểm tra độ tương phản màu sắc cho cả chế độ Sáng và Tối"),
                        img("https://images.unsplash.com/photo-1542744094-3a31f272c490?auto=format&fit=crop&w=1200&q=80", "Không gian thảo luận nhóm ấm cúng tại studio thiết kế sáng tạo")
                ) + """
                <h2>Nội dung thực hành trọng tâm trong bốn giờ làm việc</h2>
                <p>Người tham gia sẽ trực tiếp chuẩn hóa bộ biến thiết kế cho thẻ bài viết, hộp thoại xem ảnh, khung bình luận lồng nhau và biểu đồ thống kê sao cho đạt tỷ lệ tương phản chuẩn WCAG AA trên cả nền sáng lẫn nền tối. Cuối buổi học, mỗi nhóm sẽ xuất bộ quy chuẩn thành cấu hình Tailwind CSS hoàn chỉnh có thể tích hợp ngay vào dự án Vue hoặc React thực tế.</p>
                """;

        return new PostSeed(
                "design-system-accessibility-workshop",
                "Workshop thực hành: Xây dựng Design System đa giao diện Sáng — Tối đạt chuẩn trợ năng WCAG",
                "Buổi thực hành chuyên sâu giúp UI/UX Designer và Frontend Engineer đồng bộ hóa Design Tokens từ Figma sang Tailwind CSS chuẩn tương phản.",
                content,
                "https://images.unsplash.com/photo-1561070791-2526d30994b5?auto=format&fit=crop&w=1200&q=80",
                "su-kien",
                "nguyenthutrang@gmail.com",
                List.of("ui-ux", "design-system", "tailwind", "workshop", "accessibility"),
                10.8003, 106.7118, "Không gian Sáng tạo Dreamplex, Điện Biên Phủ, Bình Thạnh, TP.HCM",
                1540, 63, 0, 11, 4, 4,
                List.of(
                        CommentSeed.of("buihoangyen@gmail.com",
                                "Chủ đề thiết thực quá Trang ơi! Cho mình hỏi người tham gia cần cài sẵn những plugin kiểm tra độ tương phản nào trên Figma trước khi tới buổi học không bạn?",
                                9, 140,
                                List.of(
                                        CommentSeed.of("nguyenthutrang@gmail.com",
                                                "Bạn chỉ cần mang laptop cá nhân cài sẵn Figma bản máy tính là đủ nha Yến, tệp mẫu và danh sách công cụ kiểm tra màu tụi mình đã chuẩn bị sẵn trong thư mục tài liệu chung rồi!",
                                                8, 134)
                                )
                        )
                )
        );
    }
}
