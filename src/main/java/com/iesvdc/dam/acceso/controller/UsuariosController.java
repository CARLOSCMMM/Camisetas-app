package com.iesvdc.dam.acceso.controller;

import com.iesvdc.dam.acceso.dto.UsuarioRequest;
import com.iesvdc.dam.acceso.dto.UsuarioResponse;
import com.iesvdc.dam.acceso.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuariosController {

  private final UsuarioService service;

  public UsuariosController(UsuarioService service) {
    this.service = service;
  }

  @GetMapping
  public List<UsuarioResponse> listar() {
    return service.listar();
  }

  @GetMapping("/{id}")
  public UsuarioResponse obtener(@PathVariable String id) {
    return service.obtener(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest req) {
    return service.crear(req);
  }

  @PutMapping("/{id}")
  public UsuarioResponse actualizar(@PathVariable String id, @Valid @RequestBody UsuarioRequest req) {
    return service.actualizar(id, req);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void borrar(@PathVariable String id) {
    service.borrar(id);
  }
}