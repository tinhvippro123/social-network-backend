package com.vietblog.infrastructure.config.seed;

import com.vietblog.domain.entity.Report;
import com.vietblog.domain.entity.User;
import com.vietblog.infrastructure.config.seed.SeedModels.*;

import java.util.List;

/**
 * Danh mục hồ sơ thành viên, 8 nhóm cộng đồng chuyên biệt, hội thoại nhắn tin thời gian thực
 * và dữ liệu báo cáo kiểm duyệt chuẩn mực phục vụ trình diễn hệ thống.
 */
public final class CommunityAndSocialCatalog {

    private CommunityAndSocialCatalog() {}

    public static List<UserSeed> users() {
        return List.of(
                new UserSeed("admin@vietblog.com", "Quản trị viên Hệ thống",
                        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80",
                        "Ban điều hành nền tảng chia sẻ tri thức cộng đồng VietBlog. Phụ trách định hướng nội dung, an toàn cộng đồng và kết nối học thuật.",
                        User.Role.ADMIN, 27),

                new UserSeed("nguyenvanan@gmail.com", "Nguyễn Văn An",
                        "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=400&q=80",
                        "Kỹ sư phát triển Phần mềm Fullstack (Java Spring Boot & Vue 3). Yêu thích kiến trúc hệ thống thời gian thực, du lịch biển và góc làm việc tối giản.",
                        User.Role.USER, 25),

                new UserSeed("phamminhduc@gmail.com", "Phạm Minh Đức",
                        "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?auto=format&fit=crop&w=400&q=80",
                        "Senior Backend Architect chuyên sâu về Spring Boot, PostgreSQL và Clean Architecture. Điều phối viên nội dung kỹ thuật tại VietBlog.",
                        User.Role.MODERATOR, 23),

                new UserSeed("ngothanhlong@gmail.com", "Ngô Thành Long",
                        "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=400&q=80",
                        "Chuyên gia An toàn Ứng dụng Web (AppSec & OWASP). Quan tâm tới xác thực phi trạng thái, phòng chống XSS và bảo vệ hạ tầng API.",
                        User.Role.MODERATOR, 21),

                new UserSeed("trandinhtrong@gmail.com", "Trần Đình Trọng",
                        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=400&q=80",
                        "Kỹ sư DevOps & Cloud Infrastructure. Đam mê tự động hóa CI/CD, tối ưu container Docker và vận hành cụm cơ sở dữ liệu độ sẵn sàng cao.",
                        User.Role.USER, 18),

                new UserSeed("dangthimylinh@gmail.com", "Đặng Thị Mỹ Linh",
                        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=400&q=80",
                        "Kỹ sư Giao diện Frontend (Vue 3, TypeScript, Tailwind CSS). Theo đuổi trải nghiệm tương tác mượt mà và phương pháp quản trị tri thức cá nhân.",
                        User.Role.USER, 15),

                new UserSeed("nguyenthutrang@gmail.com", "Nguyễn Thu Trang",
                        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=400&q=80",
                        "Chuyên viên Thiết kế Sản phẩm số (UI/UX & Design Systems). Tập trung vào khả năng tiếp cận WCAG và trải nghiệm đọc thanh lịch.",
                        User.Role.USER, 13),

                new UserSeed("doanhkhoa@gmail.com", "Đỗ Anh Khoa",
                        "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&w=400&q=80",
                        "Nghiên cứu viên Xử lý Ngôn ngữ Tự nhiên (NLP tiếng Việt) & Khoa học Dữ liệu. Yêu thích các thuật toán đồ thị văn bản và tìm kiếm ngữ nghĩa.",
                        User.Role.USER, 10),

                new UserSeed("lehongnhung@gmail.com", "Lê Hồng Nhung",
                        "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=400&q=80",
                        "Cây viết Du lịch & Văn hóa bản địa. Đã đặt chân qua hơn 35 tỉnh thành Việt Nam để ghi lại những câu chuyện ẩm thực và di sản vùng miền.",
                        User.Role.USER, 8),

                new UserSeed("buihoangyen@gmail.com", "Bùi Hoàng Yến",
                        "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=400&q=80",
                        "Nhiếp ảnh gia Đời sống & Người yêu cà phê thủ công tại Sài Gòn. Thích khám phá những không gian tĩnh lặng tràn ngập ánh sáng tự nhiên.",
                        User.Role.USER, 5),

                new UserSeed("vuquoctuan@gmail.com", "Vũ Quốc Tuấn",
                        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=400&q=80",
                        "Sinh viên năm cuối Khoa Công nghệ Thông tin — Trường Đại học Tài nguyên và Môi trường TP.HCM (HCMUNRE). Thành viên tích cực CLB Học thuật CNTT.",
                        User.Role.USER, 3),

                new UserSeed("hoangdieuanh@gmail.com", "Hoàng Diệu Anh",
                        "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=400&q=80",
                        "Đối tác Phát triển Cộng đồng Công nghệ & Hướng nghiệp Kỹ sư trẻ. Cầu nối giữa các doanh nghiệp phần mềm và sinh viên các trường đại học.",
                        User.Role.USER, 2),

                new UserSeed("phanquocbao@gmail.com", "Phan Quốc Bảo",
                        "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=400&q=80",
                        "Kỹ sư Hệ thống Thông tin Địa lý (WebGIS & Bản đồ số). Đam mê ứng dụng dữ liệu không gian vào du lịch thông minh và quy hoạch đô thị.",
                        User.Role.USER, 0),

                new UserSeed("tranthaochi@gmail.com", "Trần Thảo Chi",
                        "https://images.unsplash.com/photo-1580489944761-15a19d654956?auto=format&fit=crop&w=400&q=80",
                        "Biên tập viên chuyên mục Đời sống xanh & Văn hóa đọc. Yêu những buổi sáng cuối tuần bên trang sách tại đường Nguyễn Văn Bình.",
                        User.Role.USER, 0)
        );
    }

    public static List<GroupSeed> groups() {
        List<String> coreTechMembers = List.of(
                "admin@vietblog.com", "nguyenvanan@gmail.com", "phamminhduc@gmail.com",
                "trandinhtrong@gmail.com", "dangthimylinh@gmail.com", "doanhkhoa@gmail.com",
                "vuquoctuan@gmail.com", "ngothanhlong@gmail.com", "phanquocbao@gmail.com"
        );

        List<String> lifestyleMembers = List.of(
                "lehongnhung@gmail.com", "buihoangyen@gmail.com", "nguyenthutrang@gmail.com",
                "dangthimylinh@gmail.com", "nguyenvanan@gmail.com", "vuquoctuan@gmail.com",
                "hoangdieuanh@gmail.com", "tranthaochi@gmail.com", "admin@vietblog.com"
        );

        return List.of(
                new GroupSeed(
                        "CLB Công nghệ Thông tin HCMUNRE",
                        "Không gian sinh hoạt học thuật, chia sẻ kinh nghiệm thực tập doanh nghiệp, nghiên cứu khoa học và kết nối các thế hệ sinh viên Khoa CNTT — Trường Đại học Tài nguyên và Môi trường TP.HCM.",
                        "https://images.unsplash.com/photo-1523240795612-9a054b0db644?auto=format&fit=crop&w=300&q=80",
                        "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?auto=format&fit=crop&w=1200&q=80",
                        "vuquoctuan@gmail.com", true, 24, coreTechMembers,
                        List.of(
                                new GroupPostSeed("vuquoctuan@gmail.com",
                                        "Tổng hợp biểu mẫu chuẩn bị quyển thuyết minh và slide báo cáo học kỳ này",
                                        "Chia sẻ bố cục chương mục rõ ràng từ khảo sát hiện trạng, phân tích thiết kế UML đến kết quả thực nghiệm.",
                                        "<p>Chào các bạn sinh viên khóa cuối Khoa CNTT! Ban học thuật CLB vừa rà soát và tổng hợp lại danh mục kiểm tra hình thức trình bày quyển báo cáo: từ cách đánh số hình vẽ, bảng biểu, trích dẫn tài liệu tham khảo chuẩn IEEE cho tới bố cục slide tóm tắt trong 15 phút.</p><p>Mọi người lưu ý phần sơ đồ kiến trúc tổng thể và sơ đồ cơ sở dữ liệu nên xuất ở định dạng vector hoặc ảnh độ phân giải cao để khi chiếu lên màn hình hội trường nhìn thật sắc nét nhé!</p>",
                                        "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?auto=format&fit=crop&w=1000&q=80",
                                        3,
                                        List.of(
                                                new GroupCommentSeed("nguyenvanan@gmail.com", "Cảm ơn ban học thuật CLB nhiều lắm! Đúng phần căn lề và đánh mục lục tự động mình đang cần rà lại lần cuối.", 48),
                                                new GroupCommentSeed("phanquocbao@gmail.com", "Nhắc thêm các bạn làm mảng bản đồ số nhớ chú thích rõ hệ tọa độ WGS84 trong phần mô tả dữ liệu không gian nha.", 36)
                                        )
                                ),
                                new GroupPostSeed("phanquocbao@gmail.com",
                                        "Gợi ý nguồn dữ liệu địa lý mở cho các đề tài WebGIS và Bản đồ tương tác",
                                        "Tổng hợp các lớp bản đồ nền OpenStreetMap, Leaflet và phương pháp tính khoảng cách Haversine trên mặt cầu.",
                                        "<p>Nhiều bạn trong khoa mình đang kết hợp tính năng tìm kiếm theo bán kính địa lý vào ứng dụng Web. Mình chia sẻ lại kinh nghiệm sử dụng công thức Haversine kết hợp đánh chỉ mục tọa độ giúp truy vấn các địa điểm lân cận trong phạm vi 5 đến 20 km cực kỳ nhẹ nhàng mà chưa cần cài đặt phần mở rộng phức tạp.</p>",
                                        "https://images.unsplash.com/photo-1524661135-423995f22d0b?auto=format&fit=crop&w=1000&q=80",
                                        7,
                                        List.of(
                                                new GroupCommentSeed("doanhkhoa@gmail.com", "Bài viết hữu ích quá Bảo ơi, kết hợp lọc bán kính địa lý với xếp hạng độ liên quan nội dung là ra bài toán gợi ý địa điểm cực chuẩn.", 90)
                                        )
                                )
                        )
                ),

                new GroupSeed(
                        "Spring Boot & Kiến trúc Hệ thống Việt Nam",
                        "Cộng đồng trao đổi chuyên sâu về Java 21, Spring Boot 3, Spring Security, tối ưu truy vấn Hibernate JPA, Redis Caching và thiết kế hệ thống phân tán.",
                        "https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=300&q=80",
                        "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=1200&q=80",
                        "phamminhduc@gmail.com", true, 26, coreTechMembers,
                        List.of(
                                new GroupPostSeed("phamminhduc@gmail.com",
                                        "Cách xử lý triệt để bài toán N+1 Query khi tải danh sách bài viết kèm tác giả và danh mục",
                                        "So sánh hiệu quả giữa JOIN FETCH trong JPQL, @EntityGraph và phép chiếu DTO trực tiếp.",
                                        "<p>Khi bật nhật ký SQL trên môi trường phát triển, không ít dự án bất ngờ khi thấy một trang danh sách 20 bài viết lại phát sinh tới hơn 40 câu truy vấn con để lấy thông tin tác giả và chuyên mục. Việc khai báo <code>@EntityGraph</code> đúng chỗ trên phương thức phân trang giúp gộp toàn bộ thành một câu truy vấn kết nối duy nhất, giảm thời gian phản hồi API xuống hơn ba lần.</p>",
                                        null,
                                        2,
                                        List.of(
                                                new GroupCommentSeed("trandinhtrong@gmail.com", "Chuẩn luôn anh Đức, kết hợp thêm chỉ mục kép trên cột (status, created_at DESC) bên PostgreSQL nữa là truy vấn trang chủ nhẹ tênh.", 30),
                                                new GroupCommentSeed("nguyenvanan@gmail.com", "Đối với bảng phụ @ElementCollection như tags thì dùng @BatchSize cũng giảm đáng kể số lượt gọi xuống DB đó anh.", 22)
                                        )
                                ),
                                new GroupPostSeed("ngothanhlong@gmail.com",
                                        "Kinh nghiệm sử dụng khóa Pessimistic Write khi cập nhật bộ đếm tương tác đồng thời",
                                        "Tránh hiện tượng mất lượt bình chọn khi nhiều người dùng thao tác cùng một thời điểm.",
                                        "<p>Trong các nghiệp vụ cộng trừ số lượt bình chọn hoặc duyệt trạng thái bài viết, nếu hai luồng cùng đọc giá trị cũ rồi ghi đè sẽ dẫn tới sai lệch số liệu thống kê. Sử dụng khóa hàng ngắn hạn ở tầng Repository trong phạm vi giao dịch hẹp giúp đảm bảo tính nhất quán tuyệt đối.</p>",
                                        null,
                                        6,
                                        List.of(
                                                new GroupCommentSeed("phamminhduc@gmail.com", "Rất đáng lưu tâm, nhất là khi kết hợp tính lại điểm xu hướng ngay sau mỗi lượt tương tác mới.", 80)
                                        )
                                )
                        )
                ),

                new GroupSeed(
                        "Frontend Master: Vue 3, TypeScript & Tailwind",
                        "Nơi giao lưu kỹ thuật xây dựng giao diện Web hiện đại với Vue 3 Composition API, Vite, Pinia, kiến trúc Design Token và tối ưu trải nghiệm người dùng.",
                        "https://images.unsplash.com/photo-1633356122544-f134324a6cee?auto=format&fit=crop&w=300&q=80",
                        "https://images.unsplash.com/photo-1507238691740-187a5b1d37b8?auto=format&fit=crop&w=1200&q=80",
                        "dangthimylinh@gmail.com", true, 21, coreTechMembers,
                        List.of(
                                new GroupPostSeed("dangthimylinh@gmail.com",
                                        "Tự động gom cụm ảnh liên tiếp trong bài viết thành bố cục lưới Collage mà không làm bẩn dữ liệu gốc",
                                        "Sử dụng DOMParser ở tầng hiển thị để biến đổi linh hoạt cụm 2, 3, 4 và 5+ ảnh kèm nhãn +N.",
                                        "<p>Thay vì lưu cứng các lớp CSS bố cục phức tạp vào cơ sở dữ liệu khiến nội dung khó chỉnh sửa về sau, việc giữ mã HTML bài viết ở dạng thẻ ảnh chuẩn rồi áp dụng hàm biến đổi nhẹ nhàng lúc hiển thị giúp giao diện vừa tự động dàn lưới nghệ thuật đẹp mắt, vừa tương thích hoàn toàn với trình xem ảnh phóng to toàn màn hình.</p>",
                                        "https://images.unsplash.com/photo-1507238691740-187a5b1d37b8?auto=format&fit=crop&w=1000&q=80",
                                        1,
                                        List.of(
                                                new GroupCommentSeed("nguyenthutrang@gmail.com", "Cách làm này giữ cho trình soạn thảo của người viết đơn giản tự nhiên mà lúc lên trang đọc vẫn sang xịn như tạp chí!", 18),
                                                new GroupCommentSeed("nguyenvanan@gmail.com", "Hay nhất là người viết muốn chèn cụm 5 ảnh ở đoạn mở đầu rồi chèn tiếp cụm 3 ảnh ở đoạn kết bài đều tự nhận diện riêng biệt.", 14)
                                        )
                                ),
                                new GroupPostSeed("nguyenthutrang@gmail.com",
                                        "Mẹo chọn độ xám nền tối không dùng màu đen tuyền #000000 để giảm hiện tượng nhòe chữ khi cuộn",
                                        "Sử dụng dải màu xám xanh than dịu mắt kết hợp viền mảnh bán trong suốt.",
                                        "<p>Trên các màn hình độ tương phản cao, chữ trắng tinh đặt trực tiếp trên nền đen tuyền thường gây lóa mắt khi đọc bài viết dài hơn năm phút. Chuyển nền chính sang tông xám than ấm và giảm độ chói của chữ nội dung xuống mức trắng ngà giúp mắt người đọc thư giãn hơn rất nhiều.</p>",
                                        null,
                                        5,
                                        List.of(
                                                new GroupCommentSeed("dangthimylinh@gmail.com", "Áp dụng bộ thông số màu Surface của Trang xong nhìn giao diện ban đêm sang hẳn lên!", 64)
                                        )
                                )
                        )
                ),

                new GroupSeed(
                        "Cộng đồng AI & Xử lý Ngôn ngữ Tự nhiên Tiếng Việt",
                        "Trao đổi học thuật và thực nghiệm về NLP tiếng Việt, tóm tắt văn bản tự động, phân tích quan điểm, tìm kiếm ngữ nghĩa TF-IDF / Vector và trích xuất đặc trưng ảnh.",
                        "https://images.unsplash.com/photo-1677442136019-21780ecad995?auto=format&fit=crop&w=300&q=80",
                        "https://images.unsplash.com/photo-1620712943543-bcc4688e7485?auto=format&fit=crop&w=1200&q=80",
                        "doanhkhoa@gmail.com", true, 19, coreTechMembers,
                        List.of(
                                new GroupPostSeed("doanhkhoa@gmail.com",
                                        "Chuẩn hóa Unicode dựng sẵn NFC trước khi tách từ tiếng Việt: Lỗi nhỏ nhưng ảnh hưởng lớn tới độ chính xác tìm kiếm",
                                        "Giải quyết triệt để tình trạng hai chuỗi tiếng Việt nhìn giống hệt nhau nhưng lệch mã byte dấu thanh.",
                                        "<p>Nhiều bộ gõ trên các hệ điều hành khác nhau sinh ra chuỗi tiếng Việt ở dạng tổ hợp NFD khiến phép so khớp từ điển dừng hoặc từ điển cảm xúc bị trượt hoàn toàn. Chỉ cần thêm một bước chuẩn hóa về dạng NFC ngay đầu đường ống xử lý văn bản, tỷ lệ bắt trúng từ khóa tiếng Việt tăng lên tức khắc.</p>",
                                        null,
                                        2,
                                        List.of(
                                                new GroupCommentSeed("vuquoctuan@gmail.com", "Đúng lỗi hôm trước em gặp khi nhập dữ liệu từ macOS sang Windows luôn anh Khoa, thêm Normalizer.Form.NFC cái là khớp 100%!", 28)
                                        )
                                ),
                                new GroupPostSeed("nguyenvanan@gmail.com",
                                        "Kết hợp biểu đồ màu HSV và đặc trưng cạnh cục bộ cho bài toán tìm kiếm bài viết bằng hình ảnh tương đồng",
                                        "Giải pháp gọn nhẹ chạy trực tiếp trên JVM không cần GPU chuyên dụng.",
                                        "<p>Đối với kho bài viết du lịch và ẩm thực vài nghìn bài, vector đặc trưng kết hợp phân bố màu sắc theo vùng lưới và độ sắc nét biên cạnh cho kết quả truy hồi các bài viết có khung cảnh biển xanh, đồi chè hoặc món ăn tương tự rất ấn tượng với thời gian tính toán dưới vài chục miligiây.</p>",
                                        "https://images.unsplash.com/photo-1555949963-ff9fe0c870eb?auto=format&fit=crop&w=1000&q=80",
                                        8,
                                        List.of(
                                                new GroupCommentSeed("doanhkhoa@gmail.com", "Chuẩn hóa vector về độ dài đơn vị rồi tính tích vô hướng Cosine là tối ưu nhất cho tập dữ liệu quy mô vừa.", 110)
                                        )
                                )
                        )
                ),

                new GroupSeed(
                        "DevOps, Cloud & Vận hành Hệ thống",
                        "Chia sẻ kinh nghiệm thực chiến về Docker, Kubernetes, cấu hình Nginx Reverse Proxy, bảo mật máy chủ Linux, giám sát hiệu năng và sao lưu cơ sở dữ liệu.",
                        "https://images.unsplash.com/photo-1607799279861-4dd421887fb3?auto=format&fit=crop&w=300&q=80",
                        "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&w=1200&q=80",
                        "trandinhtrong@gmail.com", true, 17, coreTechMembers,
                        List.of(
                                new GroupPostSeed("trandinhtrong@gmail.com",
                                        "Cấu hình Nginx chuyển tiếp mượt mà kết nối WebSocket STOMP không bị ngắt quãng sau 60 giây",
                                        "Lưu ý ba chỉ thị proxy_http_version 1.1, Upgrade header và proxy_read_timeout.",
                                        "<p>Mặc định Nginx sẽ đóng kết nối ngược nếu trong vòng 60 giây không có dữ liệu đi qua đường ống, khiến phiên chat thời gian thực của người dùng phải kết nối lại liên tục. Anh em nhớ đặt <code>proxy_read_timeout 3600s</code> kèm nhịp ping định kỳ từ phía client để giữ kênh truyền ổn định nhé.</p>",
                                        null,
                                        4,
                                        List.of(
                                                new GroupCommentSeed("phamminhduc@gmail.com", "Kinh nghiệm xương máu luôn, cảm ơn Trọng đã tổng hợp ngắn gọn cho anh em dễ tra cứu.", 56)
                                        )
                                )
                        )
                ),

                new GroupSeed(
                        "Hội Đam mê Xê dịch & Nhiếp ảnh Việt Nam",
                        "Cộng đồng dành cho những tâm hồn yêu vẻ đẹp thiên nhiên, văn hóa bản địa và nhiếp ảnh phong cảnh dọc chiều dài đất nước từ Hà Giang đến mũi Cà Mau.",
                        "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=300&q=80",
                        "https://images.unsplash.com/photo-1501785888041-af3ef285b470?auto=format&fit=crop&w=1200&q=80",
                        "lehongnhung@gmail.com", true, 16, lifestyleMembers,
                        List.of(
                                new GroupPostSeed("lehongnhung@gmail.com",
                                        "Cập nhật thời tiết Đà Lạt đầu tháng 11: Trời bắt đầu khô ráo, sáng sớm se lạnh cực hợp đi săn mây",
                                        "Gợi ý các khung giờ ánh sáng xiên đẹp nhất tại Cầu Đất, Đa Phú và đồi Thiên Phúc Đức.",
                                        "<p>Mấy hôm nay Đà Lạt đã dứt hẳn những cơn mưa rào chiều muộn, bầu trời ban ngày xanh ngắt và nhiệt độ sáng sớm dao động quanh mức 14 đến 15 độ C. Các bạn dự định lên chụp bộ ảnh mùa dã quỳ kết hợp săn mây cuối tuần này chuẩn bị áo len ấm là vừa đẹp nha!</p>",
                                        "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?auto=format&fit=crop&w=1000&q=80",
                                        1,
                                        List.of(
                                                new GroupCommentSeed("buihoangyen@gmail.com", "Nghe tin thời tiết đẹp mà nôn nao quá Nhung ơi, mình vừa vệ sinh xong bộ ống kính chuẩn bị tối thứ Sáu lên xe giường nằm đây!", 16),
                                                new GroupCommentSeed("tranthaochi@gmail.com", "Nhung cho mình hỏi mùa này hoa dã quỳ cung đường Tu Tra — Đơn Dương đã nở rộ chưa bạn?", 11)
                                        )
                                ),
                                new GroupPostSeed("buihoangyen@gmail.com",
                                        "Bí quyết giữ chi tiết vùng sáng đèn lồng khi chụp ảnh đêm tại Phố cổ Hội An",
                                        "Chụp ở khung giờ xanh Blue Hour lúc 17h30 giúp cân bằng hoàn hảo bầu trời và ánh đèn phố.",
                                        "<p>Thay vì đợi tới tối mịt khi nền trời đã đen kịt làm độ chênh sáng giữa đèn lồng và hậu cảnh quá gắt, mọi người hãy bấm máy trong khoảng hai mươi phút vàng ngay sau khi mặt trời vừa lặn. Lúc này bầu trời mang sắc xanh thẫm sâu thẳm tôn lên ánh vàng cam ấm áp của đèn lồng lụa cực kỳ hài hòa.</p>",
                                        "https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?auto=format&fit=crop&w=1000&q=80",
                                        6,
                                        List.of(
                                                new GroupCommentSeed("nguyenthutrang@gmail.com", "Mẹo chụp giờ xanh này áp dụng chụp kiến trúc thành phố lên đèn cũng đỉnh lắm luôn!", 72)
                                        )
                                )
                        )
                ),

                new GroupSeed(
                        "Góc Cà Phê Specialty & Không Gian Làm Việc",
                        "Nơi chia sẻ các quán cà phê yên tĩnh ngồi làm việc hiệu quả, trao đổi kỹ thuật pha chế Pour-over tại nhà và lối sống cân bằng lành mạnh.",
                        "https://images.unsplash.com/photo-1511920170033-f8396924c348?auto=format&fit=crop&w=300&q=80",
                        "https://images.unsplash.com/photo-1442512595331-e89e73853f31?auto=format&fit=crop&w=1200&q=80",
                        "buihoangyen@gmail.com", true, 14, lifestyleMembers,
                        List.of(
                                new GroupPostSeed("buihoangyen@gmail.com",
                                        "Công thức ủ Cold Brew hạt Arabica Cầu Đất thêm lát cam vàng thanh mát cho ngày nắng Sài Gòn",
                                        "Tỷ lệ 1 phần cà phê xay thô với 10 phần nước lọc ủ lạnh trong ngăn mát 16 tiếng.",
                                        "<p>Chỉ mất năm phút chuẩn bị tối hôm trước với bình thủy tinh kín nắp, sáng hôm sau lọc qua giấy phễu là bạn đã có ngay bình Cold Brew trong trẻo, ít vị đắng gắt, thoang thoảng hương trái cây chín để mang theo lên văn phòng hoặc giảng đường tỉnh táo suốt buổi sáng.</p>",
                                        "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=1000&q=80",
                                        3,
                                        List.of(
                                                new GroupCommentSeed("dangthimylinh@gmail.com", "Mình vừa làm thử mẻ đầu tiên cuối tuần rồi theo tỷ lệ 1:10 của Yến, thêm xíu vỏ cam vàng thơm dịu dễ uống cực kỳ!", 40)
                                        )
                                )
                        )
                ),

                new GroupSeed(
                        "UI/UX Design & Tư Duy Sản Phẩm Số",
                        "Diễn đàn dành cho các nhà thiết kế giao diện, nghiên cứu người dùng và quản lý sản phẩm cùng phân tích trải nghiệm thực tế trên các ứng dụng Việt.",
                        "https://images.unsplash.com/photo-1581291518857-4e27b48ff24e?auto=format&fit=crop&w=300&q=80",
                        "https://images.unsplash.com/photo-1531403009284-440f080d1e12?auto=format&fit=crop&w=1200&q=80",
                        "nguyenthutrang@gmail.com", true, 12, lifestyleMembers,
                        List.of(
                                new GroupPostSeed("nguyenthutrang@gmail.com",
                                        "Phân tích trải nghiệm cuộn tới bình luận mục tiêu khi người dùng nhấn từ chuông thông báo",
                                        "Kết hợp tự động mở rộng nhánh phản hồi ẩn và hiệu ứng nháy viền dịu mắt trong 2 giây.",
                                        "<p>Một chi tiết nhỏ nhưng quyết định sự tinh tế của sản phẩm cộng đồng là khi người dùng nhấp vào thông báo có ai đó vừa trả lời bình luận của họ, trang bài viết không chỉ mở ra ở đầu trang mà cần tự động mở nhánh hội thoại tương ứng, cuộn mượt tới đúng dòng bình luận đó và tô sáng nhẹ để mắt người đọc định vị được ngay lập tức.</p>",
                                        "https://images.unsplash.com/photo-1561070791-2526d30994b5?auto=format&fit=crop&w=1000&q=80",
                                        2,
                                        List.of(
                                                new GroupCommentSeed("nguyenvanan@gmail.com", "Đúng tinh thần lấy người dùng làm trung tâm luôn Trang ơi, chi tiết nhỏ mà dùng hằng ngày thấy tiện hơn hẳn!", 26),
                                                new GroupCommentSeed("hoangdieuanh@gmail.com", "Những điểm chạm vi tương tác được chăm chút kỹ như thế này luôn tạo thiện cảm rất lớn cho người trải nghiệm lần đầu.", 19)
                                        )
                                )
                        )
                )
        );
    }

    public static List<GroupChatSeed> groupChats() {
        return List.of(
                new GroupChatSeed(
                        "Ban Chủ nhiệm CLB CNTT HCMUNRE",
                        "vuquoctuan@gmail.com",
                        List.of("admin@vietblog.com", "nguyenvanan@gmail.com", "vuquoctuan@gmail.com",
                                "dangthimylinh@gmail.com", "doanhkhoa@gmail.com", "phanquocbao@gmail.com",
                                "hoangdieuanh@gmail.com"),
                        true,
                        List.of(
                                ChatMsgSeed.pinnedText("vuquoctuan@gmail.com",
                                        "Thông báo lịch sinh hoạt chuyên đề sáng Thứ Bảy tuần này lúc 8h30 tại phòng Hội thảo B204 cơ sở Lê Văn Sỹ: Chuyên đề Xử lý ngôn ngữ tự nhiên tiếng Việt và ứng dụng Bản đồ số nhé cả nhà!",
                                        320),
                                ChatMsgSeed.text("nguyenvanan@gmail.com",
                                        "Mình và anh Khoa đã kiểm tra xong máy chiếu với đầu chuyển HDMI ở phòng B204 chiều nay rồi, màu sắc lên rõ nét lắm Tuấn ơi.",
                                        295),
                                ChatMsgSeed.image("phanquocbao@gmail.com",
                                        "Gửi mọi người xem trước góc bàn trưng bày tài liệu học thuật tụi mình vừa sắp xếp thử chiều nay tại sảnh khoa nhé!",
                                        "https://images.unsplash.com/photo-1523240795612-9a054b0db644?auto=format&fit=crop&w=900&q=80",
                                        260),
                                ChatMsgSeed.reply("dangthimylinh@gmail.com",
                                        "Nhìn gọn gàng và chuyên nghiệp quá Bảo ơi! Sáng mai mình mang thêm hai tấm standee hướng dẫn quét mã QR tài liệu ra đặt hai bên nữa là chuẩn.",
                                        2, 230),
                                ChatMsgSeed.text("doanhkhoa@gmail.com",
                                        "Phần tài liệu thực hành thuật toán TextRank mình đã đóng gói sẵn bộ dữ liệu văn bản mẫu tiếng Việt để các bạn khóa dưới tải về chạy thử ngay tại chỗ.",
                                        140),
                                ChatMsgSeed.text("hoangdieuanh@gmail.com",
                                        "Bên đoàn anh chị cựu sinh viên và khách mời doanh nghiệp xác nhận sẽ có mặt lúc 8h15 để giao lưu đầu giờ cùng các bạn nhé!",
                                        65),
                                ChatMsgSeed.reply("admin@vietblog.com",
                                        "Cảm ơn Diệu Anh và các bạn trong Ban chủ nhiệm rất nhiều! Hẹn gặp đầy đủ mọi người sáng Thứ Bảy nhé.",
                                        5, 22)
                        )
                ),

                new GroupChatSeed(
                        "Nhóm Kỹ thuật Backend & Hạ tầng Hệ thống",
                        "phamminhduc@gmail.com",
                        List.of("phamminhduc@gmail.com", "nguyenvanan@gmail.com", "trandinhtrong@gmail.com",
                                "ngothanhlong@gmail.com", "doanhkhoa@gmail.com", "admin@vietblog.com"),
                        true,
                        List.of(
                                ChatMsgSeed.pinnedText("phamminhduc@gmail.com",
                                        "Quy ước chung: Tất cả các thay đổi liên quan tới truy vấn phân trang và cấu trúc bảng PostgreSQL đều cần kiểm tra kế hoạch thực thi EXPLAIN ANALYZE trước khi gộp nhánh nhé anh em.",
                                        480),
                                ChatMsgSeed.text("trandinhtrong@gmail.com",
                                        "Báo cáo anh em là cụm Redis Pub/Sub và PostgreSQL 16 sau khi tinh chỉnh HikariCP pool size = 20 đang giữ mức tiêu thụ RAM rất ổn định, độ trễ truy vấn trung bình dưới 4ms.",
                                        210),
                                ChatMsgSeed.text("ngothanhlong@gmail.com",
                                        "Mình cũng vừa rà soát lại bộ lọc làm sạch HTML đầu vào, các thẻ định dạng bài viết như pre, code, blockquote và cụm img đều giữ nguyên vẹn trong khi thẻ lạ bị loại bỏ hoàn toàn.",
                                        155),
                                ChatMsgSeed.reply("nguyenvanan@gmail.com",
                                        "Quá yên tâm luôn anh Long! Nhờ vậy mà bên trình soạn thảo chèn khối mã nguồn với lưới ảnh hoạt động trơn tru không bị lỗi hiển thị nào.",
                                        2, 95),
                                ChatMsgSeed.text("phamminhduc@gmail.com",
                                        "Cuối tuần này rảnh anh em mình làm buổi cà phê sáng tổng kết các bài học tối ưu hiệu năng vừa rồi thành chuỗi bài viết chia sẻ lên chuyên mục Công nghệ nhé!",
                                        35)
                        )
                ),

                new GroupChatSeed(
                        "Hội Săn Mây & Cà Phê Cuối Tuần",
                        "lehongnhung@gmail.com",
                        List.of("lehongnhung@gmail.com", "buihoangyen@gmail.com", "nguyenthutrang@gmail.com",
                                "dangthimylinh@gmail.com", "nguyenvanan@gmail.com", "tranthaochi@gmail.com"),
                        false,
                        List.of(
                                ChatMsgSeed.image("lehongnhung@gmail.com",
                                        "Tặng cả nhà tấm ảnh nắng sớm trên đồi chè Cầu Đất mình vừa lọc màu xong sáng nay nè!",
                                        "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?auto=format&fit=crop&w=900&q=80",
                                        380),
                                ChatMsgSeed.reply("buihoangyen@gmail.com",
                                        "Màu xanh đồi chè chuyển sang lớp sương mù dịu mắt quá Nhung ơi! Chụp bằng tiêu cự 50mm đúng không nàng?",
                                        0, 340),
                                ChatMsgSeed.text("lehongnhung@gmail.com",
                                        "Đúng rồi Yến ơi, khép khẩu f/5.6 lúc mặt trời vừa nhô qua rặng thông khoảng mười lăm phút là bắt được tia nắng xiên đẹp nhất.",
                                        310),
                                ChatMsgSeed.text("nguyenthutrang@gmail.com",
                                        "Chủ nhật tuần này hội mình hẹn nhau 8h30 sáng ở quán Specialty Coffee góc đường Phạm Ngọc Thạch vừa nhâm nhi Cold Brew vừa xem bộ ảnh mới của Nhung nha!",
                                        120),
                                ChatMsgSeed.text("tranthaochi@gmail.com",
                                        "Mình đăng ký một ghế cạnh cửa sổ nhé, tiện ghé đường sách Nguyễn Văn Bình ngay gần đó tìm vài cuốn tản văn mới luôn.",
                                        50)
                        )
                ),

                new GroupChatSeed(
                        "Ban Biên tập & Điều phối Nội dung VietBlog",
                        "admin@vietblog.com",
                        List.of("admin@vietblog.com", "phamminhduc@gmail.com", "ngothanhlong@gmail.com",
                                "hoangdieuanh@gmail.com", "nguyenthutrang@gmail.com"),
                        false,
                        List.of(
                                ChatMsgSeed.text("admin@vietblog.com",
                                        "Tuần này lượng bài viết chuyên sâu ở cả hai mảng Công nghệ và Du lịch — Ẩm thực đều tăng mạnh, thời gian đọc trung bình mỗi bài đạt trên bốn phút.",
                                        260),
                                ChatMsgSeed.text("hoangdieuanh@gmail.com",
                                        "Nhiều bạn đọc phản hồi rất thích tính năng tóm tắt nhanh ý chính đầu bài và bản đồ địa điểm thực tế gắn kèm các bài review vùng miền.",
                                        190),
                                ChatMsgSeed.text("ngothanhlong@gmail.com",
                                        "Trong hàng đợi kiểm duyệt hiện còn 3 báo cáo mới từ cộng đồng gửi về, chiều nay mình với Đức sẽ xem xét kỹ ngữ cảnh từng bài để phản hồi công tâm nhất.",
                                        75)
                        )
                )
        );
    }

    public static List<DirectChatSeed> directChats() {
        return List.of(
                new DirectChatSeed("nguyenvanan@gmail.com", "phamminhduc@gmail.com",
                        "Anh Đức Backend", true, 0,
                        List.of(
                                ChatMsgSeed.text("nguyenvanan@gmail.com",
                                        "Anh Đức ơi, em vừa đọc kỹ bài phân tích Clean Architecture của anh, cách đặt công thức Wilson Lower Bound tách riêng trong DomainService nhìn sáng sủa quá ạ.",
                                        240),
                                ChatMsgSeed.text("phamminhduc@gmail.com",
                                        "Cảm ơn An nhé! Bài viết về WebSocket STOMP kết hợp Redis Pub/Sub của em tuần này cũng được anh em trong nhóm Backend chia sẻ lại rất nhiều đó.",
                                        185),
                                ChatMsgSeed.reply("nguyenvanan@gmail.com",
                                        "Dạ nhờ lúc làm thực tế gặp bài toán đồng bộ trạng thái đã xem giữa nhiều tab trình duyệt nên em tổng hợp lại luôn cho mọi người đỡ vất vả giống mình ạ.",
                                        1, 120),
                                ChatMsgSeed.text("phamminhduc@gmail.com",
                                        "Phát huy tinh thần viết kỹ thuật từ trải nghiệm thực chiến như vậy nhé, đọc khác hẳn những bài dịch lý thuyết khô khan!",
                                        45)
                        )
                ),

                new DirectChatSeed("nguyenvanan@gmail.com", "dangthimylinh@gmail.com",
                        "Mỹ Linh VueJS", false, 0,
                        List.of(
                                ChatMsgSeed.text("dangthimylinh@gmail.com",
                                        "An ơi, mình vừa kiểm tra giao diện lưới ảnh Collage trên cả màn hình điện thoại dọc và màn hình rộng 27 inch, tỷ lệ khung hình co giãn mượt lắm!",
                                        195),
                                ChatMsgSeed.image("dangthimylinh@gmail.com",
                                        "Góc làm việc sáng nay của mình học theo bài công thái học của An, kê cao màn hình lên đỡ mỏi cổ hẳn luôn nè!",
                                        "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=900&q=80",
                                        160),
                                ChatMsgSeed.reply("nguyenvanan@gmail.com",
                                        "Góc bàn gọn gàng và sang quá Linh ơi! Thêm chậu cây nhỏ bên trái nữa là làm việc cả ngày không chán luôn.",
                                        1, 90)
                        )
                ),

                new DirectChatSeed("nguyenvanan@gmail.com", "doanhkhoa@gmail.com",
                        "Anh Khoa NLP", false, 1,
                        List.of(
                                ChatMsgSeed.text("nguyenvanan@gmail.com",
                                        "Khoa ơi, bộ tách câu của thuật toán TextRank hiện tại xử lý rất tốt các đoạn văn có dấu chấm kết câu rõ ràng rồi đó bạn.",
                                        150),
                                ChatMsgSeed.text("doanhkhoa@gmail.com",
                                        "Ừ An, mình cũng vừa kiểm tra lại bộ từ điển phủ định tiếng Việt cho phần thống kê cảm xúc bình luận, các cụm như 'chưa sạch' hay 'không mượt' đều bắt chính xác 100%.",
                                        60),
                                ChatMsgSeed.text("doanhkhoa@gmail.com",
                                        "Chiều thứ Bảy sinh hoạt CLB ở Lê Văn Sỹ xong rảnh ghé quán cà phê gần trường trao đổi thêm về phần gợi ý bài viết liên quan nhé!",
                                        12)
                        )
                ),

                new DirectChatSeed("nguyenvanan@gmail.com", "trandinhtrong@gmail.com",
                        "Trọng DevOps", false, 0,
                        List.of(
                                ChatMsgSeed.text("trandinhtrong@gmail.com",
                                        "An ơi, kịch bản sao lưu tự động cơ sở dữ liệu PostgreSQL lúc 0h00 hằng đêm mình đã kiểm thử khôi phục thành công trên môi trường staging rồi nhé.",
                                        300),
                                ChatMsgSeed.text("nguyenvanan@gmail.com",
                                        "Tuyệt vời quá Trọng, có bản sao lưu định kỳ tự động vậy vận hành hệ thống yên tâm hơn hẳn. Tối thứ Năm tuần này ra sân cầu lông Tân Bình như lịch cũ nha!",
                                        220),
                                ChatMsgSeed.text("trandinhtrong@gmail.com",
                                        "Chốt luôn bạn ơi, 19h30 sân số 3 nhé!",
                                        180)
                        )
                ),

                new DirectChatSeed("nguyenvanan@gmail.com", "buihoangyen@gmail.com",
                        null, false, 0,
                        List.of(
                                ChatMsgSeed.text("nguyenvanan@gmail.com",
                                        "Chào Yến, mình vừa lưu lại bài tổng hợp quán cà phê Specialty Quận 3 của bạn. Cho mình hỏi quán ở khu Hồ Con Rùa gửi xe máy có thuận tiện không bạn?",
                                        260),
                                ChatMsgSeed.text("buihoangyen@gmail.com",
                                        "Chào An nha! Ngay đầu hẻm có bãi giữ xe có mái che của tòa nhà luôn, bảo vệ dắt xe nhiệt tình và mở tới 22h tối nên yên tâm ngồi làm việc thoải mái nhé.",
                                        210)
                        )
                ),

                new DirectChatSeed("admin@vietblog.com", "nguyenvanan@gmail.com",
                        null, true, 0,
                        List.of(
                                ChatMsgSeed.text("admin@vietblog.com",
                                        "Chào An, Ban biên tập rất ấn tượng với hai bài viết về Kiến trúc WebSocket thời gian thực và Góc làm việc công thái học của bạn trong tuần qua.",
                                        280),
                                ChatMsgSeed.text("nguyenvanan@gmail.com",
                                        "Cảm ơn Ban quản trị đã động viên ạ! Mình rất vui khi các kinh nghiệm thực tế nhận được nhiều phản hồi tích cực và thảo luận sâu từ mọi người.",
                                        210),
                                ChatMsgSeed.text("admin@vietblog.com",
                                        "Bài viết của bạn đã được chọn đưa vào danh sách nổi bật đầu trang tuần này, mong chờ thêm nhiều bài chia sẻ chất lượng từ An trong thời gian tới nhé!",
                                        130)
                        )
                ),

                new DirectChatSeed("admin@vietblog.com", "phamminhduc@gmail.com",
                        "Đức Moderator", true, 0,
                        List.of(
                                ChatMsgSeed.text("phamminhduc@gmail.com",
                                        "Báo cáo quản trị viên, tuần này các thảo luận kỹ thuật trong chuyên mục Công nghệ diễn ra rất sôi nổi và văn minh, tỷ lệ bình luận tích cực chiếm đa số.",
                                        175),
                                ChatMsgSeed.text("admin@vietblog.com",
                                        "Cảm ơn Đức đã sát sao hỗ trợ điều phối nội dung nhé! Với các báo cáo phân loại sai chuyên mục, mình ưu tiên nhắc nhở nhẹ nhàng và hỗ trợ tác giả chuyển đúng danh mục.",
                                        110)
                        )
                ),

                new DirectChatSeed("admin@vietblog.com", "hoangdieuanh@gmail.com",
                        null, false, 1,
                        List.of(
                                ChatMsgSeed.text("hoangdieuanh@gmail.com",
                                        "Chào Ban quản trị, bài thông tin về Ngày hội Công nghệ HCMUNRE TechDay 2026 đang thu hút rất nhiều bạn sinh viên vào hỏi thông tin gian hàng và lịch tọa đàm.",
                                        95),
                                ChatMsgSeed.text("hoangdieuanh@gmail.com",
                                        "Mình vừa gửi bổ sung danh sách các chuyên gia tham gia bàn tròn hướng nghiệp buổi sáng, nhờ Ban quản trị ghim bài sự kiện thêm tới hết tuần sau nhé!",
                                        18)
                        )
                )
        );
    }

    public static List<ReportSeed> reports() {
        return List.of(
                new ReportSeed(
                        Report.TargetType.POST,
                        "ban-do-ca-phe-specialty-saigon",
                        "phanquocbao@gmail.com",
                        "Đề xuất chuyển sang chuyên mục Đời sống",
                        "Bài viết tập trung nhiều vào tiêu chí chọn bàn ghế, ổ điện và không gian ngồi làm việc cho dân văn phòng hơn là công thức ẩm thực thuần túy, nhờ ban điều hành xem xét phân loại chuyên mục phù hợp.",
                        Report.ReportStatus.PENDING,
                        null,
                        5
                ),
                new ReportSeed(
                        Report.TargetType.COMMENT,
                        "van-hoa-pho-ha-noi-buoi-som",
                        "lehongnhung@gmail.com",
                        "Nhận xét quy chụp thiếu khách quan",
                        "Bình luận đánh đồng không gian các quán phở truyền thống khu phố cổ chưa sạch dễ gây ảnh hưởng không tốt tới những cơ sở lâu năm đã cải tạo khang trang.",
                        Report.ReportStatus.PENDING,
                        null,
                        9
                ),
                new ReportSeed(
                        Report.TargetType.POST,
                        "jwt-security-rate-limiting",
                        "vuquoctuan@gmail.com",
                        "Cần bổ sung lưu ý cấu hình Reverse Proxy",
                        "Đề xuất tác giả ghi chú rõ hơn việc chỉ tin cậy header X-Forwarded-For từ dải IP nội bộ để người mới đọc không cấu hình sơ hở trên máy chủ thực tế.",
                        Report.ReportStatus.PENDING,
                        null,
                        14
                ),
                new ReportSeed(
                        Report.TargetType.POST,
                        "cung-duong-bien-da-nang-son-tra",
                        "trandinhtrong@gmail.com",
                        "Kiểm tra thông tin quy định phương tiện lên bán đảo",
                        "Nhờ ban biên tập xác minh lại mốc trạm gác hạn chế xe tay ga trên tuyến đường lên đỉnh Bàn Cờ để du khách nắm chính xác.",
                        Report.ReportStatus.RESOLVED,
                        "Đã đối chiếu quy định hiện hành của Ban quản lý Bán đảo Sơn Trà: nội dung cảnh báo xe tay ga trong bài viết hoàn toàn chính xác và hữu ích cho du khách.",
                        48
                ),
                new ReportSeed(
                        Report.TargetType.COMMENT,
                        "clean-architecture-spring-boot",
                        "dangthimylinh@gmail.com",
                        "Tranh luận gay gắt về độ phức tạp kiến trúc",
                        "Nhờ điều phối viên theo dõi luồng nhận xét trái chiều về chi phí viết lớp chuyển đổi DTO trong dự án quy mô nhỏ.",
                        Report.ReportStatus.DISMISSED,
                        "Nội dung bình luận phản biện kỹ thuật hoàn toàn lịch sự, đúng tinh thần trao đổi học thuật đa chiều của cộng đồng.",
                        72
                ),
                new ReportSeed(
                        Report.TargetType.POST,
                        "vietnam-web-ai-summit",
                        "buihoangyen@gmail.com",
                        "Xác nhận địa điểm tổ chức sự kiện",
                        "Nhờ kiểm tra lại thông tin cổng gửi xe khu triển lãm SECC Quận 7 để người tham dự di chuyển thuận lợi.",
                        Report.ReportStatus.RESOLVED,
                        "Đã xác thực thông tin địa điểm và thời gian tổ chức sự kiện chính xác theo thông cáo của Ban tổ chức.",
                        96
                )
        );
    }
}

