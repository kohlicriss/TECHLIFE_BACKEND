package com.hrms.project.handlers;

public class AadhaarAlreadyAssignedException extends RuntimeException{
    public  AadhaarAlreadyAssignedException(String message) {
        super(message);
    }
}
