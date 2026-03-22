package org.devx.automatedinvoicesystem.Service.impl;

import org.devx.automatedinvoicesystem.Service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@Profile("prod")
public class S3FileStorageServiceImpl implements FileStorageService {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket-name}")
    private String bucketName;

    @Value("${cloud.aws.s3.public-domain}")
    private String publicDomain;

    public S3FileStorageServiceImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    // 1. Single Upload (Converts to bytes and reuses the bulk method)
    @Override
    public String uploadFile(MultipartFile file) {
        try {
            return uploadFileBytes(file.getBytes(), file.getOriginalFilename());
        } catch (IOException e) {
            System.err.println("\n❌ FAILED TO READ SINGLE MULTIPART FILE:");
            e.printStackTrace();
            throw new RuntimeException("Failed to read file: " + e.getMessage());
        }
    }

    // 2. Bulk Upload (The actual AWS logic)
    @Override
    public String uploadFileBytes(byte[] fileBytes, String originalFileName) {
        String extension = getFileExtension(originalFileName);
        String uniqueFileName = UUID.randomUUID() + "." + extension;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(uniqueFileName)
                    .contentType("application/pdf")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));

            String cleanDomain = publicDomain.endsWith("/") ? publicDomain.substring(0, publicDomain.length() - 1) : publicDomain;
            return String.format("%s/%s", cleanDomain, uniqueFileName);

        } catch (Exception e) {
            System.err.println("\n❌ AWS SDK BULK UPLOAD CRASH:");
            e.printStackTrace();
            throw new RuntimeException("Failed to store bulk file bytes: " + e.getMessage(), e);
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        return "pdf"; // Default to pdf if no extension is found
    }
}