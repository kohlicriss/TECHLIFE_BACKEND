package com.example.employee.handlers;

import org.apache.kafka.clients.admin.ClientMetricsResourceListing;

public class UnableToFetchTicketsException extends RuntimeException{
    public UnableToFetchTicketsException(String message) {
        super(message);
    }

    public UnableToFetchTicketsException(String message, Throwable cause) {
        super(message, cause);
    }
}
