package com.vietblog.infrastructure.security;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * Utility component to sanitize Rich Text HTML content before persisting to database.
 * Prevents Stored Cross-Site Scripting (XSS) by stripping dangerous tags and protocols.
 */
@Component
public class HtmlSanitizer {

    private static final Safelist RICH_TEXT_SAFELIST = Safelist.relaxed()
            .addTags("article", "section", "figure", "figcaption", "mark", "kbd", "sub", "sup")
            .addAttributes(":all", "class", "id")
            .addAttributes("img", "loading", "alt", "title", "width", "height")
            .addProtocols("img", "src", "http", "https", "data")
            .addProtocols("a", "href", "http", "https", "mailto");

    public String sanitize(String html) {
        if (html == null) {
            return "";
        }
        return Jsoup.clean(html, RICH_TEXT_SAFELIST);
    }
}
