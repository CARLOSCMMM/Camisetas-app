package com.iesvdc.dam.acceso.dto;


public record InstalacionSnapshotResponse(
    String instalacionId,
    String nombre,
    String direccion,
    String ciudad
) {}