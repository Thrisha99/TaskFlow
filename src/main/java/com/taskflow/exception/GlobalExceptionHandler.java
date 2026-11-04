package com.taskflow.exception;

import java.util.HashMap;
import java.util.Map;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.exc.InvalidFormatException;

@RestControllerAdvice
public class GlobalExceptionHandler {
private static final Logger logger =
        LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, Object> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("messages", errors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(ResponseStatusException.class)
public ResponseEntity<Map<String, Object>> handleResponseStatusException(
        ResponseStatusException ex) {

    Map<String, Object> response = new HashMap<>();

    response.put("status", ex.getStatusCode().value());
    response.put("error", "Request Failed");
    response.put("message", ex.getReason());

    return new ResponseEntity<>(response, ex.getStatusCode());
}
@ExceptionHandler(MethodArgumentTypeMismatchException.class)
public ResponseEntity<Map<String, Object>> handleTypeMismatch(
        MethodArgumentTypeMismatchException ex) {

    Map<String, Object> error = new HashMap<>();

    error.put("error", "Invalid Request Parameter");
    error.put("message", "Invalid value for parameter: " + ex.getName());
    error.put("status", HttpStatus.BAD_REQUEST.value());

    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
}
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<Map<String, Object>> handleInvalidRequest(
        HttpMessageNotReadableException ex) {

    Map<String, Object> response = new HashMap<>();

    Throwable cause = ex.getMostSpecificCause();

    String message = "Invalid request body. Check your JSON fields and values.";

    if (cause instanceof InvalidFormatException invalidFormatException) {

        Class<?> targetType = invalidFormatException.getTargetType();

        if (targetType.isEnum()) {
            String allowedValues = java.util.Arrays.stream(targetType.getEnumConstants())
                    .map(Object::toString)
                    .collect(java.util.stream.Collectors.joining(", "));

            message = "Invalid value '" + invalidFormatException.getValue()
                    + "'. Allowed values: " + allowedValues;
        }
    }

    response.put("status", HttpStatus.BAD_REQUEST.value());
    response.put("error", "Invalid Request");
    response.put("message", message);

    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
}
@ExceptionHandler(Exception.class)
public ResponseEntity<Map<String, Object>> handleUnexpectedException(
        Exception ex) {

    logger.error("Unexpected server error occurred", ex);

    Map<String, Object> response = new HashMap<>();

    response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
    response.put("error", "Internal Server Error");
    response.put("message", "Something went wrong. Please try again later.");

    return new ResponseEntity<>(
            response,
            HttpStatus.INTERNAL_SERVER_ERROR
    );
}
}