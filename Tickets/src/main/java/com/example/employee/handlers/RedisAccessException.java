package com.example.employee.handlers;

public class RedisAccessException extends RuntimeException{
    public RedisAccessException(String s){
        super(s);
    }
}
