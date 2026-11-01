package com.vietblog.domain.search;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/** Small, deterministic lexical algorithms; no pretrained embeddings or language model. */
public final class TextAlgorithms {
    private static final Pattern TAGS = Pattern.compile("<[^>]*>");
    private static final Pattern WORDS = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final Set<String> STOP = loadWords("/nlp/stopwords.txt", Set.of("và", "là", "của", "có", "cho", "các", "một", "những", "được", "trong", "với", "để", "the", "a", "an", "and", "or", "is", "of", "to", "in", "for"));
    private static final Set<String> POSITIVE = loadWords("/nlp/positive_words.txt", Set.of(
            "tốt", "hay", "đẹp", "thích", "yêu", "tuyệt", "vui", "đỉnh", "chuẩn", "xịn", "mê", "ngon", "giỏi", "pro",
            "chất", "siêu", "bổ", "quý", "mượt", "nhanh", "sạch", "good", "great", "excellent", "love", "helpful",
            "awesome", "best", "amazing", "wonderful", "perfect", "fantastic", "super", "nice", "clean", "fast"
    ));
    private static final Set<String> NEGATIVE = loadWords("/nlp/offensive_words.txt", Set.of(
            "tệ", "xấu", "ghét", "buồn", "chán", "kém", "dở", "ngu", "dốt", "đần", "ngốc", "khùng", "điên", "rác",
            "nhảm", "tồi", "xàm", "lố", "hãm", "phế", "chó", "cút", "bậy", "láo", "ác", "lừa", "xạo", "nhục", "cùi",
            "ức", "bực", "bẩn", "bad", "awful", "hate", "terrible", "useless", "poor", "trash", "stupid", "idiot",
            "dumb", "horrible", "worst", "disgusting", "annoying", "fake", "scam", "cheat", "spam"
    ));
    private static final Set<String> NEGATION = Set.of("không", "chẳng", "chưa", "hổng", "chả", "not", "never", "no");

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
                .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replaceAll("\\s+", " ").trim();
    }

    public static List<String> tokens(String text) {
        var matcher = WORDS.matcher(Normalizer.normalize(plainText(text), Normalizer.Form.NFC).toLowerCase(Locale.ROOT));
        List<String> words = new ArrayList<>();
        while (matcher.find()) words.add(matcher.group());
        return words;
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

    public record Sentiment(String label, double score, int positiveMatches, int negativeMatches, String algorithm) {}

    public static Sentiment sentiment(String text) {
        List<String> words = tokens(text);
        int positive = 0, negative = 0;
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            int sign = 0;
            boolean isBigram = false;
            if (i + 1 < words.size()) {
                String bigram = word + " " + words.get(i + 1);
                if (POSITIVE.contains(bigram)) {
                    sign = 1;
                    isBigram = true;
                } else if (NEGATIVE.contains(bigram)) {
                    sign = -1;
                    isBigram = true;
                }
            }
            if (sign == 0) {
                if (POSITIVE.contains(word)) sign = 1;
                else if (NEGATIVE.contains(word)) sign = -1;
            }
            if (sign == 0) continue;
            for (int j = Math.max(0, i - 2); j < i; j++) if (NEGATION.contains(words.get(j))) { sign = -sign; break; }
            if (sign > 0) positive++; else negative++;
            if (isBigram) i++;
        }
        double score = positive + negative == 0 ? 0 : (double) (positive - negative) / (positive + negative);
        return new Sentiment(score > 0 ? "positive" : score < 0 ? "negative" : "neutral", score, positive, negative, "lexicon-v1");
    }
}
