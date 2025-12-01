package com.app.chat_service.handler;

public class InvalidForwardTargetException extends RuntimeException {
    public InvalidForwardTargetException(String message) {
        super(message);
    }
}