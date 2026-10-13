package com.vietblog.infrastructure.search;

import com.vietblog.domain.exception.BusinessRuleException;
import com.vietblog.domain.exception.ErrorCode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Locale;
import java.util.zip.ZipInputStream;

@Component
public class SearchFileReader {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    public static final int MAX_TEXT = 100_000;

    public byte[] readBounded(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) throw invalid();
        try (InputStream input = file.getInputStream()) {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) throw invalid();
            return bytes;
        } catch (IOException ex) { throw invalid(); }
    }

    public BufferedImage image(MultipartFile file) {
        byte[] bytes = readBounded(file);
        // Decode only formats supported by the standard JDK, never URLs or external resources.
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalid();
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("png") && !format.equals("jpeg") && !format.equals("jpg")) throw invalid();
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > 16_000_000) throw invalid();
                var parameters = reader.getDefaultReadParam();
                int sample = Math.max(1, Math.min(width, height) / 256);
                parameters.setSourceSubsampling(sample, sample, 0, 0);
                return reader.read(0, parameters);
            } finally { reader.dispose(); }
        } catch (IOException | IllegalArgumentException ex) { throw invalid(); }
    }

    public String document(MultipartFile file) {
        byte[] bytes = readBounded(file);
        String text;
        try {
            if (bytes.length > 5 && new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) {
                try (var pdf = Loader.loadPDF(bytes)) {
                    if (pdf.isEncrypted() || !pdf.getCurrentAccessPermission().canExtractContent() || pdf.getNumberOfPages() > 100) throw invalid();
                    var output = new BoundedWriter();
                    new PDFTextStripper().writeText(pdf, output);
                    text = output.toString();
                }
            } else if (bytes.length > 4 && bytes[0] == 'P' && bytes[1] == 'K') {
                text = docx(bytes);
            } else throw invalid();
        } catch (BusinessRuleException ex) { throw ex; }
        catch (Exception ex) { throw invalid(); }
        if (text.isBlank() || text.length() > MAX_TEXT) throw invalid();
        return text;
    }

    private String docx(byte[] bytes) throws Exception {
        byte[] document = null;
        int total = 0, entries = 0;
        // Bound decompressed archive size before handing any XML to a parser.
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (++entries > 200) throw invalid();
                ByteArrayOutputStream xml = entry.getName().equals("word/document.xml") ? new ByteArrayOutputStream() : null;
                byte[] buffer = new byte[8192];
                int entryBytes = 0;
                for (int count; (count = zip.read(buffer)) != -1;) {
                    total += count;
                    entryBytes += count;
                    if (total > 20 * 1024 * 1024 || entryBytes > MAX_BYTES) throw invalid();
                    if (xml != null) xml.write(buffer, 0, count);
                }
                if (xml != null) { if (document != null) throw invalid(); document = xml.toByteArray(); }
            }
        }
        if (document == null) throw invalid();
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
        factory.setXMLResolver((publicId, systemId, base, namespace) -> { throw invalid(); });
        var xml = factory.createXMLStreamReader(new ByteArrayInputStream(document));
        var text = new StringBuilder();
        boolean insideText = false;
        try {
            while (xml.hasNext()) {
                int event = xml.next();
                if (event == XMLStreamConstants.DTD || event == XMLStreamConstants.ENTITY_REFERENCE) throw invalid();
                if (event == XMLStreamConstants.START_ELEMENT && "t".equals(xml.getLocalName())) insideText = true;
                if (event == XMLStreamConstants.CHARACTERS && insideText) text.append(xml.getText());
                if (event == XMLStreamConstants.END_ELEMENT) {
                    if ("t".equals(xml.getLocalName())) insideText = false;
                    if ("p".equals(xml.getLocalName())) text.append('\n');
                }
                if (text.length() > MAX_TEXT) throw invalid();
            }
        } finally { xml.close(); }
        return text.toString();
    }

    private static BusinessRuleException invalid() { return new BusinessRuleException(ErrorCode.INVALID_INPUT); }

    private static final class BoundedWriter extends Writer {
        private final StringBuilder text = new StringBuilder();
        @Override public void write(char[] buffer, int offset, int length) throws IOException {
            if (text.length() + length > MAX_TEXT) throw new IOException("Extracted text exceeds limit");
            text.append(buffer, offset, length);
        }
        @Override public void flush() {}
        @Override public void close() {}
        @Override public String toString() { return text.toString(); }
    }
}
