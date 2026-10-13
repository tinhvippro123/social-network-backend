package com.vietblog.domain.search;

import org.junit.jupiter.api.Test;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class SearchAlgorithmsTest {
    @Test void tfIdfRanksRelevantContentAndRejectsUnknownVocabulary() {
        List<String> corpus = List.of("Spring Boot Java server REST API", "Vue component frontend CSS", "Java Spring deployment server");
        double[] scores = TextAlgorithms.similarities(corpus, "Spring Java REST");
        assertThat(scores[0]).isGreaterThan(scores[2]);
        assertThat(scores[2]).isGreaterThan(scores[1]);
        assertThat(TextAlgorithms.similarities(corpus, "unseenword")).containsOnly(0);
        assertThat(TextAlgorithms.similarities(List.of(), "query")).isEmpty();
    }

    @Test void normalizesVietnameseUnicodeAndIgnoresMarkup() {
        assertThat(TextAlgorithms.tokens("<p>TỐT</p><script>bad()</script>")).containsExactly("tốt");
        assertThat(TextAlgorithms.similarities(List.of("<b>lập trình</b> Java"), "lập trình")[0]).isPositive();
    }

    @Test void textRankKeepsOriginalOrderAndHandlesDisconnectedSentences() {
        String text = "Java Spring builds APIs. Java Spring serves requests. Bananas grow outdoors. Java APIs handle requests.";
        List<String> summary = TextAlgorithms.summarize(text, 2);
        assertThat(summary).hasSize(2).doesNotContain("Bananas grow outdoors.");
        assertThat(text.indexOf(summary.get(0))).isLessThan(text.indexOf(summary.get(1)));
        assertThat(TextAlgorithms.summarize("Alpha. Beta. Gamma.", 2)).hasSize(2);
        assertThat(TextAlgorithms.summarize("", 3)).isEmpty();
    }

    @Test void sentimentHandlesNegationAndNeutralText() {
        assertThat(TextAlgorithms.sentiment("Bài viết rất tốt và hay").label()).isEqualTo("positive");
        assertThat(TextAlgorithms.sentiment("không tốt").label()).isEqualTo("negative");
        assertThat(TextAlgorithms.sentiment("not bad").label()).isEqualTo("positive");
        assertThat(TextAlgorithms.sentiment("Java version 17").score()).isZero();
    }

    @Test void haversineHandlesAntimeridianAndKnownDistance() {
        assertThat(GeoAlgorithms.distanceKm(0, 0, 0, 1)).isCloseTo(111.195, within(0.01));
        assertThat(GeoAlgorithms.distanceKm(0, 179.9, 0, -179.9)).isCloseTo(22.239, within(0.01));
        assertThat(GeoAlgorithms.distanceKm(10, 106, 10, 106)).isZero();
        assertThat(GeoAlgorithms.distanceKm(0, 0, 0, 180)).isFinite();
    }

    @Test void imageSimilarityPrefersSameColorAndIsFiniteForFlatImages() {
        double[] red = ImageAlgorithms.features(solid(Color.RED));
        double[] blue = ImageAlgorithms.features(solid(Color.BLUE));
        assertThat(red).hasSize(ImageAlgorithms.DIMENSIONS);
        assertThat(ImageAlgorithms.cosine(red, red)).isCloseTo(1, within(1e-10));
        assertThat(ImageAlgorithms.cosine(red, blue)).isLessThan(0.1);
        for (double value : ImageAlgorithms.features(solid(Color.BLACK))) assertThat(value).isFinite();
    }

    private static BufferedImage solid(Color color) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) image.setRGB(x, y, color.getRGB());
        return image;
    }
}
