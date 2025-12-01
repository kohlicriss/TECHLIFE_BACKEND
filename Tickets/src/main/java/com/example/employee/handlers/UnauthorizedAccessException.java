package com.example.employee.handlers;

public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String s) {
        super(s);
    }
    public UnauthorizedAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}

