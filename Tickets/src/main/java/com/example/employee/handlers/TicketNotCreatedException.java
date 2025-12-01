package com.example.employee.handlers;

public class TicketNotCreatedException extends RuntimeException{
    public TicketNotCreatedException(String s){
        super((s));
    }
}
