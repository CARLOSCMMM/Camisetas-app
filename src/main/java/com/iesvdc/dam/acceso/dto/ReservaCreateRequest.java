package com.iesvdc.dam.acceso.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaCreateRequest(
    @NotBlank(message = "usuarioId es obligatorio") String usuarioId,
    @NotBlank(message = "instalacionId es obligatorio") String instalacionId,
    @NotNull(message = "dia es obligatorio") LocalDate dia,
    @NotNull(message = "horaInicio es obligatoria") LocalTime horaInicio,
    @NotNull(message = "horaFin es obligatoria") LocalTime horaFin
) {}
