package com.app.chat_service.handler;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.app.chat_service.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
	
    private ResponseEntity<ErrorResponse> buildErrorResponse(Exception ex, HttpStatus status, HttpServletRequest request) {
            
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(), 
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, status);
    }
    
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> hanleNotFoundException(NotFoundException ex, HttpServletRequest request){
    	return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }
    
    @ExceptionHandler(IlleagalArgumentsException.class)
    public ResponseEntity<ErrorResponse> handleIlleagalArgumentException(IlleagalArgumentsException ex, HttpServletRequest request){
    	return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }
    
    @ExceptionHandler(RedisOperationException.class)
    public ResponseEntity<ErrorResponse> handleRedisOperationException(RedisOperationException ex, HttpServletRequest request){
    	return buildErrorResponse(ex, HttpStatus.BAD_GATEWAY, request);
    }
    
    
    @ExceptionHandler(InvalidForwardTargetException.class)
    public ResponseEntity<ErrorResponse> handleInvalidForwardTargetException(InvalidForwardTargetException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }
}