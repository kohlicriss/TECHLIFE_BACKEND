package com.hrms.project.handlers;

public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String s) {
        super(s);
    }
}
