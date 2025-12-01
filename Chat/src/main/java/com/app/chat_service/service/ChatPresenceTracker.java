package com.app.chat_service.service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.app.chat_service.handler.NotFoundException;
import com.app.chat_service.handler.RedisOperationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ChatPresenceTracker {

    private static final String ACTIVE_WINDOWS_KEY_PREFIX = "active:windows:";

    private final RedisTemplate<String, String> redisTemplate;

    public ChatPresenceTracker(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String getKey(String userId) {
    	
    	if(userId==null || userId.trim().isEmpty()) {
    		throw new NotFoundException("User I cannot be null or empty");
    	}
        return ACTIVE_WINDOWS_KEY_PREFIX + userId; 
    }

    public void openChat(String userId, String target) {
    	validateInputs(userId, target);
    	try {
    		redisTemplate.opsForSet().add(getKey(userId), target);
		} catch (DataAccessException e) {
			throw new RedisOperationException("Failed to open the chat");
		}
        
    }

    public void closeChat(String userId, String target) {
    	validateInputs(userId, target);
        try {
            redisTemplate.opsForSet().remove(getKey(userId), target);
        } catch (DataAccessException ex) {
            throw new RedisOperationException("Failed to close chat in Redis for user: ");
        }
    }

    public boolean isChatWindowOpen(String userId, String target) {
        try {
            if (userId == null || userId.trim().isEmpty() || target == null || target.trim().isEmpty()) {
                return false;
            }
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(getKey(userId), target));
        } catch (Exception e) {
            log.error("Error checking chat window status for user {} and target {}", userId, target, e);
            return false;
        }
    }

    public Set<String> getOpenWindows(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        try {
            Set<String> openWindows = redisTemplate.opsForSet().members(getKey(userId));
            return openWindows != null ? openWindows : Collections.emptySet();
        } catch (DataAccessException ex) {
            throw new RedisOperationException("Failed to fetch open chat window");
        }
    }
    
    private void validateInputs(String userId, String target) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (target == null || target.trim().isEmpty()) {
            throw new IllegalArgumentException("Target cannot be null or empty");
        }
    }

}