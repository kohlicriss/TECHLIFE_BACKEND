package com.app.chat_service.controller;

import com.app.chat_service.dto.NotificationRequest;
import com.app.chat_service.feignclient.NotificationClient;
import com.app.chat_service.kakfa.KafkaMessageProcessorService;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;
import com.app.chat_service.service.AllEmployees;
import com.app.chat_service.service.TeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/chat")
@Slf4j
public class FileUploadController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private KafkaMessageProcessorService messageProcessor;

    @Autowired
    private NotificationClient notificationClient;

    @Autowired
    private TeamService teamService;

    @Autowired
    private AllEmployees allEmployees;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sender") String sender,
            @RequestParam(value = "receiver", required = false) String receiver,
            @RequestParam(value = "groupId", required = false) String groupId,
            @RequestParam(value = "type", defaultValue = "PRIVATE") String type,
            @RequestParam(value = "client_id", required = false) String clientId) {

        log.info("⬆️ File upload request received from sender: {}. File size: {}", sender, file.getSize());

        try {
            type = type.toUpperCase();
            if (!Set.of("PRIVATE", "TEAM", "DEPARTMENT").contains(type)) {
                log.error("Invalid chat type received: {}", type);
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid chat type"));
            }

            ChatMessage message = new ChatMessage();
            message.setSender(sender);
            message.setReceiver(receiver);
            message.setGroupId(groupId);
            message.setType(type);
            message.setTimestamp(LocalDateTime.now());
            message.setFileName(file.getOriginalFilename());
            message.setFileType(file.getContentType());
            message.setFileData(file.getBytes());
            message.setRead(false);
            message.setClientId(clientId);
            message.setContent(file.getOriginalFilename());
            message.setFileSize(file.getSize());

            ChatMessage savedMessage = chatMessageRepository.save(message);
            log.info("✅ File message saved to DB with ID: {}", savedMessage.getId());

            messageProcessor.processChatMessage(savedMessage);
            log.info("Sent file message to message processor. ClientID: {}, DB ID: {}", clientId, savedMessage.getId());

            
            NotificationRequest notificationRequest = new NotificationRequest();
            notificationRequest.setSender(sender);
            notificationRequest.setMessage("Sent a file: " + file.getOriginalFilename());
            notificationRequest.setSubject("New file from " + allEmployees.getEmployeeById(sender).getEmployeeName());
            notificationRequest.setType("CHAT");

            if ("PRIVATE".equalsIgnoreCase(type)) {
                notificationRequest.setReceiver(receiver);
                notificationRequest.setLink("/chat/" + receiver + "/with?id=" + sender);
                try {
                    notificationClient.send(notificationRequest);
                    log.info("📩 Private file upload notification sent to {}", receiver);
                } catch (Exception e) {
                    log.error("❌ Failed to send private file upload notification: {}", e.getMessage());
                }
            } else if ("TEAM".equalsIgnoreCase(type)) {
                try {
                    List<String> memberIds = teamService.getEmployeeIdsByTeamId(groupId);
                    for (String memberId : memberIds) {
                        if (!memberId.equals(sender)) {
                            notificationRequest.setReceiver(memberId);
                            notificationRequest.setLink("/chat/" + memberId + "/with?id=" + groupId);
                            notificationClient.send(notificationRequest);
                            log.info("📩 Group file upload notification sent to {}", memberId);
                        }
                    }
                } catch (Exception e) {
                    log.error("❌ Failed to send group file upload notification: {}", e.getMessage());
                }
            }
            

            return ResponseEntity.ok(Map.of(
                "message", "File uploaded successfully",
                "messageId", savedMessage.getId(),
                "fileName", savedMessage.getFileName(),
                "fileSize", savedMessage.getFileSize()
            ));

        } catch (Exception e) {
            log.error("File upload failed for sender: {}", sender, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload file"));
        }
    }

    @GetMapping("/file/{id}")
    public ResponseEntity<byte[]> getFile(@PathVariable Long id) {
        ChatMessage msg = chatMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found with id: " + id));

        if (msg.getFileData() == null) {
            log.error("File data is null for message id: {}", id);
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(msg.getFileType()));

        ContentDisposition disposition = (msg.getFileType() != null && (msg.getFileType().startsWith("image/") || msg.getFileType().startsWith("audio/") || msg.getFileType().startsWith("video/")))
                ? ContentDisposition.inline().filename(msg.getFileName()).build()
                : ContentDisposition.attachment().filename(msg.getFileName()).build();
        headers.setContentDisposition(disposition);

        log.info("File found:");
        return new ResponseEntity<>(msg.getFileData(), headers, HttpStatus.OK);
    }
}