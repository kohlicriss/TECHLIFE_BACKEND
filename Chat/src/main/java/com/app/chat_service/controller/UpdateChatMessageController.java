package com.app.chat_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.app.chat_service.dto.ChatMessageRequest;
import com.app.chat_service.handler.IlleagalArgumentsException;
import com.app.chat_service.handler.NotFoundException;
import com.app.chat_service.service.UpdateChatMessageService;

@RestController
@RequestMapping("/api/chat")
public class UpdateChatMessageController {

    private final UpdateChatMessageService updateChatMessageService;

    public UpdateChatMessageController(UpdateChatMessageService updateChatMessageService) {
        this.updateChatMessageService = updateChatMessageService;
    }

    @PutMapping("/update/{messageId}")
    public ResponseEntity<String> updateMessage( @PathVariable Long messageId,@RequestBody ChatMessageRequest updatedRequest) 
    {
        
        updateChatMessageService.updateChatMessage(messageId, updatedRequest);

        return ResponseEntity.ok("Message updated successfully");
    }
}
