package com.hrms.project.handlers;

import org.springframework.web.bind.annotation.ResponseStatus;


public class AadhaarNotFoundException extends RuntimeException{
    public AadhaarNotFoundException(String message) {
        super(message);
    }
}
