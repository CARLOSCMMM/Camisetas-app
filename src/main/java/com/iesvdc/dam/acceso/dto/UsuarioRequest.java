package com.iesvdc.dam.acceso.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UsuarioRequest(
    @NotBlank(message = "nombre es obligatorio") String nombre,
    @NotBlank(message = "email es obligatorio") @Email(message = "email no válido") String email
) {}
