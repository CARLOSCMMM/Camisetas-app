package com.iesvdc.dam.acceso.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
        new ApiError(Instant.now(), 404, "NOT_FOUND", ex.getMessage(), List.of()));
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
        new ApiError(Instant.now(), 400, "BAD_REQUEST", ex.getMessage(), List.of()));
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(
        new ApiError(Instant.now(), 409, "CONFLICT", ex.getMessage(), List.of()));
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
        new ApiError(Instant.now(), 400, "VALIDATION", "Datos no válidos", details));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneric(Exception ex) {
    // En un entorno docente puede resultar útil devolver el mensaje.
    // En producción se recomienda no exponer detalles internos.
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        new ApiError(Instant.now(), 500, "INTERNAL_ERROR", "Error interno del servidor", List.of(ex.getMessage())));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex) {

    // Mensaje genérico para el cliente
    String msg = "JSON inválido o tipos de datos incorrectos";

    // Intentamos afinar si la causa es un formato inválido (Jackson)
    Throwable cause = ex.getCause();
    String detail = null;

    // Caso típico: InvalidFormatException (por ejemplo LocalTime = "pepe")
    if (cause instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException ife) {
      String field = ife.getPath().isEmpty() ? "?" : ife.getPath().get(0).getFieldName();
      Object value = ife.getValue();

      detail = "Campo '" + field + "' tiene un valor inválido: '" + value + "'";
      // Puedes añadir hint específico si es LocalTime
      if (ife.getTargetType() != null && ife.getTargetType().equals(java.time.LocalTime.class)) {
        detail += ". Formato esperado: HH:mm (ej. 10:00)";
      }
    } else if (cause instanceof com.fasterxml.jackson.core.JsonParseException) {
      // JSON roto (comas, llaves…)
      detail = "JSON mal formado";
    } else if (cause != null) {
      // Fallback: algo legible sin filtrar demasiado
      detail = cause.getMessage();
    }

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
        new ApiError(
            Instant.now(),
            400,
            "BAD_REQUEST",
            msg,
            detail == null ? List.of() : List.of(detail)));
  }

}