package com.iesvdc.dam.acceso.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.iesvdc.dam.acceso.dto.PedidoCreateRequest;
import com.iesvdc.dam.acceso.model.Pedido;
import com.iesvdc.dam.acceso.service.PedidoService;
import com.iesvdc.dam.acceso.web.NotFoundException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
  
  @Autowired
  private PedidoService pedidoService;

  @GetMapping({ "", "/" })
  public List<Pedido> findAll() {
    return pedidoService.findAll();
  }

  @PostMapping({ "", "/" })
  @ResponseStatus(HttpStatus.CREATED)
  public Pedido create(@Valid @RequestBody PedidoCreateRequest request) {
    return pedidoService.create(request);
  }

  @GetMapping("/{id}")
  public Pedido findById(@PathVariable String id) {
    return pedidoService.findById(id)
        .orElseThrow(() -> new NotFoundException("Pedido no encontrado: " + id));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    pedidoService.deleteById(id);
  }
}
