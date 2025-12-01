package com.app.chat_service.handler;

public class NotFoundException extends RuntimeException {
	
	public NotFoundException(String message) {
		super(message);
	}
}
