package com.aryansaxena.paymentswitch.web;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.aryansaxena.paymentswitch.iso8583.Iso8583Exception;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(Iso8583Exception.class)
  public ResponseEntity<Map<String, Object>> handleIso(Iso8583Exception ex) {
    return error(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
    return error(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
    return ResponseEntity.status(status)
        .body(
            Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message == null ? "" : message));
  }
}
