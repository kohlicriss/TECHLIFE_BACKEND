package com.app.chat_service.controller;
 
import java.lang.System.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.chat_service.feignclient.CustomFeignContext;
import com.app.chat_service.handler.NotFoundException;
import com.app.chat_service.service.MessageDeleteService;

import lombok.extern.slf4j.Slf4j;
 
@RequestMapping("/api/chat") 
@RestController
@Slf4j
public class DeleteMessageController {
 
    private final MessageDeleteService messageDeleteService;
    private final CustomFeignContext customFeignContext;
 
    @Autowired
    public DeleteMessageController(MessageDeleteService messageDeleteService, CustomFeignContext customFeignContext) {
        this.messageDeleteService = messageDeleteService;
        this.customFeignContext=customFeignContext;
    }
 
    // Soft delete for current user (hide message for that user only)
    @PostMapping("delete/{messageId}/me")
    public ResponseEntity<?> deleteForMe(
            @PathVariable Long messageId,
            @RequestParam String userId) {
        try {
            messageDeleteService.deleteForMe(messageId, userId);
            log.info("Message deleted for me {}",userId);
            return ResponseEntity.ok("Message hidden for user: " + userId);
            
        } catch (NotFoundException e) {
        	log.info("Message not found with id {}",messageId);
        	throw new NotFoundException("Message not found with id:"+messageId);
        }
    }
 
    // Soft delete for everyone (only sender can do this)
    @PostMapping("delete/{messageId}/everyone")
    public ResponseEntity<?> deleteForEveryone(
            @PathVariable Long messageId,
            @RequestParam String userId,
            @RequestHeader("Authorization") String token) {
    	
        try {
        	
        	customFeignContext.setToken(token);
            messageDeleteService.deleteForEveryone(messageId, userId);
            return ResponseEntity.ok("Message deleted for everyone");
        } catch (NotFoundException e) {
        	throw new NotFoundException("Only the sender can delete this message for everyone.");
        }
    }
}