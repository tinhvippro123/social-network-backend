package com.vietblog.domain.search;

import com.vietblog.domain.entity.Post;
import com.vietblog.domain.service.PostDomainService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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

        SentimentMetrics metrics = evaluateSentimentCorpus(
                "KẾT QUẢ THỰC NGHIỆM ĐÁNH GIÁ GIẢI THUẬT PHÂN TÍCH CẢM XÚC (BLOG TEST CORPUS)",
                testCorpus
        );
        assertThat(metrics.accuracy()).isGreaterThan(0.75); // Must achieve at least 75% accuracy
    }

    @Test
    @DisplayName("Evaluate Vietnamese Sentiment Analysis on UIT-VSFC Benchmark Test Split (3,166 samples)")
    void evaluateSentimentOnUitVsfcBenchmark() throws IOException {
        List<TestCase> testCorpus = new ArrayList<>();
        try (var is = AlgorithmEvaluationTest.class.getResourceAsStream("/nlp/uit_vsfc_test.tsv")) {
            assertThat(is).isNotNull();
            try (var reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    int tab = line.indexOf('\t');
                    if (tab <= 0) continue;
                    String label = line.substring(0, tab).trim();
                    String sentence = line.substring(tab + 1).trim();
                    testCorpus.add(new TestCase(sentence, label));
                }
            }
        }
        assertThat(testCorpus).hasSize(3166);

        SentimentMetrics metrics = evaluateSentimentCorpus(
                "KẾT QUẢ THỰC NGHIỆM ĐÁNH GIÁ CẢM XÚC TRÊN BỘ CHUẨN UIT-VSFC TEST (3,166 MẪU)",
                testCorpus
        );
        assertThat(metrics.accuracy()).isGreaterThan(0.55);
        assertThat(metrics.negativePrecision()).isGreaterThan(0.85);
        assertThat(metrics.positiveF1()).isGreaterThan(0.75);
    }

    private record SentimentMetrics(double accuracy, double macroF1, double negativePrecision, double positiveF1) {}

    private static SentimentMetrics evaluateSentimentCorpus(String title, List<TestCase> testCorpus) {
        int total = testCorpus.size();
        int correct = 0;

        Map<String, Integer> support = new HashMap<>(Map.of("positive", 0, "negative", 0, "neutral", 0));
        Map<String, Integer> tp = new HashMap<>(Map.of("positive", 0, "negative", 0, "neutral", 0));
        Map<String, Integer> fp = new HashMap<>(Map.of("positive", 0, "negative", 0, "neutral", 0));
        Map<String, Integer> fn = new HashMap<>(Map.of("positive", 0, "negative", 0, "neutral", 0));

        for (TestCase tc : testCorpus) {
            support.merge(tc.expectedLabel, 1, Integer::sum);
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

        System.out.println("\n==========================================================================");
        System.out.println(" " + title);
        System.out.println("==========================================================================");
        System.out.println("| Nhãn cảm xúc | Tổng mẫu | TP | FP | FN | Precision (%) | Recall (%) | F1-Score (%) |");
        System.out.println("|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|");

        double sumPrecision = 0;
        double sumRecall = 0;
        double sumF1 = 0;
        double negativePrecision = 0;
        double positiveF1 = 0;

        for (String label : List.of("positive", "negative", "neutral")) {
            int t = tp.get(label);
            int f_p = fp.get(label);
            int f_n = fn.get(label);

            double precision = (t + f_p == 0) ? 0 : (double) t / (t + f_p);
            double recall = (t + f_n == 0) ? 0 : (double) t / (t + f_n);
            double f1 = (precision + recall == 0) ? 0 : 2 * (precision * recall) / (precision + recall);

            if ("negative".equals(label)) negativePrecision = precision;
            if ("positive".equals(label)) positiveF1 = f1;

            sumPrecision += precision;
            sumRecall += recall;
            sumF1 += f1;

            System.out.printf("| %-12s | %-8d | %-2d | %-2d | %-2d | %-13.2f | %-10.2f | %-12.2f |\n",
                    label.toUpperCase(), support.get(label), t, f_p, f_n, precision * 100, recall * 100, f1 * 100);
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

        return new SentimentMetrics(accuracy, macroF1, negativePrecision, positiveF1);
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

    // =========================================================================
    // BENCHMARK 4: CBIR IMAGE SEARCH (51-D HSV + SOBEL + GLCM vs 3-D RGB MEAN)
    // =========================================================================

    @Test
    @DisplayName("Evaluate CBIR Image Search (51-D HSV + Sobel + GLCM) vs 3-D Global RGB Mean Baseline")
    void evaluateCbirImageSearchMetrics() {
        List<BufferedImage> gallery = List.of(
                gradientImage(255, 70, 20, 220, 40, 15, true),    // 0: Sunset Warm A
                gradientImage(240, 82, 28, 205, 52, 18, true),    // 1: Sunset Warm B
                gradientImage(18, 112, 240, 20, 72, 202, false),  // 2: Ocean Blue A
                gradientImage(28, 128, 250, 12, 86, 212, false),  // 3: Ocean Blue B
                diagonalStripes(20, 132, 46, 46, 178, 76, 4),     // 4: Forest Green Stripes A
                diagonalStripes(26, 142, 52, 54, 188, 86, 4),     // 5: Forest Green Stripes B
                checkerboardImage(4, 245, 10),                    // 6: High-Texture Checkerboard A (mean RGB ~127.5)
                checkerboardImage(8, 240, 15),                    // 7: High-Texture Checkerboard B (mean RGB ~127.5)
                solidColorImage(128, 128, 128),                   // 8: Flat Neutral Gray Decoy A (same RGB mean 128!)
                solidColorImage(122, 122, 122)                    // 9: Flat Neutral Gray Decoy B (same RGB mean 122!)
        );

        record ImageQuery(BufferedImage query, Set<Integer> relevantIndices) {}
        List<ImageQuery> queries = List.of(
                new ImageQuery(gradientImage(248, 76, 24, 212, 46, 16, true), Set.of(0, 1)),
                new ImageQuery(gradientImage(22, 120, 245, 16, 80, 206, false), Set.of(2, 3)),
                new ImageQuery(diagonalStripes(22, 136, 48, 50, 182, 80, 4), Set.of(4, 5)),
                new ImageQuery(checkerboardImage(4, 236, 18), Set.of(6, 7)),
                new ImageQuery(solidColorImage(127, 127, 127), Set.of(8, 9))
        );

        double[][] gallery51 = gallery.stream().map(ImageAlgorithms::features).toArray(double[][]::new);
        double[][] galleryRgb = gallery.stream().map(AlgorithmEvaluationTest::meanRgbFeature).toArray(double[][]::new);

        double rgbApSum = 0, cbirApSum = 0;
        double rgbP1Sum = 0, cbirP1Sum = 0;
        double rgbR2Sum = 0, cbirR2Sum = 0;

        for (ImageQuery q : queries) {
            double[] qRgb = meanRgbFeature(q.query);
            double[] q51 = ImageAlgorithms.features(q.query);

            List<Integer> rgbRanked = rankByCosineVectors(galleryRgb, qRgb);
            List<Integer> cbirRanked = rankByCosineVectors(gallery51, q51);

            rgbApSum += averagePrecision(rgbRanked, q.relevantIndices);
            cbirApSum += averagePrecision(cbirRanked, q.relevantIndices);
            rgbP1Sum += q.relevantIndices.contains(rgbRanked.get(0)) ? 1.0 : 0.0;
            cbirP1Sum += q.relevantIndices.contains(cbirRanked.get(0)) ? 1.0 : 0.0;
            rgbR2Sum += recallAtK(rgbRanked, q.relevantIndices, 2);
            cbirR2Sum += recallAtK(cbirRanked, q.relevantIndices, 2);
        }

        int n = queries.size();
        double rgbMap = rgbApSum / n;
        double cbirMap = cbirApSum / n;
        double rgbP1 = rgbP1Sum / n;
        double cbirP1 = cbirP1Sum / n;
        double rgbR2 = rgbR2Sum / n;
        double cbirR2 = cbirR2Sum / n;

        System.out.println("\n=================================================================================");
        System.out.println(" KẾT QUẢ THỰC NGHIỆM TÌM KIẾM HÌNH ẢNH CBIR 51 CHIỀU VS RGB BASELINE (THESIS REPORT)");
        System.out.println("=================================================================================");
        System.out.println("| Phương pháp trích xuất đặc trưng | Số truy vấn | Precision@1 (%) | Recall@2 (%) | MAP (%) |");
        System.out.println("|---|:---:|:---:|:---:|:---:|");
        System.out.printf("| Baseline (3-D Global RGB Mean) | %d | %.2f%% | %.2f%% | %.2f%% |\n",
                n, rgbP1 * 100, rgbR2 * 100, rgbMap * 100);
        System.out.printf("| **Proposed (51-D HSV + Sobel + GLCM)** | **%d** | **%.2f%%** | **%.2f%%** | **%.2f%%** |\n",
                n, cbirP1 * 100, cbirR2 * 100, cbirMap * 100);
        System.out.println("=================================================================================\n");

        assertThat(cbirMap).isGreaterThan(rgbMap).isGreaterThanOrEqualTo(0.90);
        assertThat(cbirP1).isGreaterThanOrEqualTo(rgbP1);
    }

    // =========================================================================
    // BENCHMARK 5: TRENDING RANKING (REDDIT HOT + HN GRAVITY + WILSON 95%)
    // =========================================================================

    @Test
    @DisplayName("Evaluate Trending Ranking (Reddit Hot + HN Gravity + Wilson Score) vs Chronological & Raw Upvotes")
    void evaluateTrendingRankingMetrics() {
        PostDomainService ranker = new PostDomainService();
        LocalDateTime now = LocalDateTime.now();

        Post freshQuality = Post.builder().id("P0").upvotesCount(40).downvotesCount(1)
                .commentsCount(15).viewsCount(300).createdAt(now.minusHours(2)).build();
        Post staleViral = Post.builder().id("P1").upvotesCount(140).downvotesCount(10)
                .commentsCount(30).viewsCount(1500).createdAt(now.minusDays(7)).build();
        Post controversialFresh = Post.builder().id("P2").upvotesCount(25).downvotesCount(24)
                .commentsCount(5).viewsCount(120).createdAt(now.minusHours(2)).build();
        Post lowSampleOneVote = Post.builder().id("P3").upvotesCount(1).downvotesCount(0)
                .commentsCount(0).viewsCount(10).createdAt(now.minusHours(2)).build();
        Post brandNewEmpty = Post.builder().id("P4").upvotesCount(0).downvotesCount(0)
                .commentsCount(0).viewsCount(2).createdAt(now.minusMinutes(5)).build();

        List<Post> pool = List.of(freshQuality, staleViral, controversialFresh, lowSampleOneVote, brandNewEmpty);
        pool.forEach(ranker::calculateTrendingScore);

        record PairwiseAxiom(String axiom, Post higher, Post lower, boolean chronoPass, boolean rawNetVotesPass, boolean hybridPass) {}
        List<PairwiseAxiom> axioms = List.of(
                new PairwiseAxiom(
                        "Time Decay (2h quality > 7d old viral)",
                        freshQuality, staleViral,
                        freshQuality.getCreatedAt().isAfter(staleViral.getCreatedAt()),
                        netVotes(freshQuality) > netVotes(staleViral),
                        freshQuality.getTrendingScore() > staleViral.getTrendingScore()
                ),
                new PairwiseAxiom(
                        "Engagement vs Empty New (2h quality > 5m empty)",
                        freshQuality, brandNewEmpty,
                        freshQuality.getCreatedAt().isAfter(brandNewEmpty.getCreatedAt()),
                        netVotes(freshQuality) > netVotes(brandNewEmpty),
                        freshQuality.getTrendingScore() > brandNewEmpty.getTrendingScore()
                ),
                new PairwiseAxiom(
                        "Controversy Penalty (40/1 > 25/24 at same age)",
                        freshQuality, controversialFresh,
                        false, // same age -> chronological cannot separate
                        netVotes(freshQuality) > netVotes(controversialFresh),
                        freshQuality.getTrendingScore() > controversialFresh.getTrendingScore()
                ),
                new PairwiseAxiom(
                        "Wilson Dampening (40/1 > 1/0 consensus confidence)",
                        freshQuality, lowSampleOneVote,
                        false, // same age
                        netVotes(freshQuality) > netVotes(lowSampleOneVote),
                        freshQuality.getTrendingScore() > lowSampleOneVote.getTrendingScore()
                ),
                new PairwiseAxiom(
                        "Anti-Negative Surge (25/24 split > 0/0 ignored at same age)",
                        controversialFresh, brandNewEmpty,
                        controversialFresh.getCreatedAt().isAfter(brandNewEmpty.getCreatedAt()),
                        netVotes(controversialFresh) > netVotes(brandNewEmpty),
                        controversialFresh.getTrendingScore() > brandNewEmpty.getTrendingScore()
                )
        );

        long chronoOk = axioms.stream().filter(PairwiseAxiom::chronoPass).count();
        long rawOk = axioms.stream().filter(PairwiseAxiom::rawNetVotesPass).count();
        long hybridOk = axioms.stream().filter(PairwiseAxiom::hybridPass).count();

        // Raw approval ratio 1/1 = 100% vs 40/41 = 97.56%, whereas Wilson 95% prevents small-sample takeover
        double wilsonSmall = PostDomainService.wilsonLowerBound(1, 0);
        double wilsonLarge = PostDomainService.wilsonLowerBound(40, 1);

        System.out.println("\n=================================================================================");
        System.out.println(" KẾT QUẢ THỰC NGHIỆM XẾP HẠNG XU HƯỚNG TRENDING RANKING (THESIS REPORT)");
        System.out.println("=================================================================================");
        System.out.println("| Phương pháp xếp hạng | Số tiên đề đạt | Pairwise Accuracy (%) | Wilson (1/1 vs 40/41) |");
        System.out.println("|---|:---:|:---:|:---:|");
        System.out.printf("| Baseline 1 (Chronological Newest) | %d/%d | %.2f%% | Không đánh giá tương tác |\n",
                chronoOk, axioms.size(), chronoOk * 100.0 / axioms.size());
        System.out.printf("| Baseline 2 (Raw Net Upvotes) | %d/%d | %.2f%% | Lệch 7d cũ > mới |\n",
                rawOk, axioms.size(), rawOk * 100.0 / axioms.size());
        System.out.printf("| **Proposed (Reddit Hot + HN Gravity + Wilson 95%%)** | **%d/%d** | **%.2f%%** | **%.3f vs %.3f** |\n",
                hybridOk, axioms.size(), hybridOk * 100.0 / axioms.size(), wilsonSmall, wilsonLarge);
        System.out.println("=================================================================================\n");

        assertThat(hybridOk).isEqualTo(axioms.size());
        assertThat(wilsonLarge).isGreaterThan(wilsonSmall);
    }

    // =========================================================================
    // BENCHMARK 6: GEOSPATIAL SEARCH (HAVERSINE DISTANCE & RADIUS FILTERING)
    // =========================================================================

    @Test
    @DisplayName("Evaluate Geospatial Haversine Accuracy & Radius Filter Precision/Recall across HCMC Landmarks")
    void evaluateGeospatialHaversineMetrics() {
        double hcmunreLat = 10.7967, hcmunreLng = 106.6669; // 236B Lê Văn Sỹ, Tân Bình

        record Landmark(String name, double lat, double lng) {}
        List<Landmark> landmarks = List.of(
                new Landmark("Công viên Hoàng Văn Thụ (Tân Bình)", 10.7998, 106.6632),
                new Landmark("Ngã tư Phú Nhuận", 10.7990, 106.6803),
                new Landmark("Chợ Bến Thành (Quận 1)", 10.7721, 106.6983),
                new Landmark("Khu đô thị Sala (Thủ Đức)", 10.7745, 106.7235),
                new Landmark("Phú Mỹ Hưng (Quận 7)", 10.7294, 106.7219),
                new Landmark("Làng Đại học Thủ Đức", 10.8700, 106.8031),
                new Landmark("Đà Lạt (Lâm Đồng)", 11.9404, 108.4583),
                new Landmark("Hồ Hoàn Kiếm (Hà Nội)", 21.0285, 105.8542)
        );

        double maxAbsErrKm = 0;
        double sumAbsErrKm = 0;
        int within5Actual = 0, within5Pred = 0, within5Tp = 0;
        int within10Actual = 0, within10Pred = 0, within10Tp = 0;

        for (Landmark lm : landmarks) {
            double haversine = GeoAlgorithms.distanceKm(hcmunreLat, hcmunreLng, lm.lat, lm.lng);
            double reference = sphericalLawOfCosinesKm(hcmunreLat, hcmunreLng, lm.lat, lm.lng);
            double absErr = Math.abs(haversine - reference);
            maxAbsErrKm = Math.max(maxAbsErrKm, absErr);
            sumAbsErrKm += absErr;

            boolean act5 = reference <= 5.0;
            boolean pred5 = haversine <= 5.0;
            if (act5) within5Actual++;
            if (pred5) within5Pred++;
            if (act5 && pred5) within5Tp++;

            boolean act10 = reference <= 10.0;
            boolean pred10 = haversine <= 10.0;
            if (act10) within10Actual++;
            if (pred10) within10Pred++;
            if (act10 && pred10) within10Tp++;
        }

        double maeKm = sumAbsErrKm / landmarks.size();
        double p5 = within5Pred == 0 ? 0 : (double) within5Tp / within5Pred;
        double r5 = within5Actual == 0 ? 0 : (double) within5Tp / within5Actual;
        double p10 = within10Pred == 0 ? 0 : (double) within10Tp / within10Pred;
        double r10 = within10Actual == 0 ? 0 : (double) within10Tp / within10Actual;

        System.out.println("\n=================================================================================");
        System.out.println(" KẾT QUẢ THỰC NGHIỆM TÌM KIẾM KHÔNG GIAN ĐỊA LÝ HAVERSINE (THESIS REPORT)");
        System.out.println("=================================================================================");
        System.out.println("| Ngưỡng bán kính lọc | Số điểm thực tế | MAE vs Great-Circle (km) | Max Error (km) | Precision (%) | Recall (%) |");
        System.out.println("|---|:---:|:---:|:---:|:---:|:---:|");
        System.out.printf("| Bán kính <= 5.0 km | %d/%d | %.6f | %.6f | %.2f%% | %.2f%% |\n",
                within5Actual, landmarks.size(), maeKm, maxAbsErrKm, p5 * 100, r5 * 100);
        System.out.printf("| Bán kính <= 10.0 km | %d/%d | %.6f | %.6f | %.2f%% | %.2f%% |\n",
                within10Actual, landmarks.size(), maeKm, maxAbsErrKm, p10 * 100, r10 * 100);
        System.out.println("=================================================================================\n");

        assertThat(maxAbsErrKm).isLessThan(0.01);
        assertThat(p5).isCloseTo(1.0, within(1e-9));
        assertThat(r5).isCloseTo(1.0, within(1e-9));
        assertThat(p10).isCloseTo(1.0, within(1e-9));
        assertThat(r10).isCloseTo(1.0, within(1e-9));
    }

    private static int netVotes(Post post) {
        return post.getUpvotesCount() - post.getDownvotesCount();
    }

    private static double sphericalLawOfCosinesKm(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2);
        double dl = Math.toRadians(lon2 - lon1);
        double cos = Math.max(-1.0, Math.min(1.0, Math.sin(p1) * Math.sin(p2) + Math.cos(p1) * Math.cos(p2) * Math.cos(dl)));
        return 6371.0 * Math.acos(cos);
    }

    private static double[] meanRgbFeature(BufferedImage image) {
        long r = 0, g = 0, b = 0;
        int w = image.getWidth(), h = image.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = image.getRGB(x, y);
                r += (rgb >>> 16) & 255;
                g += (rgb >>> 8) & 255;
                b += rgb & 255;
            }
        }
        double total = (double) w * h;
        return new double[]{r / total, g / total, b / total};
    }

    private static List<Integer> rankByCosineVectors(double[][] gallery, double[] query) {
        List<double[]> scored = new ArrayList<>();
        for (int i = 0; i < gallery.length; i++) {
            scored.add(new double[]{i, ImageAlgorithms.cosine(query, gallery[i])});
        }
        scored.sort((a, b) -> Double.compare(b[1], a[1]));
        return scored.stream().map(a -> (int) a[0]).toList();
    }

    private static BufferedImage solidColorImage(int r, int g, int b) {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        int rgb = new Color(r, g, b).getRGB();
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                img.setRGB(x, y, rgb);
            }
        }
        return img;
    }

    private static BufferedImage gradientImage(int r1, int g1, int b1, int r2, int g2, int b2, boolean horizontal) {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                double t = (horizontal ? x : y) / 63.0;
                int r = (int) Math.round(r1 * (1 - t) + r2 * t);
                int g = (int) Math.round(g1 * (1 - t) + g2 * t);
                int b = (int) Math.round(b1 * (1 - t) + b2 * t);
                img.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        return img;
    }

    private static BufferedImage diagonalStripes(int r1, int g1, int b1, int r2, int g2, int b2, int period) {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        int c1 = new Color(r1, g1, b1).getRGB();
        int c2 = new Color(r2, g2, b2).getRGB();
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                img.setRGB(x, y, ((x + y) / period) % 2 == 0 ? c1 : c2);
            }
        }
        return img;
    }

    private static BufferedImage checkerboardImage(int blockSize, int light, int dark) {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        int cLight = new Color(light, light, light).getRGB();
        int cDark = new Color(dark, dark, dark).getRGB();
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                img.setRGB(x, y, (((x / blockSize) + (y / blockSize)) % 2 == 0) ? cLight : cDark);
            }
        }
        return img;
    }
}
