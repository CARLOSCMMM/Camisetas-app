package com.iesvdc.dam.acceso.model;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;



@NoArgsConstructor
@AllArgsConstructor
@Data
@Document(collection = "camisetas")
public class Camiseta {
  @Id
  private String id;

  @NotBlank(message = "nombre es obligatorio")
  private String nombre;

  @NotBlank(message = "La Talla es obligatoria")
  @Pattern(regexp = "^(S|M|L|XL)$", message = "La talla debe ser S, M, L o XL")
  private String talla;

  @NotBlank(message = "El color es obligatorio")
  private String color;
  
  @NotNull(message = "El precio es obligatorio")
  @Positive(message = "El precio debe ser mayor que 0")
  private double precio;
  
  @NotNull(message = "El stock es obligatorio")
  @Min(value = 0, message = "El stock no puede ser negativo")
  private int stock;
}
