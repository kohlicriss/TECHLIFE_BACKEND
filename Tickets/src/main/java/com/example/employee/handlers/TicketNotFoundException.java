package com.example.employee.handlers;

public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(String s){
        super(s);
    }

}
