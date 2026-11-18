package com.vietblog;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.vietblog.domain.entity.User;
import com.vietblog.infrastructure.repository.UserRepository;
import com.vietblog.infrastructure.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:upload_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "chat.redis.enabled=false",
        "cloudinary.cloud-name=",
        "cloudinary.api-key=",
        "cloudinary.api-secret="
})
@Transactional
class FileUploadIntegrationTest {

    private static Path tempUploadDir;

    @Autowired
    WebApplicationContext context;
    @Autowired
    UserRepository users;
    @Autowired
    JwtTokenProvider jwt;

    MockMvc mvc;
    User uploader;
    String token;

    @DynamicPropertySource
    static void registerUploadDir(DynamicPropertyRegistry registry) throws Exception {
        tempUploadDir = Files.createTempDirectory("vietblog-upload-test");
        registry.add("app.upload-dir", () -> tempUploadDir.toString());
    }

    @AfterAll
    static void cleanupUploadDir() throws Exception {
        if (tempUploadDir != null && Files.exists(tempUploadDir)) {
            try (var paths = Files.walk(tempUploadDir)) {
                for (Path p : paths.sorted((a, b) -> b.compareTo(a)).toList()) {
                    Files.deleteIfExists(p);
                }
            }
        }
    }

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        uploader = users.save(User.builder()
                .name("Uploader")
                .email("uploader-" + UUID.randomUUID() + "@example.test")
                .password("unused")
                .role(User.Role.USER)
                .build());
        token = jwt.generateTokenFromUserId(uploader.getId());
    }

    @Test
    void localFallbackUploadAndPublicFetchWorkWhenCloudinaryIsUnconfigured() throws Exception {
        byte[] pngBytes = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53,
                (byte) 0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41,
                0x54, 0x78, (byte) 0x9C, 0x63, (byte) 0xF8, (byte) 0xCF, (byte) 0xC0, 0x00,
                0x00, 0x03, 0x01, 0x01, 0x00, (byte) 0xC9, (byte) 0xFE, (byte) 0x92,
                (byte) 0xEF, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E,
                0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };
        MockMultipartFile file = new MockMultipartFile("file", "screenshot.png", "image/png", pngBytes);

        MvcResult uploadResult = mvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .cookie(new Cookie("vietblog_access_token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileUrl").isNotEmpty())
                .andReturn();

        JsonNode body = new ObjectMapper().readTree(uploadResult.getResponse().getContentAsString());
        String fileUrl = body.path("data").path("fileUrl").asString();
        String publicPath = fileUrl.substring(fileUrl.indexOf("/api/public/files/"));
        assertThat(publicPath).endsWith(".png");

        // Public fetch succeeds without authentication cookie
        mvc.perform(get(publicPath))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(pngBytes));

        // Path traversal is rejected cleanly
        mvc.perform(get("/api/public/files/..secret.png"))
                .andExpect(status().isBadRequest());
    }
}
