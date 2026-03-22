package org.devx.automatedinvoicesystem.Service.impl;

import org.devx.automatedinvoicesystem.Service.FileStorageService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Profile("dev") // This implementation will only be active in the "dev" profile. In production, we can switch to a different implementation (e.g., S3FileStorageServiceImpl).)
public class LocalFileStorageServiceImpl implements FileStorageService {

    // Saves files to a folder named "uploads" in your project directory
    private final Path storageDirectory = Paths.get("uploads");

    public LocalFileStorageServiceImpl() {
        try {
            // Create the directory if it doesn't exist when the server starts
            Files.createDirectories(storageDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!");
        }
    }

    @Override
    public String uploadFile(MultipartFile file) {
        String extension = getFileExtension(file.getOriginalFilename());
        String uniqueFileName = UUID.randomUUID().toString() + extension;

        try {
            Path destination = storageDirectory.resolve(uniqueFileName);
            // Copy the file to the local folder
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            // Return a fake URL for local testing
            return "http://localhost:8081/local-files/" + uniqueFileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file locally.", e);
        }
    }

//    @Override
//    public String uploadFileBytes(byte[] fileBytes, String name) {
//        return "";
//    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf("."));
        }
        return "";
    }
}
