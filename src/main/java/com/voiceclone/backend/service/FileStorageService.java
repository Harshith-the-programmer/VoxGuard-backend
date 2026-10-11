package com.voiceclone.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".wav", ".mp3", ".flac", ".ogg",
            ".oga", ".opus",
            ".m4a", ".mp4", ".aac", ".webm",
            ".aiff", ".aif", ".aifc"
    );

    private final Path uploadPath;

    public FileStorageService(
            @Value("${file.upload-dir:uploads}") String uploadDir)
            throws IOException {

        this.uploadPath = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(this.uploadPath);

        if (!Files.isDirectory(this.uploadPath)
                || !Files.isWritable(this.uploadPath)) {
            throw new IOException(
                    "Audio upload directory is not writable.");
        }
    }

    public String storeFile(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Audio file is required.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Audio file must not exceed 20MB.");
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() == null
                        ? "audio"
                        : file.getOriginalFilename()
        );

        if (originalFilename.isBlank()
                || originalFilename.contains("..")
                || originalFilename.contains("/")
                || originalFilename.contains("\\")) {
            throw new IllegalArgumentException(
                    "Invalid audio filename.");
        }

        int dotIndex = originalFilename.lastIndexOf(".");

        String extension = dotIndex >= 0
                ? originalFilename.substring(dotIndex)
                        .toLowerCase(Locale.ROOT)
                : "";

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Unsupported audio file extension.");
        }

        // Generate a unique name instead of trusting the uploaded filename.
        String storedFilename = UUID.randomUUID() + extension;

        Path targetLocation = uploadPath.resolve(storedFilename)
                .normalize();

        if (!targetLocation.startsWith(uploadPath)) {
            throw new IOException("Invalid file path.");
        }

        // Do not overwrite an existing file.
        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation);
        }

        return targetLocation.toString();
    }
}