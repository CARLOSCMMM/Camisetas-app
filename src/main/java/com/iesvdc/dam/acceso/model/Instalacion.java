package com.iesvdc.dam.acceso.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "instalaciones")
public class Instalacion {

  @Id
  private String id;

  @NotBlank(message = "nombre es obligatorio")
  private String nombre;

  @NotBlank(message = "direccion es obligatoria")
  private String direccion;

  @NotBlank(message = "ciudad es obligatoria")
  private String ciudad;

  // Constructor necesario para maestro-detalle Horario->Instalación
  public Instalacion(String id){
    this.id=id;
    this.nombre="Instalación sin nombre";
    this.direccion="Instalación sin dirección";
    this.ciudad="Instalación sin ciudad";
  }

}

