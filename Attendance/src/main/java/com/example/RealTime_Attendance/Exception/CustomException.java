package com.example.RealTime_Attendance.Exception;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.language.SourceLocation;

public class CustomException extends RuntimeException implements GraphQLError {

  private final HttpStatus httpStatus;
  
  public CustomException(String message, HttpStatus httpStatus) {
    super(message);
    this.httpStatus = httpStatus;
  }
  
  public HttpStatus getHttpStatus() {
    return httpStatus;
  }
  @ExceptionHandler(CustomException.class)
  public ResponseEntity<java.lang.String> handleCustomException(CustomException ex) {
    return ResponseEntity.status(ex.getHttpStatus()).body(ex.getMessage());
  }

    @Override
    public Map<String, Object> getExtensions() {
        Map<String, Object> customAttributes = new LinkedHashMap<>();

        customAttributes.put("errorCode", this.getHttpStatus());
        customAttributes.put("errorMessage", this.getMessage());

        return customAttributes;
    }

    @Override
    public List<SourceLocation> getLocations() { return null; }

    @Override
    public ErrorClassification getErrorType() { return null;
    }
}
