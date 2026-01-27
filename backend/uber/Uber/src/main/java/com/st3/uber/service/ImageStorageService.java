package com.st3.uber.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

import static reactor.netty.http.HttpConnectionLiveness.log;

@Service
public class ImageStorageService {

    private static final String PROFILE_DIR = "uploads/profiles";

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    public String saveProfileImage(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty");
        }

        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Unsupported image type");
        }

        try {
            Files.createDirectories(Path.of(PROFILE_DIR));

            String extension = getExtension(file.getOriginalFilename());
            String filename = "user-" + userId + "-" + UUID.randomUUID() + extension;

            Path targetPath = Path.of(PROFILE_DIR, filename);
            Files.write(targetPath, file.getBytes());

            return "/uploads/profiles/" + filename;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store image", e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    public void delete(String path) {
        try {
            if (path == null || !path.startsWith("/uploads/profiles/")) {
                return;
            }

            String filename = path.replace("/uploads/profiles/", "");
            Path filePath = Paths.get(PROFILE_DIR, filename);

            Files.deleteIfExists(filePath);

        } catch (IOException e) {
                log.info("[IMAGE CLEANUP] Deleted rejected profile image: {}", path);
        }
    }

    public void deleteProfileImageForUser(Long userId, String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return;
        }
        delete(imagePath);
    }



}
