package com.example.employee.handlers;

import com.example.employee.Repository.TicketReplyRepository;

public class TicketReplyException extends RuntimeException{
        public TicketReplyException(String message, Throwable cause) {
            super(message, cause);
        }
    }


