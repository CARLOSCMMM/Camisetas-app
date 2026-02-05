package com.iesvdc.dam.acceso.model;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Document(collection = "usuarios")
public class Usuario {

  @Id
  private String id;

  @NotBlank(message = "nombre es obligatorio")
  private String nombre;

  @NotBlank(message = "email es obligatorio")
  @Email(message = "email no válido")
  private String email;

  @NotBlank(message = "la contraseña es obligatoria")
  private String password;
}
