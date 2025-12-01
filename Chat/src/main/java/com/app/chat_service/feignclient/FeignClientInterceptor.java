package com.app.chat_service.feignclient;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Component
public class FeignClientInterceptor implements RequestInterceptor {
	
	private final CustomFeignContext customFeignContext;
	
	@Override
    public void apply(RequestTemplate template) {
        String token = customFeignContext.getToken();
        log.info("looking for Fient Client info {} {}",customFeignContext, token );
        if (token != null) {
            template.header("Authorization", token);
        } else {
            log.warn("❌ No token found to forward in Feign request");
        }
    }
    
    
}