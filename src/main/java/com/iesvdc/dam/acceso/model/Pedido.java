package com.iesvdc.dam.acceso.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Document(collection = "pedidos")
public class Pedido {
  @Id
  private String id;

  @NotNull(message = "usuario es obligatorio")
  @Valid
  private PedidoUsuario usuario;

  @NotEmpty(message = "camisetas es obligatorio")
  @Valid
  private List<Camiseta> camisetas;

  @NotNull(message = "fechaCreacion es obligatoria")
  private LocalDateTime fechaCreacion;
}
