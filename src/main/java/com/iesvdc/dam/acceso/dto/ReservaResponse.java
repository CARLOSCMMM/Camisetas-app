package com.iesvdc.dam.acceso.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaResponse(
    String id,
    Instant fechaReserva,
    String usuarioId,
    LocalDate dia,
    LocalTime horaInicio,
    LocalTime horaFin,
    InstalacionSnapshotResponse instalacion
) {}
