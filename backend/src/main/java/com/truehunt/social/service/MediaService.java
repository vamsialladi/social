package com.truehunt.social.service;

import com.truehunt.social.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class MediaService {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    public Map<String, Object> uploadFile(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty or missing");
        }

        try {
            Path targetDir = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "media";
            String ext = "";
            int dotIdx = originalName.lastIndexOf('.');
            if (dotIdx > 0) {
                ext = originalName.substring(dotIdx);
            }

            String storedFileName = UUID.randomUUID().toString() + ext;
            Path targetPath = targetDir.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/" + storedFileName;

            Map<String, Object> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("fileName", originalName);
            result.put("size", file.getSize());
            result.put("contentType", file.getContentType());
            result.put("category", category != null ? category : "general");
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store media file: " + e.getMessage(), e);
        }
    }
}

