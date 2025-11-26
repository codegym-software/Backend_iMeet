package com.example.iMeetBE.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Xử lý validation errors từ @Valid annotation
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        errors.put("success", false);
        errors.put("message", "Validation failed");
        errors.put("errors", fieldErrors);
        
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Xử lý ConstraintViolationException
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(
            ConstraintViolationException ex) {
        Map<String, Object> errors = new HashMap<>();
        errors.put("success", false);
        errors.put("message", ex.getMessage());
        
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Xử lý RuntimeException (thường là business logic errors)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> errors = new HashMap<>();
        errors.put("success", false);
        errors.put("message", ex.getMessage());
        
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Xử lý các exception khác
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> errors = new HashMap<>();
        errors.put("success", false);
        errors.put("message", "An unexpected error occurred: " + ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errors);
    }
}


