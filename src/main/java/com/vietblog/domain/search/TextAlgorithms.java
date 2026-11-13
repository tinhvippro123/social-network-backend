package com.vietblog.domain.search;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/** Small, deterministic lexical algorithms; no pretrained embeddings or language model. */
public final class TextAlgorithms {
    public static final int TEXT_VECTOR_DIMENSIONS = 256;
    private static final Pattern TAGS = Pattern.compile("<[^>]*>");
    private static final Pattern WORDS = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final Set<String> POSITIVE = loadWords("/nlp/positive_words.txt", Set.of(
            "tốt", "hay", "đẹp", "thích", "yêu", "tuyệt", "vui", "đỉnh", "chuẩn", "xịn", "mê", "ngon", "giỏi", "pro",
            "chất", "siêu", "bổ", "quý", "mượt", "nhanh", "sạch", "good", "great", "excellent", "love", "helpful",
            "awesome", "best", "amazing", "wonderful", "perfect", "fantastic", "super", "nice", "clean", "fast",
            "hay không tưởng", "đỉnh không tưởng", "tốt không tưởng", "đẹp không tưởng", "ngon không tưởng"
    ));
    private static final Set<String> NEGATIVE = loadWords("/nlp/offensive_words.txt", Set.of(
            "tệ", "xấu", "ghét", "buồn", "chán", "kém", "dở", "ngu", "dốt", "đần", "ngốc", "khùng", "điên", "rác",
            "nhảm", "tồi", "xàm", "lố", "hãm", "phế", "chó", "cút", "bậy", "láo", "ác", "lừa", "xạo", "nhục", "cùi",
            "ức", "bực", "bẩn", "bad", "awful", "hate", "terrible", "useless", "poor", "trash", "stupid", "idiot",
            "dumb", "horrible", "worst", "disgusting", "annoying", "fake", "scam", "cheat", "spam"
    ));
    private static final Set<String> NEGATION = Set.of("không", "chẳng", "chưa", "hổng", "chả", "not", "never", "no");
    private static final Set<String> CONTRAST = Set.of("nhưng", "tuy", "song", "mà", "ngược");
    private static final Set<String> STOP = buildStopwords();
    private static final double[] POSITIVE_PROTOTYPE = buildLexiconPrototype(POSITIVE);
    private static final double[] NEGATIVE_PROTOTYPE = buildLexiconPrototype(NEGATIVE);

    private static Set<String> buildStopwords() {
        Set<String> raw = new HashSet<>(loadWords("/nlp/stopwords.txt", Set.of(
                "và", "là", "của", "có", "cho", "các", "một", "những", "được", "trong", "với", "để",
                "the", "a", "an", "and", "or", "is", "of", "to", "in", "for"
        )));
        raw.removeIf(w -> w.contains(" "));
        raw.removeAll(POSITIVE);
        raw.removeAll(NEGATIVE);
        raw.removeAll(NEGATION);
        raw.removeAll(Set.of(
                "ai", "ăn", "anh", "bài", "bán", "bản", "bạn", "bộ", "buổi", "cao", "cấp", "cây",
                "chơi", "chọn", "chung", "chuyển", "chuyện", "cuộc", "cuối", "cuốn", "dài", "dẫn",
                "đạt", "đặt", "đầy", "dễ", "điểm", "điều", "dữ", "đủ", "đưa", "dùng", "đúng",
                "gần", "gặp", "giảm", "giờ", "giữ", "hiểu", "hỏi", "khách", "khó", "loại", "lời",
                "lớn", "lòng", "lượng", "mang", "mạnh", "mất", "mở", "mới", "mức", "năm", "nặng",
                "nền", "ngày", "nghe", "nghĩ", "ngôi", "ngồi", "ngọn", "ngọt", "người", "nguồn",
                "nhà", "nhận", "nhìn", "nhỏ", "nhớ", "nhóm", "nói", "nơi", "nước", "phần", "phía",
                "pho", "quả", "quận", "quay", "rõ", "sáng", "số", "sớm", "sự", "tấm", "tấn",
                "tăng", "tạo", "tay", "tên", "tháng", "thanh", "thấp", "thật", "thêm", "thiếu",
                "thuần", "thường", "tìm", "tin", "tính", "toà", "trả", "tránh", "tuổi", "việc",
                "vở", "vùng", "vượt", "xa", "xem", "ý"
        ));
        return Collections.unmodifiableSet(raw);
    }

    private static Set<String> loadWords(String resourcePath, Set<String> fallback) {
        Set<String> set = new HashSet<>(fallback);
        try (var is = TextAlgorithms.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        set.add(Normalizer.normalize(line, Normalizer.Form.NFC).toLowerCase(Locale.ROOT));
                    }
                }
            }
        } catch (Exception ignored) {
            // keep fallback
        }
        return Collections.unmodifiableSet(set);
    }

    private TextAlgorithms() {}

    public static String plainText(String text) {
        if (text == null) return "";
        return TAGS.matcher(text.replaceAll("(?is)<(script|style)\\b[^>]*>.*?</\\1>", " ")).replaceAll(" ")
                .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'")
                .replace("&amp;", "&")
                .replaceAll("\\s+", " ").trim();
    }

    public static List<String> tokens(String text) {
        var matcher = WORDS.matcher(Normalizer.normalize(plainText(text), Normalizer.Form.NFC).toLowerCase(Locale.ROOT));
        List<String> words = new ArrayList<>();
        while (matcher.find()) words.add(matcher.group());
        return words;
    }

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");

    /** Removes Vietnamese diacritics (including đ/Đ) and lowercases: "Đà Lạt" -> "da lat". */
    public static String fold(String text) {
        if (text == null) return "";
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        return COMBINING_MARKS.matcher(decomposed).replaceAll("")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    /** True when the text carries no Vietnamese diacritics, i.e. the user typed an unaccented query. */
    public static boolean isUnaccented(String text) {
        String lower = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        return fold(lower).equals(lower);
    }

    /** Stop-word filtered, diacritic-folded token stream used for accent-insensitive matching. */
    public static String foldedContent(String text) {
        StringBuilder sb = new StringBuilder();
        for (String word : tokens(text)) {
            if (STOP.contains(word)) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(fold(word));
        }
        return sb.toString();
    }

    private static Map<String, Double> frequencies(String text) {
        Map<String, Double> result = new HashMap<>();
        for (String word : tokens(text)) if (!STOP.contains(word)) result.merge(word, 1.0, Double::sum);
        result.replaceAll((word, count) -> 1.0 + Math.log(count));
        return result;
    }

    /** Shared IDF fitted only on the corpus; all query/document vectors use the same vocabulary. */
    public static List<Map<String, Double>> vectors(List<String> documents, List<String> queries) {
        List<Map<String, Double>> corpus = documents.stream().map(TextAlgorithms::frequencies).toList();
        Map<String, Integer> df = new HashMap<>();
        corpus.forEach(vector -> vector.keySet().forEach(word -> df.merge(word, 1, Integer::sum)));
        List<Map<String, Double>> result = new ArrayList<>(corpus);
        queries.stream().map(TextAlgorithms::frequencies).forEach(result::add);
        for (Map<String, Double> vector : result) {
            vector.keySet().removeIf(word -> !df.containsKey(word));
            vector.replaceAll((word, tf) -> tf * (1.0 + Math.log((1.0 + documents.size()) / (1.0 + df.get(word)))));
        }
        return result;
    }

    public static double cosine(Map<String, Double> left, Map<String, Double> right) {
        double dot = 0, l2 = 0, r2 = 0;
        for (var entry : left.entrySet()) { dot += entry.getValue() * right.getOrDefault(entry.getKey(), 0.0); l2 += entry.getValue() * entry.getValue(); }
        for (double value : right.values()) r2 += value * value;
        return l2 == 0 || r2 == 0 ? 0 : Math.min(1, dot / Math.sqrt(l2 * r2));
    }

    public static double[] similarities(List<String> documents, String query) {
        var vectors = vectors(documents, List.of(query));
        var target = vectors.get(documents.size());
        return IntStream.range(0, documents.size()).mapToDouble(i -> cosine(vectors.get(i), target)).toArray();
    }

    /**
     * Projects text into a fixed-dimension L2-normalized vector via sublinear TF + signed FNV-1a
     * feature hashing over unigrams and bigrams so vectors can be stored and queried in pgvector.
     */
    public static double[] hashedTfIdfVector(String text) {
        return hashedTfIdfVector(text, TEXT_VECTOR_DIMENSIONS);
    }

    public static double[] hashedTfIdfVector(String text, int dimensions) {
        if (dimensions <= 0) throw new IllegalArgumentException("dimensions must be positive");
        double[] embedding = new double[dimensions];
        List<String> filtered = tokens(text).stream().filter(w -> !STOP.contains(w)).toList();
        if (filtered.isEmpty()) return embedding;

        Map<String, Integer> unigramCounts = new HashMap<>();
        for (String w : filtered) unigramCounts.merge(w, 1, Integer::sum);
        for (var entry : unigramCounts.entrySet()) {
            double weight = 1.0 + Math.log(entry.getValue());
            accumulateSignedHash(embedding, entry.getKey(), weight);
        }

        Map<String, Integer> bigramCounts = new HashMap<>();
        for (int i = 0; i + 1 < filtered.size(); i++) {
            bigramCounts.merge(filtered.get(i) + "_" + filtered.get(i + 1), 1, Integer::sum);
        }
        for (var entry : bigramCounts.entrySet()) {
            double weight = 0.75 * (1.0 + Math.log(entry.getValue()));
            accumulateSignedHash(embedding, entry.getKey(), weight);
        }

        double normSq = 0;
        for (double v : embedding) normSq += v * v;
        if (normSq > 0) {
            double invNorm = 1.0 / Math.sqrt(normSq);
            for (int i = 0; i < dimensions; i++) embedding[i] *= invNorm;
        }
        return embedding;
    }

    private static void accumulateSignedHash(double[] embedding, String feature, double weight) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < feature.length(); i++) {
            hash ^= feature.charAt(i);
            hash *= 0x100000001b3L;
        }
        int bucket = (int) Long.remainderUnsigned(hash & 0x7fffffffffffffffL, embedding.length);
        double sign = ((hash >>> 63) & 1L) == 0L ? 1.0 : -1.0;
        embedding[bucket] += sign * weight;
    }

    public static double cosineDense(double[] left, double[] right) {
        if (left == null || right == null || left.length == 0 || left.length != right.length) return 0;
        double dot = 0, l2 = 0, r2 = 0;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            l2 += left[i] * left[i];
            r2 += right[i] * right[i];
        }
        return l2 == 0 || r2 == 0 ? 0 : Math.max(-1.0, Math.min(1.0, dot / Math.sqrt(l2 * r2)));
    }

    /** Weighted TextRank with dangling-node redistribution; selected sentences retain source order. */
    public static List<String> summarize(String text, int count) {
        String proseSource = text == null ? "" : text.replaceAll("(?is)<(pre|h[1-6])\\b[^>]*>.*?</\\1>", " ");
        String extracted = plainText(proseSource);
        if (extracted.isEmpty()) extracted = plainText(text);
        String[] sentences = Arrays.stream(extracted.split("(?i)(?<!\\b(?:tp|ts|gs|bs|ks|cn|dr|mr|ms|st)\\.)(?<!\\b(?:ths|pgs)\\.)(?<=[.!?。！？])\\s+"))
                .map(String::trim).filter(s -> !s.isEmpty()).limit(200).toArray(String[]::new);
        int n = sentences.length;
        if (n <= count) return List.of(sentences);
        var vectors = vectors(List.of(sentences), List.of());
        double[][] edges = new double[n][n];
        double[] weights = new double[n], rank = new double[n];
        Arrays.fill(rank, 1.0 / n);
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) if (i != j) { edges[i][j] = cosine(vectors.get(i), vectors.get(j)); weights[i] += edges[i][j]; }
        for (int iteration = 0; iteration < 100; iteration++) {
            double dangling = 0;
            for (int i = 0; i < n; i++) if (weights[i] == 0) dangling += rank[i];
            double[] next = new double[n];
            Arrays.fill(next, (0.15 + 0.85 * dangling) / n);
            for (int i = 0; i < n; i++) if (weights[i] > 0) for (int j = 0; j < n; j++) next[j] += 0.85 * rank[i] * edges[i][j] / weights[i];
            double delta = 0;
            for (int i = 0; i < n; i++) delta += Math.abs(next[i] - rank[i]);
            rank = next;
            if (delta < 1e-8) break;
        }
        final double[] scores = rank;
        return IntStream.range(0, n).boxed().sorted(Comparator.<Integer>comparingDouble(i -> scores[i]).reversed().thenComparingInt(i -> i))
                .limit(count).sorted().map(i -> sentences[i]).toList();
    }

    private static double[] buildLexiconPrototype(Set<String> terms) {
        double[] vec = new double[TEXT_VECTOR_DIMENSIONS];
        for (String term : terms) {
            List<String> parts = tokens(term);
            if (parts.isEmpty()) continue;
            if (parts.size() == 1) {
                accumulateSignedHash(vec, parts.get(0), 1.0);
            } else {
                accumulateSignedHash(vec, String.join("_", parts), 1.5);
                for (String p : parts) {
                    if (!STOP.contains(p) && !NEGATION.contains(p)) {
                        accumulateSignedHash(vec, p, 0.5);
                    }
                }
            }
        }
        l2Normalize(vec);
        return vec;
    }

    private static void l2Normalize(double[] vec) {
        double normSq = 0;
        for (double v : vec) normSq += v * v;
        if (normSq > 0) {
            double invNorm = 1.0 / Math.sqrt(normSq);
            for (int i = 0; i < vec.length; i++) vec[i] *= invNorm;
        }
    }

    /** Computes an L2-normalized 256-d sentiment word/n-gram embedding with negation & contrast handling. */
    public static double[] sentimentEmbedding(String text) {
        double[] embedding = new double[TEXT_VECTOR_DIMENSIONS];
        List<String> words = tokens(text);
        if (words.isEmpty()) return embedding;

        boolean afterContrast = false;
        for (int i = 0; i < words.size(); i++) {
            String w = words.get(i);
            if (CONTRAST.contains(w)) afterContrast = true;
            if (STOP.contains(w) || NEGATION.contains(w)) continue;

            boolean doubleNegation = i >= 3 && NEGATION.contains(words.get(i - 3))
                    && ("phải".equals(words.get(i - 2)) || "hề".equals(words.get(i - 2)))
                    && NEGATION.contains(words.get(i - 1));
            int polarity = 1;
            if (!doubleNegation) {
                int negCount = 0;
                for (int j = Math.max(0, i - 2); j < i; j++) {
                    if (NEGATION.contains(words.get(j))) negCount++;
                }
                if (negCount % 2 == 1) polarity = -1;
            }
            double weight = polarity * (afterContrast ? 2.0 : 1.0);
            accumulateSignedHash(embedding, w, weight);
            if (i + 1 < words.size() && !STOP.contains(words.get(i + 1)) && !NEGATION.contains(words.get(i + 1))) {
                accumulateSignedHash(embedding, w + "_" + words.get(i + 1), 0.75 * weight);
            }
        }
        l2Normalize(embedding);
        return embedding;
    }

    public record Sentiment(String label, double score, int positiveMatches, int negativeMatches, String algorithm) {}

    public static Sentiment sentiment(String text) {
        List<String> words = tokens(text);
        int positive = 0, negative = 0;
        int posWeight = 0, negWeight = 0;
        boolean afterContrast = false;
        for (int i = 0; i < words.size(); i++) {
            if (CONTRAST.contains(words.get(i))) {
                afterContrast = true;
            }
            int sign = 0;
            int matchedLen = 1;
            for (int len = Math.min(4, words.size() - i); len >= 1; len--) {
                String phrase = String.join(" ", words.subList(i, i + len));
                if (POSITIVE.contains(phrase)) {
                    sign = 1;
                    matchedLen = len;
                    break;
                } else if (NEGATIVE.contains(phrase)) {
                    sign = -1;
                    matchedLen = len;
                    break;
                }
            }
            if (sign == 0) continue;
            boolean doubleNegation = i >= 3 && NEGATION.contains(words.get(i - 3))
                    && ("phải".equals(words.get(i - 2)) || "hề".equals(words.get(i - 2)))
                    && NEGATION.contains(words.get(i - 1));
            if (!doubleNegation) {
                int negCount = 0;
                for (int j = Math.max(0, i - 2); j < i; j++) {
                    if (NEGATION.contains(words.get(j))) negCount++;
                }
                if (negCount % 2 == 1) sign = -sign;
            }
            int weight = afterContrast ? 2 : 1;
            if (sign > 0) {
                positive++;
                posWeight += weight;
            } else {
                negative++;
                negWeight += weight;
            }
            i += matchedLen - 1;
        }
        if (posWeight + negWeight == 0) {
            return new Sentiment("neutral", 0.0, 0, 0, "lexicon+embedding-v1");
        }
        double lexiconScore = (double) (posWeight - negWeight) / (posWeight + negWeight);
        double[] emb = sentimentEmbedding(text);
        double embScore = cosineDense(emb, POSITIVE_PROTOTYPE) - cosineDense(emb, NEGATIVE_PROTOTYPE);
        double score = Math.max(-1.0, Math.min(1.0, 0.85 * lexiconScore + 0.15 * embScore));
        if (lexiconScore > 0 && score <= 0) score = Math.max(0.05, lexiconScore * 0.5);
        else if (lexiconScore < 0 && score >= 0) score = Math.min(-0.05, lexiconScore * 0.5);
        return new Sentiment(score > 0 ? "positive" : score < 0 ? "negative" : "neutral", score, positive, negative, "lexicon+embedding-v1");
    }
}
