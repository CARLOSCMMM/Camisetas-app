package com.iesvdc.dam.acceso.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.data.mongodb.core.mapping.Document;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Document(collection = "horarios")
public class Horario {

  @NotNull(message = "dia es obligatorio")
  private LocalDate dia;

  @NotNull(message = "horaInicio es obligatoria")
  private LocalTime horaInicio;

  @NotNull(message = "horaFin es obligatoria")
  private LocalTime horaFin;

  @NotNull(message = "instalacionSnapshot es obligatorio")
  @Valid
  private InstalacionSnapshot instalacionSnapshot;
}