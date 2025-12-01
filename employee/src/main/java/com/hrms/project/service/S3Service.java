package com.hrms.project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class S3Service  {

    @Autowired
    private S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Autowired
    private S3Presigner s3Presigner;


    public String uploadFile(String employeeId, String fileType, MultipartFile file) throws IOException {
        String extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String key = employeeId + "/" + fileType + extension;
        String contentType = detectContentType(file);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .contentDisposition("inline")
                .build();

        s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        return key;
    }

    public String uploadDegreeFile(String employeeId, String folderName, String fileType, MultipartFile file) throws IOException {
        String extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String fileName = employeeId + "/" + folderName + "/" + fileType + extension;
        String contentType = detectContentType(file);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(contentType)
                .contentDisposition("inline")
                .build();

        s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        return fileName;
    }

    public String generatePresignedUrl(String key) {
        String contentType = detectContentTypeFromKey(key); // ✅ FIXED

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .responseContentDisposition("inline")
                .responseContentType(contentType) // ✅ correct MIME type
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(24))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest)
                .url()
                .toString();
    }

    public void deleteFile(String key){
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }


    public List<String> uploadMultipleFiles(String employeeId, String fileType, MultipartFile[] files) throws IOException {
        List<String> fileKeys = new ArrayList<>();
        if (files == null || files.length == 0) {
            return fileKeys;
        }

        for (MultipartFile file : files) {
            String fileName = employeeId + "/" + fileType + "/" + file.getOriginalFilename();
            String contentType = detectContentType(file);

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(contentType)
                    .contentDisposition("inline")
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            fileKeys.add(fileName);
        }
        return fileKeys;
    }

    private String detectContentType(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) return "application/octet-stream";
        return detectContentTypeFromKey(filename);
    }


    private String detectContentTypeFromKey(String key) {
        String ext = key.substring(key.lastIndexOf(".") + 1).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain";
            case "csv" -> "text/csv";
            case "html", "htm" -> "text/html";
            default -> "application/octet-stream"; // fallback
        };
    }
}
