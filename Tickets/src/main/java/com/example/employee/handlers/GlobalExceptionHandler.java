package com.example.employee.handlers;

import com.example.employee.dto.ErrorResponse;
import com.sun.net.httpserver.HttpsServer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            Exception ex, HttpStatus status, HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, status);
    }


    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse>handleUnauthorizedAccess(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ErrorResponse>handleTicketNodFoundAccess(Exception ex , HttpServletRequest request){
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(TicketNotCreatedException.class)
    public ResponseEntity<ErrorResponse>handleNotCreatedAccess(Exception ex , HttpServletRequest request){
        return buildErrorResponse(ex , HttpStatus.NOT_IMPLEMENTED,request);
    }
    @ExceptionHandler(NotificationNotSentException.class)
    public ResponseEntity<ErrorResponse>handleNotification(Exception exception, HttpServletRequest request){
        return buildErrorResponse(exception, HttpStatus.BAD_REQUEST, request);
    }
    @ExceptionHandler(RedisAccessException.class)
    public ResponseEntity<ErrorResponse>handleRedis(Exception exr , HttpServletRequest request){
        return buildErrorResponse(exr, HttpStatus.NOT_FOUND, request);
    }
    @ExceptionHandler(UnableToFetchTicketsException.class)
    public ResponseEntity<ErrorResponse>handleTickets(Exception e, HttpServletRequest request){
        return buildErrorResponse(e, HttpStatus.NOT_FOUND, request);
    }
    @ExceptionHandler(TicketReplyException.class)
    public ResponseEntity<ErrorResponse>handleReply(Exception e, HttpServletRequest request){
        return buildErrorResponse(e, HttpStatus.NO_CONTENT, request);
    }

    @ExceptionHandler(TicketReplySaveException.class)
    public ResponseEntity<ErrorResponse>handleReplySave(Exception e , HttpServletRequest request){
        return buildErrorResponse(e, HttpStatus.BAD_REQUEST, request);
    }
}

