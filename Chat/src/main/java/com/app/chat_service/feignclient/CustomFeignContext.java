package com.app.chat_service.feignclient;

import org.springframework.stereotype.Component;

@Component
public class CustomFeignContext {
    private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();
    private static final ThreadLocal<String> USER = new ThreadLocal<>();

    public void setToken(String token) {
        TOKEN.set(token);
    }
    
    public void setUser(String user) {
        USER.set(user);
    }
    
    public String getToken() {
        return TOKEN.get();
    }
    
    public String getUser() {
        return USER.get();
    }

    public void clear() {
        TOKEN.remove();
        USER.remove();
    }
}
