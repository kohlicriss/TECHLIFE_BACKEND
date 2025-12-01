// package com.example.RealTime_Attendance.configuration;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.fasterxml.jackson.databind.SerializationFeature;
// import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
// import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
// import org.springframework.cache.annotation.EnableCaching;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

// import org.springframework.data.redis.connection.RedisConnectionFactory;
// import org.springframework.data.redis.connection.RedisPassword;
// import org.springframework.data.redis.connection.RedisSentinelConfiguration;
// import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
// import org.springframework.data.redis.core.RedisTemplate;
// import org.springframework.data.redis.listener.ChannelTopic;
// import org.springframework.data.redis.listener.RedisMessageListenerContainer;

// import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
// import org.springframework.data.redis.serializer.StringRedisSerializer;

// import org.springframework.data.redis.cache.RedisCacheManager;
// import org.springframework.data.redis.cache.RedisCacheConfiguration;
// import org.springframework.data.redis.serializer.RedisSerializationContext;

// import java.time.Duration;

// @Configuration
// @EnableCaching
// public class RedisConfig {

//     private final RedisProperties redisProperties;

//     public RedisConfig(RedisProperties redisProperties) {
//         this.redisProperties = redisProperties;
//     }


//     @Bean
//     public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
//         RedisTemplate<String, Object> template = new RedisTemplate<>();
//         template.setConnectionFactory(connectionFactory);

//         ObjectMapper mapper = new ObjectMapper();
//         mapper.registerModule(new JavaTimeModule());
//         mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

//         GenericJackson2JsonRedisSerializer serializer =
//                 new GenericJackson2JsonRedisSerializer(mapper);

//         template.setKeySerializer(new StringRedisSerializer());
//         template.setValueSerializer(serializer);
//         template.setHashKeySerializer(new StringRedisSerializer());
//         template.setHashValueSerializer(serializer);
//         template.afterPropertiesSet();
//         return template;
//     }
//     @Bean
//     public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
//         ObjectMapper mapper = new ObjectMapper();
//         mapper.registerModule(new JavaTimeModule());
//         mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

//         GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

//         RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
//                 .entryTtl(Duration.ofMinutes(10))
//                 .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
//                 .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

//         return RedisCacheManager.builder(connectionFactory)
//                 .cacheDefaults(config)
//                 .build();
//     }

//     // @Bean
//     // public LettuceConnectionFactory redisConnectionFactory() {
//     //     // Connect to Redis on localhost:6379 (no Sentinel)
//     //     return new LettuceConnectionFactory("localhost", 6379);
//     // }

//     @Bean
//     public LettuceConnectionFactory redisConnectionFactory() {
//         RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
//                 .master(redisProperties.getSentinel().getMaster());

//         for (String s : redisProperties.getSentinel().getNodes()) {
//             String[] parts = s.split(":");
//             if (parts.length != 2) {
//                 throw new IllegalArgumentException("Invalid sentinel node: " + s);
//             }
//             sentinelConfig.sentinel(parts[0], Integer.parseInt(parts[1]));
//         }

//         // This sets the password for Redis master
//         sentinelConfig.setPassword(RedisPassword.of(redisProperties.getPassword()));

//         return new LettuceConnectionFactory(sentinelConfig);
//     }


//     // @Bean
//     // public RedisMessageListenerContainer listenerContainer(
//     //         RedisConnectionFactory connectionFactory,
//     //         ChannelTopic topic) {

//     //     RedisMessageListenerContainer container = new RedisMessageListenerContainer();
//     //     container.setConnectionFactory(connectionFactory);
//     //     return container;
//     // }
// }
