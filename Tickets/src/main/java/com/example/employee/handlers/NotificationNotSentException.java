
package com.example.employee.handlers;

public class NotificationNotSentException extends RuntimeException{
    public NotificationNotSentException(String s){
        super(s);
    }
}
