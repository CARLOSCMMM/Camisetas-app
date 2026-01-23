package com.iesvdc.dam.acceso.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Document(collection = "reservas")
public class Reserva {

  @Id
  private String id;

  @NotNull(message = "fechaReserva es obligatoria")
  private Instant fechaReserva;

  @NotNull(message = "horario es obligatorio")
  @Valid
  private Horario horario;

  @NotBlank(message = "usuarioId es obligatorio")
  private String usuarioId;
}