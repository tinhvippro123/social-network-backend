package com.vietblog.application.service.impl;

import com.vietblog.application.service.FileStorageService;
import com.vietblog.domain.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;

    public LocalFileStorageServiceImpl(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Không thể tạo thư mục lưu trữ file.", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        // Chuẩn hóa tên file
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "");

        try {
            // Kiểm tra tên file chứa ký tự không hợp lệ
            if (originalFileName.contains("..")) {
                throw new BusinessRuleException("Tên file không hợp lệ: " + originalFileName);
            }

            // Tạo tên file ngẫu nhiên để tránh trùng lặp
            String fileExtension = "";
            int dotIndex = originalFileName.lastIndexOf('.');
            if (dotIndex >= 0) {
                fileExtension = originalFileName.substring(dotIndex);
            }
            String newFileName = UUID.randomUUID().toString() + fileExtension;

            // Copy file vào thư mục lưu trữ
            Path targetLocation = this.fileStorageLocation.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Tạo URL truy cập
            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(newFileName)
                    .toUriString();

            return fileDownloadUri;
        } catch (IOException ex) {
            throw new RuntimeException("Không thể lưu file " + originalFileName + ". Vui lòng thử lại!", ex);
        }
    }
}
