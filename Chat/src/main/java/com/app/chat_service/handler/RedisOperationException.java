package com.app.chat_service.handler;

public class RedisOperationException extends RuntimeException{
	
	public RedisOperationException(String message) {
		super(message);
	}
}
