package com.app.chat_service.service;

import com.app.chat_service.handler.NotFoundException;
import com.app.chat_service.model.ClearedChat;
import com.app.chat_service.repo.ClearedChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ClearedChatService {

    private final ClearedChatRepository clearedChatRepository;
    private final AllEmployees allEmployees;
    private final TeamService teamService;

    public void clearChat(String userId, String chatId) {
    	
    	if(!allEmployees.existsById(userId)) {
    		throw new NotFoundException("User with id {} "+userId+" is not found");
    	}
    	
    	if(allEmployees.existsById(chatId) && teamService.existsByTeamId(chatId)) {
    		throw new NotFoundException("Chat with id " + chatId + " not found");
    	}
    	
        ClearedChat clearedChat = clearedChatRepository
                .findByUserIdAndChatId(userId, chatId)
                .orElse(new ClearedChat());

        clearedChat.setUserId(userId);
        clearedChat.setChatId(chatId);
        clearedChat.setClearedAt(LocalDateTime.now());

        clearedChatRepository.save(clearedChat);
    }

    public LocalDateTime getClearedAt(String userId, String chatId) {
        return clearedChatRepository
                .findByUserIdAndChatId(userId, chatId)
                .map(ClearedChat::getClearedAt)
                .orElse(LocalDateTime.of(1970, 1, 1, 0, 0)); // default = show all
    }
}
