package com.vietblog.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HtmlSanitizerTest {

    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    @Test
    @DisplayName("Should strip script tags and inline event handlers to prevent XSS")
    void shouldStripMaliciousScripts() {
        String input = "<p>Hello world</p><script>alert('xss')</script><img src=\"test.jpg\" onerror=\"alert('hacked')\">";
        String clean = sanitizer.sanitize(input);

        assertFalse(clean.contains("<script>"));
        assertFalse(clean.contains("alert("));
        assertFalse(clean.contains("onerror"));
        assertTrue(clean.contains("<p>Hello world</p>"));
        assertTrue(clean.contains("<img"));
    }

    @Test
    @DisplayName("Should remove javascript: pseudo-protocol from links")
    void shouldRemoveJavascriptPseudoProtocol() {
        String input = "<a href=\"javascript:alert(1)\">Click me</a>";
        String clean = sanitizer.sanitize(input);

        assertFalse(clean.contains("javascript:"));
        assertTrue(clean.contains("Click me"));
    }

    @Test
    @DisplayName("Should preserve safe rich text tags, formatting, and valid links")
    void shouldPreserveSafeRichTextTags() {
        String input = "<h1>Title</h1><p>Normal <b>bold</b> and <i>italic</i> with <a href=\"https://vietblog.com\">link</a></p>";
        String clean = sanitizer.sanitize(input);

        assertTrue(clean.contains("<h1>Title</h1>"));
        assertTrue(clean.contains("<b>bold</b>"));
        assertTrue(clean.contains("<i>italic</i>"));
        assertTrue(clean.contains("<a href=\"https://vietblog.com\">link</a>"));
    }

    @Test
    @DisplayName("Should handle null and empty input safely")
    void shouldHandleNullAndEmptyInput() {
        assertEquals("", sanitizer.sanitize(null));
        assertEquals("", sanitizer.sanitize(""));
    }
}
