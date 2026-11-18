package com.vietblog.presentation.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves locally stored uploaded files when Cloudinary credentials are not configured (e.g. local development).
 */
@RestController
@RequestMapping("/api/public/files")
public class PublicFileController {

    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");

    @Value("${app.upload-dir:uploads}")
    private String uploadDir = "uploads";

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String fileName) {
        if (fileName == null
                || fileName.contains("..")
                || !SAFE_FILENAME_PATTERN.matcher(fileName).matches()) {
            return ResponseEntity.badRequest().build();
        }

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path target = root.resolve(fileName).normalize();
        if (!target.startsWith(root) || !Files.exists(target) || !Files.isRegularFile(target)) {
            return ResponseEntity.notFound().build();
        }

        try {
            Resource resource = new UrlResource(target.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(resolveMediaType(target, fileName))
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                    .body(resource);
        } catch (MalformedURLException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    private MediaType resolveMediaType(Path path, String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (lower.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        try {
            String probed = Files.probeContentType(path);
            if (probed != null && !probed.equalsIgnoreCase("image/svg+xml")) {
                return MediaType.parseMediaType(probed);
            }
        } catch (IOException ignored) {
            // Fall back to octet-stream when content probing fails
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
