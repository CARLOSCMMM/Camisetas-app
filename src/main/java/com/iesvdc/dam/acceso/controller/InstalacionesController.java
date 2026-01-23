package com.iesvdc.dam.acceso.controller;

import com.iesvdc.dam.acceso.dto.InstalacionRequest;
import com.iesvdc.dam.acceso.dto.InstalacionResponse;
import com.iesvdc.dam.acceso.service.InstalacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instalaciones")
public class InstalacionesController {

  private final InstalacionService service;

  public InstalacionesController(InstalacionService service) {
    this.service = service;
  }

  @GetMapping
  public List<InstalacionResponse> listar(@RequestParam(required = false) String ciudad,
                                          @RequestParam(required = false) String q) {
    return service.listar(ciudad, q);
  }

  @GetMapping("/{id}")
  public InstalacionResponse obtener(@PathVariable String id) {
    return service.obtener(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public InstalacionResponse crear(@Valid @RequestBody InstalacionRequest req) {
    return service.crear(req);
  }

  @PutMapping("/{id}")
  public InstalacionResponse actualizar(@PathVariable String id, @Valid @RequestBody InstalacionRequest req) {
    return service.actualizar(id, req);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void borrar(@PathVariable String id) {
    service.borrar(id);
  }
}