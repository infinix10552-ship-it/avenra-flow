package org.devx.automatedinvoicesystem.Service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String uploadFile(MultipartFile file);

    final String UPLOAD_DIR = "local-files/";

    public default String uploadFileBytes(byte[] fileBytes, String originalFilename) {
        try {
            // 1. Generate a unique file name so bulk files don't overwrite each other
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFileName = java.util.UUID.randomUUID().toString() + fileExtension;

            // 2. Create the physical file path
            java.nio.file.Path path = java.nio.file.Paths.get(UPLOAD_DIR + uniqueFileName);

            // 3. Ensure the directory exists
            java.nio.file.Files.createDirectories(path.getParent());

            // 4. Write the raw bytes to your hard drive
            java.nio.file.Files.write(path, fileBytes);

            // 5.Return the exact URL format that Python expects!
            return "http://localhost:8081/local-files/" + uniqueFileName;

        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to store bulk file bytes", e);
        }
    }
}