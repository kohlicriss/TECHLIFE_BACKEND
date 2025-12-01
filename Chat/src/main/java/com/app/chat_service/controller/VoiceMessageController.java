package com.app.chat_service.controller;

import com.app.chat_service.dto.NotificationRequest;
import com.app.chat_service.dto.VoiceMessageRequest;
import com.app.chat_service.feignclient.NotificationClient;
import com.app.chat_service.kakfa.KafkaMessageProcessorService;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;
import com.app.chat_service.service.AllEmployees;
import com.app.chat_service.service.TeamService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/chat/voice")
@Slf4j
@RequiredArgsConstructor
public class VoiceMessageController {

    private final ChatMessageRepository chatMessageRepository;
    private final KafkaMessageProcessorService messageProcessor;
    private final NotificationClient notificationClient;
    private final TeamService teamService;
    private final AllEmployees allEmployees;
    
    

    @PostMapping("/upload")
    public ResponseEntity<?> uploadVoiceMessage(@RequestBody VoiceMessageRequest voiceRequest) {
    	log.info("Voice Message request received to backend");
    	try {
            if (voiceRequest.getFileData() == null || voiceRequest.getFileData().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File data is empty."));
            }

          
            String base64Data = voiceRequest.getFileData().contains(",")
                    ? voiceRequest.getFileData().substring(voiceRequest.getFileData().indexOf(",") + 1)
                    : voiceRequest.getFileData();

            byte[] audioBytes = Base64.getDecoder().decode(base64Data);

            ChatMessage message = new ChatMessage();
            message.setSender(voiceRequest.getSender());
            message.setReceiver(voiceRequest.getReceiver());
            message.setGroupId(voiceRequest.getGroupId());
            message.setType(voiceRequest.getType().toUpperCase());
            message.setTimestamp(LocalDateTime.now());
            message.setClientId(voiceRequest.getClientId());
            message.setFileName(voiceRequest.getFileName());
            message.setFileType(voiceRequest.getFileType());
            message.setFileData(audioBytes);
            message.setFileSize((long) audioBytes.length);
            message.setContent(voiceRequest.getFileName());
            message.setRead(false);
            
            message.setDuration(voiceRequest.getDuration());

            ChatMessage savedMessage = chatMessageRepository.save(message);

            log.info("✅ Voice message saved. ID: {}, FileName: {}, Type: {}, Size: {} bytes",
                    savedMessage.getId(),
                    savedMessage.getFileName(),
                    savedMessage.getFileType(),
                    savedMessage.getFileSize()
            );

//            chatKafkaProducer.send(savedMessage);
            messageProcessor.processChatMessage(savedMessage);


            log.info("📤 Voice message sent to Message processer succesfully. ID: {}, FileName: {}, Type: {}, Size: {} bytes",
                    savedMessage.getId(),
                    savedMessage.getFileName(),
                    savedMessage.getFileType(),
                    savedMessage.getFileSize()
            );
            
            NotificationRequest notificationRequest = new NotificationRequest();
            notificationRequest.setSender(voiceRequest.getSender());
            notificationRequest.setMessage("Sent a voice message.");
            notificationRequest.setSubject("New voice message from " + allEmployees.getEmployeeById(voiceRequest.getSender()).getEmployeeName());
            notificationRequest.setType("CHAT");

            if ("PRIVATE".equalsIgnoreCase(voiceRequest.getType())) {
                notificationRequest.setReceiver(voiceRequest.getReceiver());
                notificationRequest.setLink("/chat/" + voiceRequest.getReceiver() + "/with?id=" + voiceRequest.getSender());
                try {
                    notificationClient.send(notificationRequest);
                    log.info("📩 Private voice message notification sent to {}", voiceRequest.getReceiver());
                } catch (Exception e) {
                    log.error("❌ Failed to send private voice message notification: {}", e.getMessage());
                }
                
            } else if ("TEAM".equalsIgnoreCase(voiceRequest.getType())) {
                try {
                    List<String> memberIds = teamService.getEmployeeIdsByTeamId(voiceRequest.getGroupId());
                    for (String memberId : memberIds) {
                        if (!memberId.equals(voiceRequest.getSender())) {
                            notificationRequest.setReceiver(memberId);
                            notificationRequest.setLink("/chat/" + memberId + "/with?id=" + voiceRequest.getGroupId());
                            notificationClient.send(notificationRequest);
                            log.info("📩 Group voice message notification sent to {}", memberId);
                        }
                    }
                } catch (Exception e) {
                    log.error("❌ Failed to send group voice message notification: {}", e.getMessage());
                }
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Voice message uploaded successfully",
                    "messageId", savedMessage.getId(),
                    "fileName", savedMessage.getFileName(),
                    "fileSize", savedMessage.getFileSize()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to upload voice message"));
        }
    }
}
