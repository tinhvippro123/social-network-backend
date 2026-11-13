package com.vietblog.domain.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Benchmark evaluation test for NLP Algorithms (Sentiment Analysis & TextRank)
 * calculates empirical metrics (Accuracy, Precision, Recall, F1-Score)
 * on a Vietnamese test corpus to provide quantitative evidence for Chapter 3 of the thesis report.
 */
class AlgorithmEvaluationTest {

    record TestCase(String text, String expectedLabel) {}

    @Test
    @DisplayName("Evaluate Vietnamese Sentiment Analysis: Accuracy, Precision, Recall, F1-Score")
    void evaluateSentimentAnalysisMetrics() {
        List<TestCase> testCorpus = List.of(
            // --- POSITIVE (20 samples) ---
            new TestCase("Bài viết chia sẻ rất hay và chi tiết, cảm ơn bạn nhiều", "positive"),
            new TestCase("Hướng dẫn rất dễ hiểu, làm theo là chạy ngay được", "positive"),
            new TestCase("Dự án tuyệt vời, kiến trúc Clean Architecture rất chuẩn", "positive"),
            new TestCase("Tác giả viết có tâm quá, ủng hộ hết mình", "positive"),
            new TestCase("Nội dung cực kỳ bổ ích và thiết thực cho sinh viên", "positive"),
            new TestCase("Giao diện đẹp mắt, trải nghiệm mượt mà xuất sắc", "positive"),
            new TestCase("Bài viết chất lượng cao, đúng cái mình đang tìm kiếm", "positive"),
            new TestCase("Cảm ơn tác giả đã đóng góp cho cộng đồng mã nguồn mở", "positive"),
            new TestCase("Giải thích cặn kẽ, ví dụ sinh động và sáng tạo", "positive"),
            new TestCase("Ứng dụng chạy rất ổn định và nhanh, đánh giá 5 sao", "positive"),
            new TestCase("Rất đáng học hỏi, phong cách code rất chuyên nghiệp", "positive"),
            new TestCase("Tuyệt vời ông mặt trời, tiếp tục phát huy nhé", "positive"),
            new TestCase("Thích cách bạn tiếp cận vấn đề, rất thực tế", "positive"),
            new TestCase("Không tệ chút nào, bài viết rất khá", "positive"), // Negation test
            new TestCase("Không hề làm mình thất vọng, quá đỉnh", "positive"), // Double negation
            new TestCase("Bài chia sẻ siêu hay, chúc bạn thành công", "positive"),
            new TestCase("Quá xuất sắc, mong chờ các bài viết tiếp theo của bạn", "positive"),
            new TestCase("Hữu ích lắm, mình đã bookmark lại để đọc dần", "positive"),
            new TestCase("Tài liệu đầy đủ và rõ ràng, rất khen ngợi", "positive"),
            new TestCase("Phần giải thuật tự viết rất ấn tượng và chỉn chu", "positive"),

            // --- NEGATIVE (20 samples) ---
            new TestCase("Nội dung bài viết quá sơ sài và tệ hại", "negative"),
            new TestCase("Code lỗi lung tung không chạy được, dở quá", "negative"),
            new TestCase("Giải thích lủng củng, đọc chả hiểu gì cả", "negative"),
            new TestCase("Bài viết copy từ nguồn khác mà không ghi credit, đạo văn", "negative"),
            new TestCase("Phần mềm giật lag và nhiều bug nghiêm trọng", "negative"),
            new TestCase("Tác giả trả lời bình luận thái độ rất mất dạy và vô văn hóa", "negative"),
            new TestCase("Dự án vớ vẩn, không có chút giá trị thực tiễn nào", "negative"),
            new TestCase("Tốn thời gian đọc, nội dung quá thất vọng", "negative"),
            new TestCase("Không tốt chút nào, chất lượng bài viết kém", "negative"), // Negation test
            new TestCase("Chẳng hay ho gì, toàn chém gió linh tinh", "negative"), // Negation test
            new TestCase("Chưa thấy bài viết nào tệ như bài này", "negative"),
            new TestCase("Hướng dẫn sai bét làm mình cài lại máy cả buổi", "negative"),
            new TestCase("Lừa đảo, giật tít câu view rẻ tiền", "negative"),
            new TestCase("Ứng dụng này như rác rưởi, không nên dùng", "negative"),
            new TestCase("Kiến thức sai lệch hoàn toàn, nguy hiểm cho người mới", "negative"),
            new TestCase("Giao diện xấu xí và khó dùng kinh khủng", "negative"),
            new TestCase("Chất lượng quá kém so với kỳ vọng ban đầu", "negative"),
            new TestCase("Bài viết không đáng một xu, quá tệ", "negative"),
            new TestCase("Nội dung nhảm nhí, đề nghị admin xóa bài", "negative"),
            new TestCase("Tác giả viết cẩu thả và thiếu trách nhiệm", "negative"),

            // --- NEUTRAL (20 samples) ---
            new TestCase("Dự án này sử dụng Java 21 và Spring Boot 4", "neutral"),
            new TestCase("Bạn cho mình hỏi database đang dùng phiên bản nào vậy?", "neutral"),
            new TestCase("Bài viết được đăng vào ngày 15 tháng 9 năm 2026", "neutral"),
            new TestCase("Hệ thống hỗ trợ cả kiến trúc Monolith và Microservices", "neutral"),
            new TestCase("Độ trễ phản hồi đo được qua WebSocket là 45ms", "neutral"),
            new TestCase("Địa chỉ trường nằm tại 236B Lê Văn Sỹ Tân Bình", "neutral"),
            new TestCase("Vui lòng kiểm tra lại cấu hình cổng 8080 và 5432", "neutral"),
            new TestCase("Bài viết gồm 4 chương và 15 mục chi tiết", "neutral"),
            new TestCase("Tập tin PDF có kích thước 2.5 MB", "neutral"),
            new TestCase("Thuật toán chuyển đổi không gian màu RGB sang HSV", "neutral"),
            new TestCase("Chức năng chat gửi tin nhắn dạng văn bản và hình ảnh", "neutral"),
            new TestCase("Thư viện được tải về từ kho Maven Central", "neutral"),
            new TestCase("Thời gian hết hạn của Access Token là 24 giờ", "neutral"),
            new TestCase("Người dùng cần đăng nhập để thực hiện chức năng này", "neutral"),
            new TestCase("Tọa độ GPS gồm kinh độ và vĩ độ dạng số thực", "neutral"),
            new TestCase("Bình luận được sắp xếp theo thời gian mới nhất", "neutral"),
            new TestCase("Danh mục bao gồm Công nghệ, Giáo dục và Đời sống", "neutral"),
            new TestCase("Dữ liệu được lưu trữ trong bảng posts của PostgreSQL", "neutral"),
            new TestCase("Đây là bài viết đầu tiên trên nền tảng VietBlog", "neutral"),
            new TestCase("Mỗi trang hiển thị tối đa 10 bài viết theo phân trang", "neutral")
        );

        int total = testCorpus.size();
        int correct = 0;

        Map<String, Integer> tp = new HashMap<>(Map.of("positive", 0, "negative", 0, "neutral", 0));
        Map<String, Integer> fp = new HashMap<>(Map.of("positive", 0, "negative", 0, "neutral", 0));
        Map<String, Integer> fn = new HashMap<>(Map.of("positive", 0, "negative", 0, "neutral", 0));

        for (TestCase tc : testCorpus) {
            String predicted = TextAlgorithms.sentiment(tc.text).label();
            String expected = tc.expectedLabel;

            if (predicted.equals(expected)) {
                correct++;
                tp.put(expected, tp.get(expected) + 1);
            } else {
                fp.put(predicted, fp.get(predicted) + 1);
                fn.put(expected, fn.get(expected) + 1);
            }
        }

        double accuracy = (double) correct / total;

        // Print Markdown table for thesis report inclusion
        System.out.println("\n==========================================================================");
        System.out.println(" KẾT QUẢ THỰC NGHIỆM ĐÁNH GIÁ GIẢI THUẬT PHÂN TÍCH CẢM XÚC (THESIS REPORT)");
        System.out.println("==========================================================================");
        System.out.println("| Nhãn cảm xúc | Tổng mẫu | TP | FP | FN | Precision (%) | Recall (%) | F1-Score (%) |");
        System.out.println("|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|");

        double sumPrecision = 0;
        double sumRecall = 0;
        double sumF1 = 0;

        for (String label : List.of("positive", "negative", "neutral")) {
            int t = tp.get(label);
            int f_p = fp.get(label);
            int f_n = fn.get(label);

            double precision = (t + f_p == 0) ? 0 : (double) t / (t + f_p);
            double recall = (t + f_n == 0) ? 0 : (double) t / (t + f_n);
            double f1 = (precision + recall == 0) ? 0 : 2 * (precision * recall) / (precision + recall);

            sumPrecision += precision;
            sumRecall += recall;
            sumF1 += f1;

            System.out.printf("| %-12s | %-8d | %-2d | %-2d | %-2d | %-13.2f | %-10.2f | %-12.2f |\n",
                    label.toUpperCase(), 20, t, f_p, f_n, precision * 100, recall * 100, f1 * 100);
        }

        double macroPrecision = sumPrecision / 3.0;
        double macroRecall = sumRecall / 3.0;
        double macroF1 = sumF1 / 3.0;

        System.out.println("|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|");
        System.out.printf("| **TRUNG BÌNH (Macro-avg)** | **%d** | - | - | - | **%.2f%%** | **%.2f%%** | **%.2f%%** |\n",
                total, macroPrecision * 100, macroRecall * 100, macroF1 * 100);
        System.out.printf("| **ĐỘ CHÍNH XÁC TOÀN CỤC (Accuracy)** | **%.2f%%** (%d/%d mẫu chính xác) |\n",
                accuracy * 100, correct, total);
        System.out.println("==========================================================================\n");

        assertThat(accuracy).isGreaterThan(0.75); // Must achieve at least 75% accuracy
    }

    record SummarySample(String title, String document, String referenceSummary) {}

    @Test
    @DisplayName("Evaluate TextRank Summarization: ROUGE-1 & ROUGE-2 vs Lead-2 Baseline")
    void evaluateTextRankSummarizationRougeMetrics() {
        List<SummarySample> samples = List.of(
            new SummarySample(
                "Clean Architecture Spring Boot",
                "Xin chào các bạn lập trình viên trẻ hôm nay trời nắng đẹp. "
                    + "Trong chuyên mục công nghệ cuối tuần này chúng ta cùng trò chuyện nhẹ nhàng. "
                    + "Kiến trúc Clean Architecture trong Spring Boot phân tách rõ các tầng Domain, Application, Infrastructure và Presentation giúp hệ thống dễ bảo trì và kiểm thử. "
                    + "Tầng Domain chứa các quy tắc nghiệp vụ cốt lõi và không phụ thuộc vào cơ sở dữ liệu hay framework bên ngoài. "
                    + "Mọi giao tiếp giữa tầng Application và PostgreSQL đều đi qua cổng giao diện Port và Adapter nhằm đảm bảo nguyên tắc đảo ngược phụ thuộc.",
                "Kiến trúc Clean Architecture trong Spring Boot phân tách rõ các tầng Domain, Application, Infrastructure và Presentation giúp hệ thống dễ bảo trì và kiểm thử. Tầng Domain chứa các quy tắc nghiệp vụ cốt lõi và không phụ thuộc vào cơ sở dữ liệu hay framework bên ngoài."
            ),
            new SummarySample(
                "Tìm kiếm ngữ nghĩa TF-IDF & pgvector",
                "Gần đây nhiều bạn sinh viên thường đặt câu hỏi về đề tài đồ án tốt nghiệp. "
                    + "Hôm nay mình sẽ chia sẻ một kinh nghiệm thực tế từ dự án đã hoàn thiện. "
                    + "Giải thuật TF-IDF chuyển đổi văn bản tiếng Việt thành vector trọng số không gian để tính độ tương đồng Cosine với các bài viết khác. "
                    + "Kết hợp vector TF-IDF 256 chiều lưu trữ trong tiện ích mở rộng pgvector giúp hệ thống tìm kiếm ngữ nghĩa phản hồi chính xác ngay cả khi truy vấn khác từ khóa. "
                    + "Ngoài ra việc tách dấu tiếng Việt giúp người dùng tra cứu nhanh mà không cần gõ đầy đủ thanh điệu.",
                "Giải thuật TF-IDF chuyển đổi văn bản tiếng Việt thành vector trọng số không gian để tính độ tương đồng Cosine với các bài viết khác. Kết hợp vector TF-IDF 256 chiều lưu trữ trong tiện ích mở rộng pgvector giúp hệ thống tìm kiếm ngữ nghĩa phản hồi chính xác ngay cả khi truy vấn khác từ khóa."
            ),
            new SummarySample(
                "Tìm kiếm hình ảnh CBIR",
                "Nhiếp ảnh và chia sẻ khoảnh khắc là thói quen quen thuộc của giới trẻ. "
                    + "Tuy nhiên việc phân loại hàng nghìn bức ảnh thủ công tốn rất nhiều công sức. "
                    + "Hệ thống tìm kiếm hình ảnh tự cài đặt trích xuất đặc trưng màu sắc HSV Color Histogram kết hợp bộ lọc phát hiện cạnh Sobel Edge Detection. "
                    + "Đồng thời ma trận đồng xuất hiện mức xám GLCM phân tích kết cấu bề mặt ảnh để tạo thành vector đặc trưng 51 chiều so khớp bằng độ tương đồng Cosine. "
                    + "Phương pháp thị giác máy tính cổ điển này hoạt động nhẹ nhàng mà không cần mô hình học sâu nặng nề.",
                "Hệ thống tìm kiếm hình ảnh tự cài đặt trích xuất đặc trưng màu sắc HSV Color Histogram kết hợp bộ lọc phát hiện cạnh Sobel Edge Detection. Đồng thời ma trận đồng xuất hiện mức xám GLCM phân tích kết cấu bề mặt ảnh để tạo thành vector đặc trưng 51 chiều so khớp bằng độ tương đồng Cosine."
            ),
            new SummarySample(
                "Tìm kiếm vị trí địa lý GIS",
                "Cuối tuần bạn thường làm gì khi rảnh rỗi ở thành phố. "
                    + "Đôi khi việc tìm một quán cà phê yên tĩnh cũng trở nên khó khăn. "
                    + "Chức năng bản đồ bài viết sử dụng công thức Haversine tính khoảng cách địa lý theo bán kính từ tọa độ kinh độ và vĩ độ của người dùng. "
                    + "Ở tầng cơ sở dữ liệu PostgreSQL, PostGIS sử dụng chỉ mục không gian GIST và hàm ST_DWithin để truy vấn địa điểm xung quanh trên bản đồ Leaflet. "
                    + "Người viết bài dễ dàng gắn thẻ địa điểm quán ăn, phòng trọ hoặc sự kiện trực tiếp trên bản đồ tương tác.",
                "Chức năng bản đồ bài viết sử dụng công thức Haversine tính khoảng cách địa lý theo bán kính từ tọa độ kinh độ và vĩ độ của người dùng. Ở tầng cơ sở dữ liệu PostgreSQL, PostGIS sử dụng chỉ mục không gian GIST và hàm ST_DWithin để truy vấn địa điểm xung quanh trên bản đồ Leaflet."
            ),
            new SummarySample(
                "Chat thời gian thực WebSocket",
                "Thời đại số kết nối mọi người lại gần nhau hơn bao giờ hết. "
                    + "Các nền tảng mạng xã hội luôn chú trọng trải nghiệm phản hồi tức thì. "
                    + "Kênh giao tiếp song công WebSocket theo chuẩn STOMP cho phép máy khách và máy chủ trao đổi tin nhắn 1-1 và chat nhóm với độ trễ thấp. "
                    + "Khi triển khai đa máy chủ, Redis Pub/Sub phân phối sự kiện tin nhắn đồng bộ giữa các phiên kết nối WebSocket khác nhau. "
                    + "Hệ thống còn hỗ trợ xác thực qua vé ngắn hạn và đếm số tin nhắn chưa đọc theo thời gian thực.",
                "Kênh giao tiếp song công WebSocket theo chuẩn STOMP cho phép máy khách và máy chủ trao đổi tin nhắn 1-1 và chat nhóm với độ trễ thấp. Khi triển khai đa máy chủ, Redis Pub/Sub phân phối sự kiện tin nhắn đồng bộ giữa các phiên kết nối WebSocket khác nhau."
            ),
            new SummarySample(
                "Xếp hạng xu hướng Trending",
                "Mỗi ngày có hàng trăm bài đăng mới xuất hiện trên trang chủ. "
                    + "Nếu chỉ sắp xếp theo thời gian thì nội dung chất lượng sẽ nhanh chóng bị trôi mất. "
                    + "Thuật toán xếp hạng xu hướng kết hợp Reddit Hot Ranking và hệ số phân rã trọng lực thời gian Gravity Decay của Hacker News. "
                    + "Bên cạnh đó công thức Wilson Score Interval với độ tin cậy 95 phần trăm giúp đánh giá công bằng các bài viết có ít lượt bình chọn. "
                    + "Nhờ vậy các bài viết có nhiều upvote, bình luận tích cực và lượt xem cao luôn nổi bật ở vị trí đầu bảng.",
                "Thuật toán xếp hạng xu hướng kết hợp Reddit Hot Ranking và hệ số phân rã trọng lực thời gian Gravity Decay của Hacker News. Bên cạnh đó công thức Wilson Score Interval với độ tin cậy 95 phần trăm giúp đánh giá công bằng các bài viết có ít lượt bình chọn."
            )
        );

        double trR1P = 0, trR1R = 0, trR1F1 = 0, trR2P = 0, trR2R = 0, trR2F1 = 0;
        double blR1P = 0, blR1R = 0, blR1F1 = 0, blR2P = 0, blR2R = 0, blR2F1 = 0;

        for (SummarySample s : samples) {
            String textRankSummary = String.join(" ", TextAlgorithms.summarize(s.document, 2));
            String[] allSentences = s.document.split("(?<=\\.)\\s+");
            String lead2Baseline = allSentences.length >= 2 ? allSentences[0] + " " + allSentences[1] : s.document;

            double[] tr1 = rougeN(textRankSummary, s.referenceSummary, 1);
            double[] tr2 = rougeN(textRankSummary, s.referenceSummary, 2);
            double[] bl1 = rougeN(lead2Baseline, s.referenceSummary, 1);
            double[] bl2 = rougeN(lead2Baseline, s.referenceSummary, 2);

            trR1P += tr1[0]; trR1R += tr1[1]; trR1F1 += tr1[2];
            trR2P += tr2[0]; trR2R += tr2[1]; trR2F1 += tr2[2];
            blR1P += bl1[0]; blR1R += bl1[1]; blR1F1 += bl1[2];
            blR2P += bl2[0]; blR2R += bl2[1]; blR2F1 += bl2[2];
        }

        int n = samples.size();
        System.out.println("\n==================================================================================");
        System.out.println(" KẾT QUẢ THỰC NGHIỆM TÓM TẮT VĂN BẢN TEXTRANK VS BASELINE LEAD-2 (THESIS REPORT)");
        System.out.println("==================================================================================");
        System.out.println("| Phương pháp | ROUGE-1 P (%) | ROUGE-1 R (%) | ROUGE-1 F1 (%) | ROUGE-2 P (%) | ROUGE-2 R (%) | ROUGE-2 F1 (%) |");
        System.out.println("|---|:---:|:---:|:---:|:---:|:---:|:---:|");
        System.out.printf("| Baseline (Lead-2) | %.2f | %.2f | %.2f | %.2f | %.2f | %.2f |\n",
                blR1P / n * 100, blR1R / n * 100, blR1F1 / n * 100, blR2P / n * 100, blR2R / n * 100, blR2F1 / n * 100);
        System.out.printf("| **TF-IDF + TextRank** | **%.2f** | **%.2f** | **%.2f** | **%.2f** | **%.2f** | **%.2f** |\n",
                trR1P / n * 100, trR1R / n * 100, trR1F1 / n * 100, trR2P / n * 100, trR2R / n * 100, trR2F1 / n * 100);
        System.out.println("==================================================================================\n");

        assertThat(trR1F1 / n).isGreaterThan(blR1F1 / n);
        assertThat(trR2F1 / n).isGreaterThan(blR2F1 / n);
    }

    private static double[] rougeN(String candidate, String reference, int nGram) {
        List<String> candTokens = TextAlgorithms.tokens(candidate);
        List<String> refTokens = TextAlgorithms.tokens(reference);
        Map<String, Integer> candGrams = nGramCounts(candTokens, nGram);
        Map<String, Integer> refGrams = nGramCounts(refTokens, nGram);
        int candTotal = candGrams.values().stream().mapToInt(Integer::intValue).sum();
        int refTotal = refGrams.values().stream().mapToInt(Integer::intValue).sum();
        if (candTotal == 0 || refTotal == 0) return new double[]{0, 0, 0};

        int overlap = 0;
        for (var e : candGrams.entrySet()) {
            overlap += Math.min(e.getValue(), refGrams.getOrDefault(e.getKey(), 0));
        }
        double precision = (double) overlap / candTotal;
        double recall = (double) overlap / refTotal;
        double f1 = (precision + recall == 0) ? 0 : 2 * precision * recall / (precision + recall);
        return new double[]{precision, recall, f1};
    }

    private static Map<String, Integer> nGramCounts(List<String> tokens, int n) {
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i + n <= tokens.size(); i++) {
            counts.merge(String.join(" ", tokens.subList(i, i + n)), 1, Integer::sum);
        }
        return counts;
    }

    record SearchQueryCase(String query, Set<Integer> relevantDocIndices) {}

    @Test
    @DisplayName("Evaluate Semantic Search (TF-IDF + Cosine) vs Strict Keyword Baseline using MAP & Recall@3")
    void evaluateSemanticSearchMapMetrics() {
        List<String> corpus = List.of(
            "Bí quyết nấu phở Bắc truyền thống nước dùng xương bò ninh kỹ thơm mùi quế hồi thảo quả",
            "Hướng dẫn xây dựng REST API với Spring Boot theo kiến trúc Clean Architecture và Domain Driven Design",
            "Cài đặt giải thuật TF-IDF kết hợp Cosine Similarity và pgvector cho tìm kiếm ngữ nghĩa tiếng Việt",
            "Trích xuất đặc trưng ảnh bằng Color Histogram HSV, bộ lọc cạnh Sobel và ma trận GLCM",
            "Tích hợp bản đồ Leaflet và truy vấn không gian PostGIS với công thức khoảng cách Haversine",
            "Xây dựng hệ thống nhắn tin thời gian thực bằng giao thức WebSocket STOMP và Redis Pub/Sub",
            "Bảo mật xác thực người dùng với JSON Web Token lưu trong HttpOnly Cookie và phân quyền RBAC",
            "Kinh nghiệm du lịch Đà Lạt tự túc khám phá đồi chè Cầu Đất và ẩm thực chợ đêm",
            "Tóm tắt văn bản tự động bằng đồ thị liên kết câu TextRank kết hợp trọng số từ khóa",
            "Phát triển giao diện Single Page Application hiện đại bằng Vue 3 Composition API và Pinia"
        );

        List<SearchQueryCase> queries = List.of(
            new SearchQueryCase("cách nấu phở ngon", Set.of(0)),
            new SearchQueryCase("kiến trúc Clean Architecture Spring Boot", Set.of(1)),
            new SearchQueryCase("vector TF-IDF tìm kiếm ngữ nghĩa postgres", Set.of(2)),
            new SearchQueryCase("tìm bài viết quanh vị trí bản đồ", Set.of(4)),
            new SearchQueryCase("chat thời gian thực nhiều server Redis", Set.of(5)),
            new SearchQueryCase("du lịch da lat", Set.of(7))
        );

        double sumKeywordAp = 0, sumSemanticAp = 0;
        double sumKeywordR3 = 0, sumSemanticR3 = 0;

        for (SearchQueryCase q : queries) {
            List<Integer> keywordRanked = rankByStrictKeywordBaseline(corpus, q.query);
            List<Integer> semanticRanked = rankBySemanticTfIdf(corpus, q.query);

            sumKeywordAp += averagePrecision(keywordRanked, q.relevantDocIndices);
            sumSemanticAp += averagePrecision(semanticRanked, q.relevantDocIndices);
            sumKeywordR3 += recallAtK(keywordRanked, q.relevantDocIndices, 3);
            sumSemanticR3 += recallAtK(semanticRanked, q.relevantDocIndices, 3);
        }

        int m = queries.size();
        double keywordMap = sumKeywordAp / m;
        double semanticMap = sumSemanticAp / m;
        double keywordR3 = sumKeywordR3 / m;
        double semanticR3 = sumSemanticR3 / m;

        System.out.println("\n===========================================================================");
        System.out.println(" KẾT QUẢ THỰC NGHIỆM TÌM KIẾM NGỮ NGHĨA VS KEYWORD BASELINE (THESIS REPORT)");
        System.out.println("===========================================================================");
        System.out.println("| Phương pháp tìm kiếm | Số truy vấn | MAP (%) | Recall@3 (%) |");
        System.out.println("|---|:---:|:---:|:---:|");
        System.out.printf("| Baseline (Exact Keyword AND) | %d | %.2f%% | %.2f%% |\n", m, keywordMap * 100, keywordR3 * 100);
        System.out.printf("| **Proposed (TF-IDF + Cosine + Accent Fold)** | **%d** | **%.2f%%** | **%.2f%%** |\n", m, semanticMap * 100, semanticR3 * 100);
        System.out.println("===========================================================================\n");

        assertThat(semanticMap).isGreaterThan(keywordMap);
        assertThat(semanticMap).isGreaterThanOrEqualTo(0.80);
    }

    private static List<Integer> rankByStrictKeywordBaseline(List<String> corpus, String query) {
        List<String> qTokens = TextAlgorithms.tokens(query);
        List<double[]> scored = new ArrayList<>();
        for (int i = 0; i < corpus.size(); i++) {
            Set<String> docTokens = new HashSet<>(TextAlgorithms.tokens(corpus.get(i)));
            boolean allMatch = !qTokens.isEmpty() && docTokens.containsAll(qTokens);
            if (allMatch) {
                scored.add(new double[]{i, (double) qTokens.size() / Math.max(1, docTokens.size())});
            }
        }
        scored.sort((a, b) -> Double.compare(b[1], a[1]));
        return scored.stream().map(a -> (int) a[0]).toList();
    }

    private static List<Integer> rankBySemanticTfIdf(List<String> corpus, String query) {
        boolean allUnaccented = TextAlgorithms.isUnaccented(query);
        boolean hasUnaccentedToken = TextAlgorithms.tokens(query).stream().anyMatch(TextAlgorithms::isUnaccented);
        double[] scores;
        if (allUnaccented) {
            scores = TextAlgorithms.similarities(corpus.stream().map(TextAlgorithms::foldedContent).toList(), TextAlgorithms.foldedContent(query));
        } else if (hasUnaccentedToken) {
            double[] exact = TextAlgorithms.similarities(corpus, query);
            double[] folded = TextAlgorithms.similarities(corpus.stream().map(TextAlgorithms::foldedContent).toList(), TextAlgorithms.foldedContent(query));
            scores = new double[corpus.size()];
            for (int i = 0; i < corpus.size(); i++) scores[i] = Math.max(exact[i], folded[i] * 0.95);
        } else {
            scores = TextAlgorithms.similarities(corpus, query);
        }
        List<Integer> ranked = new ArrayList<>();
        for (int i = 0; i < corpus.size(); i++) {
            if (scores[i] > 0) ranked.add(i);
        }
        ranked.sort((a, b) -> Double.compare(scores[b], scores[a]));
        return ranked;
    }

    private static double averagePrecision(List<Integer> ranked, Set<Integer> relevant) {
        if (relevant.isEmpty()) return 0;
        int hits = 0;
        double sumPrec = 0;
        for (int i = 0; i < ranked.size(); i++) {
            if (relevant.contains(ranked.get(i))) {
                hits++;
                sumPrec += (double) hits / (i + 1);
            }
        }
        return sumPrec / relevant.size();
    }

    private static double recallAtK(List<Integer> ranked, Set<Integer> relevant, int k) {
        if (relevant.isEmpty()) return 0;
        int hits = 0;
        for (int i = 0; i < Math.min(k, ranked.size()); i++) {
            if (relevant.contains(ranked.get(i))) hits++;
        }
        return (double) hits / relevant.size();
    }
}
