package com.vietblog.application.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.vietblog.application.service.FileStorageService;
import com.vietblog.domain.exception.BusinessRuleException;
import com.vietblog.domain.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class CloudinaryStorageServiceImpl implements FileStorageService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName = "";

    @Value("${cloudinary.api-key:}")
    private String apiKey = "";

    @Value("${cloudinary.api-secret:}")
    private String apiSecret = "";

    @Value("${app.upload-dir:uploads}")
    private String uploadDir = "uploads";

    @Override
    @SuppressWarnings("unchecked")
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.FILE_EMPTY);
        }

        if (!isCloudinaryConfigured()) {
            return storeLocally(file);
        }

        try {
            String publicId = "vietblog/" + UUID.randomUUID();

            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "auto"
                    ));

            return uploadResult.get("secure_url").toString();
        } catch (Exception e) {
            log.error("Cloudinary upload failed: {}", e.getMessage(), e);
            throw new BusinessRuleException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private boolean isCloudinaryConfigured() {
        return StringUtils.hasText(cloudName)
                && StringUtils.hasText(apiKey)
                && StringUtils.hasText(apiSecret);
    }

    private String storeLocally(MultipartFile file) {
        try {
            Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(root);

            String storedName = UUID.randomUUID() + resolveExtension(file.getOriginalFilename(), file.getContentType());
            Path target = root.resolve(storedName).normalize();
            if (!target.startsWith(root)) {
                throw new BusinessRuleException(ErrorCode.FILE_UPLOAD_FAILED);
            }

            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Cloudinary credentials not configured; stored uploaded file locally at {}", target);
            return buildPublicUrl(storedName);
        } catch (IOException ex) {
            log.error("Local file fallback upload failed: {}", ex.getMessage(), ex);
            throw new BusinessRuleException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private String buildPublicUrl(String storedName) {
        if (RequestContextHolder.getRequestAttributes() != null) {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/public/files/")
                    .path(storedName)
                    .toUriString();
        }
        return "/api/public/files/" + storedName;
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (StringUtils.hasText(originalFilename)) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0 && dot < originalFilename.length() - 1) {
                String ext = originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (ext.matches("[a-z0-9]{1,10}")) {
                    return "." + ext;
                }
            }
        }
        if (!StringUtils.hasText(contentType)) {
            return ".bin";
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        if (normalized.contains("png")) return ".png";
        if (normalized.contains("jpeg") || normalized.contains("jpg")) return ".jpg";
        if (normalized.contains("webp")) return ".webp";
        if (normalized.contains("gif")) return ".gif";
        if (normalized.contains("pdf")) return ".pdf";
        return ".bin";
    }
}
