package com.app.chat_service.service;


import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.app.chat_service.handler.IlleagalArgumentsException;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
public class S3Service  {

    @Autowired
    private S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;


    @Autowired
    private S3Presigner s3Presigner;

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

	private String detectContentType(MultipartFile file) {
	    String filename = file.getOriginalFilename();
	    if (filename == null) return "application/octet-stream";
	    return detectContentTypeFromKey(filename);
	}
}
	




 