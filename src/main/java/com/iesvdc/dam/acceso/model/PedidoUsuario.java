package com.iesvdc.dam.acceso.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class PedidoUsuario {
  private String id;
  private String nombre;
  private String email;
  private Rol rol;
}
