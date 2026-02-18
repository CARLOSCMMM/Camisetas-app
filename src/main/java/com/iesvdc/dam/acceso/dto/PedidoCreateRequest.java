package com.iesvdc.dam.acceso.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class PedidoCreateRequest {

  @NotBlank(message = "usuarioId es obligatorio")
  private String usuarioId;

  @NotEmpty(message = "camisetasIds es obligatorio")
  private List<@NotBlank(message = "camisetaId es obligatorio") String> camisetasIds;
}
