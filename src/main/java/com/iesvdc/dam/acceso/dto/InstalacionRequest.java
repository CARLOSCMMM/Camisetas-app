package com.iesvdc.dam.acceso.dto;

import jakarta.validation.constraints.NotBlank;

public record InstalacionRequest(
    @NotBlank(message = "nombre es obligatorio") String nombre,
    @NotBlank(message = "direccion es obligatoria") String direccion,
    @NotBlank(message = "ciudad es obligatoria") String ciudad
) {}