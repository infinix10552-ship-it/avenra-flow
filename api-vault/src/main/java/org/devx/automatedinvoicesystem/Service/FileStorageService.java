package org.devx.automatedinvoicesystem.Service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    // Contract for single files
    String uploadFile(MultipartFile file);

    // Contract for bulk zip files
    String uploadFileBytes(byte[] fileBytes, String originalFilename);
}