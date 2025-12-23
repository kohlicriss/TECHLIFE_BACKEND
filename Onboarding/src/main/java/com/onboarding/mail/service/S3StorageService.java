package com.onboarding.mail.service;

import com.onboarding.mail.exceptionHandler.FileUploadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.Objects;
@Service
@Slf4j
public class S3StorageService {

    @Value("${cloud.aws.s3.bucket-name}")
    private String bucketName;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 200 * 1024 * 1024; // 200MB


    public S3StorageService(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    public String uploadFile(MultipartFile file, Long candidateId, String docType) {

        if (file == null || file.isEmpty()) {
            log.warn("Skipping {} upload (file missing)", docType);
            return null;
        }

        long allowedSize =
                "introVideo".equals(docType) ? MAX_VIDEO_SIZE : MAX_FILE_SIZE;

        if (file.getSize() > allowedSize) {
            throw new IllegalArgumentException(
                    docType + " exceeds allowed size limit"
            );
        }


        try {
            String originalName = Objects.requireNonNull(file.getOriginalFilename())
                    .replaceAll("\\s+", "_")
                    .replaceAll("[^a-zA-Z0-9._-]", "");

            String s3Key = "candidates/" + candidateId + "/" + docType + "_" + originalName;

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .build();

            try (InputStream inputStream = file.getInputStream()) {
                s3Client.putObject(
                        request,
                        RequestBody.fromInputStream(inputStream, file.getSize())
                );
            }

            log.info("Uploaded {} → {}", docType, s3Key);
            return s3Key;

        } catch (Exception e) {
            log.error("S3 upload failed for {}", docType, e);
            throw new FileUploadException("File upload failed for " + docType, e);
        }
    }

    public String generatePresignedUrl(String key) {

        if (key == null || key.isBlank()) {
            return null;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .responseContentDisposition("inline")
                .responseContentType(detectContentTypeFromKey(key))
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(24))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest)
                .url()
                .toString();
    }

    private String detectContentTypeFromKey(String key) {

        if (!key.contains(".")) {
            return "application/octet-stream";
        }

        String ext = key.substring(key.lastIndexOf('.') + 1).toLowerCase();

        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain";
            case "csv" -> "text/csv";
            case "html", "htm" -> "text/html";
            default -> "application/octet-stream";
        };
    }

}
