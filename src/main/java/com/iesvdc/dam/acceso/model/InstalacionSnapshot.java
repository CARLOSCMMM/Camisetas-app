package com.iesvdc.dam.acceso.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InstalacionSnapshot {

  @NotBlank(message = "instalacionId es obligatorio")
  private String instalacionId;

  @NotBlank(message = "nombre es obligatorio")
  private String nombre;

  @NotBlank(message = "direccion es obligatoria")
  private String direccion;

  @NotBlank(message = "ciudad es obligatoria")
  private String ciudad;

}