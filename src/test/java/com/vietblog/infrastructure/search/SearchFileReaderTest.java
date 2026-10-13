package com.vietblog.infrastructure.search;

import com.vietblog.domain.exception.BusinessRuleException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.assertj.core.api.Assertions.*;

class SearchFileReaderTest {
    private final SearchFileReader reader = new SearchFileReader();

    @Test void extractsPdfText() throws Exception {
        try (var pdf = new PDDocument(); var output = new ByteArrayOutputStream()) {
            var page = new PDPage();
            pdf.addPage(page);
            try (var stream = new PDPageContentStream(pdf, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(20, 700);
                stream.showText("Spring Boot search");
                stream.endText();
            }
            pdf.save(output);
            assertThat(reader.document(file(output.toByteArray()))).contains("Spring Boot search");
        }
    }

    @Test void extractsDocxParagraphsWithoutFollowingExternalEntities() throws Exception {
        byte[] document = docx("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body><w:p><w:r><w:t>Hello world</w:t></w:r></w:p></w:body></w:document>");
        assertThat(reader.document(file(document))).contains("Hello world");
        byte[] malicious = docx("<!DOCTYPE d [<!ENTITY x SYSTEM 'file:///does-not-exist'>]><d><t>&x;</t></d>");
        assertThatThrownBy(() -> reader.document(file(malicious))).isInstanceOf(BusinessRuleException.class);
    }

    @Test void rejectsZipInflationAndInvalidPayloads() throws Exception {
        byte[] large = docx("x".repeat(SearchFileReader.MAX_BYTES + 1));
        assertThatThrownBy(() -> reader.document(file(large))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> reader.document(file("not a document".getBytes(StandardCharsets.UTF_8)))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> reader.readBounded(file(new byte[SearchFileReader.MAX_BYTES + 1]))).isInstanceOf(BusinessRuleException.class);
    }

    @Test void acceptsPngBytesAndRejectsSvg() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB), "png", output);
        assertThat(reader.image(file(output.toByteArray())).getWidth()).isEqualTo(16);
        assertThatThrownBy(() -> reader.image(file("<svg/>".getBytes(StandardCharsets.UTF_8)))).isInstanceOf(BusinessRuleException.class);
    }

    private static MockMultipartFile file(byte[] bytes) { return new MockMultipartFile("file", "input", "application/octet-stream", bytes); }

    private static byte[] docx(String xml) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(xml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }
}
