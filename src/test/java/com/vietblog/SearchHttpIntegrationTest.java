package com.vietblog;

import com.vietblog.domain.entity.*;
import com.vietblog.infrastructure.repository.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class SearchHttpIntegrationTest extends VietblogBackendApplicationTests {
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired PostRepository posts;
    @Autowired CategoryRepository categories;
    MockMvc mvc;
    User owner;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = users.save(User.builder().name("Writer").email(UUID.randomUUID() + "@example.test").password("unused").build());
        Category category = categories.save(Category.builder().name("Test-" + UUID.randomUUID()).slug(UUID.randomUUID().toString()).build());
        posts.save(Post.builder().title("Unique searchable " + UUID.randomUUID()).content("Useful content about Java and Spring. ".repeat(3))
                .author(owner).category(category).tags(new ArrayList<>(List.of("java", "spring")))
                .latitude(10.8).longitude(106.7).status(Post.PostStatus.PUBLISHED).build());
        posts.flush();
    }

    @Test void malformedSearchRequestsReturnClientErrorsAndNearbyReturnsCoordinates() throws Exception {
        mvc.perform(get("/api/search")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/posts/nearby").param("lat", "no").param("lng", "106")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/posts/nearby").param("lat", "10.8").param("lng", "106.7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].latitude").value(10.8));
    }

    @Test void imageAndDocumentMultipartSearchWorkEndToEnd() throws Exception {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                image.setRGB(x, y, Color.RED.getRGB());
            }
        }
        ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
        ImageIO.write(image, "png", pngOut);
        MockMultipartFile validImage = new MockMultipartFile("image", "sample.png", "image/png", pngOut.toByteArray());
        MockMultipartFile invalidImage = new MockMultipartFile("image", "bad.png", "image/png", "not-an-image".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/search/image").file(validImage))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        mvc.perform(multipart("/api/search/image").file(invalidImage))
                .andExpect(status().isBadRequest());

        ByteArrayOutputStream docxOut = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(docxOut)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            String xml = "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                    + "<w:body><w:p><w:r><w:t>Java Spring</w:t></w:r></w:p></w:body></w:document>";
            zip.write(xml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        MockMultipartFile validDocx = new MockMultipartFile(
                "file",
                "query.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxOut.toByteArray());
        MockMultipartFile invalidDoc = new MockMultipartFile("file", "plain.txt", "text/plain", "not-a-docx".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/search/document").file(validDocx))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)));

        mvc.perform(multipart("/api/search/document").file(invalidDoc))
                .andExpect(status().isBadRequest());
    }

}
