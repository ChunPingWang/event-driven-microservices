package com.example.orderservice.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 錯誤響應 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    @JsonProperty("error")
    private String error;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("status")
    private int status;
    
    @JsonProperty("path")
    private String path;
    
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    @JsonProperty("validationErrors")
    private List<ValidationError> validationErrors;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        
        @JsonProperty("field")
        private String field;
        
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("rejectedValue")
        private Object rejectedValue;
    }
    
    public static ErrorResponse of(String error, String message, int status, String path) {
        return ErrorResponse.builder()
            .error(error)
            .message(message)
            .status(status)
            .path(path)
            .timestamp(LocalDateTime.now())
            .build();
    }
}