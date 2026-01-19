package com.iesvdc.dam.acceso.web;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
        new ApiError(Instant.now(), 404, "NOT_FOUND", ex.getMessage(), List.of())
    );
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
        new ApiError(Instant.now(), 400, "BAD_REQUEST", ex.getMessage(), List.of())
    );
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(
        new ApiError(Instant.now(), 409, "CONFLICT", ex.getMessage(), List.of())
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {

    List<String> details = ex.getBindingResult().getAllErrors().stream()
        .map(err -> {
          if (err instanceof FieldError fe) {
            return fe.getField() + ": " + fe.getDefaultMessage();
          }
          return err.getDefaultMessage();
        })
        .toList();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
        new ApiError(Instant.now(), 400, "VALIDATION", "Datos no válidos", details)
    );
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneric(Exception ex) {
    // En un entorno docente puede resultar útil devolver el mensaje.
    // En producción se recomienda no exponer detalles internos.
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        new ApiError(Instant.now(), 500, "INTERNAL_ERROR", "Error interno del servidor", List.of(ex.getMessage()))
    );
  }
}