package com.example.employee.Controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/redis")
public class RedisPublisherController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String CHANNEL = "ticket-group";

    @PostMapping("/publish")
    public String publishMessage(@RequestParam String message) {
        redisTemplate.convertAndSend(CHANNEL, message);
        return "Message published to Redis channel: " + CHANNEL;
    }
}
