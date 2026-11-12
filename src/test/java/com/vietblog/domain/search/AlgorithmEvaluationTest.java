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
}
