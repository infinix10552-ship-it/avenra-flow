package org.devx.automatedinvoicesystem.Service.impl;

import org.devx.automatedinvoicesystem.Service.FileStorageService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Profile("dev")
public class LocalFileStorageServiceImpl implements FileStorageService {

    private final String UPLOAD_DIR = "local-files/";

    @Override
    public String uploadFile(MultipartFile file) {
        try {
            return uploadFileBytes(file.getBytes(), file.getOriginalFilename());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read local file", e);
        }
    }

    @Override
    public String uploadFileBytes(byte[] fileBytes, String originalFilename) {
        try {
            String fileExtension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".pdf";
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            Path path = Paths.get(UPLOAD_DIR + uniqueFileName);
            Files.createDirectories(path.getParent());
            Files.write(path, fileBytes);

            return "http://localhost:8081/local-files/" + uniqueFileName;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store local bulk file bytes", e);
        }
    }
}