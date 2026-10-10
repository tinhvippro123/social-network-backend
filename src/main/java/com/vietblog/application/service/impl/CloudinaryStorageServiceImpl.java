package com.vietblog.application.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.vietblog.application.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class CloudinaryStorageServiceImpl implements FileStorageService {

    private final Cloudinary cloudinary;

    @Override
    public String storeFile(MultipartFile file) {
        try {
            // Generate a unique filename without extension to use as public_id
            String originalFileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
            String publicId = "vietblog/" + UUID.randomUUID().toString();

            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "auto" // Automatically detect if it's image or video
                    ));

            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi tải file lên Cloudinary: " + e.getMessage(), e);
        }
    }
}
