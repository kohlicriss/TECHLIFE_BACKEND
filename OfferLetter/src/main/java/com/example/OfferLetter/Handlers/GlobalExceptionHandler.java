package com.example.OfferLetter.Handlers;



import com.example.OfferLetter.Dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @ExceptionHandler(ApplicationResponse.class)
    public ResponseEntity<ErrorResponse>ApplicationResponse(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.OK, request);
    }
    @ExceptionHandler(ErrorException.class)
    public ResponseEntity<ErrorResponse>ErrorException(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }
}

