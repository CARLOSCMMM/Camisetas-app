package com.iesvdc.dam.acceso.controller;

import com.iesvdc.dam.acceso.dto.ReservaCreateRequest;
import com.iesvdc.dam.acceso.dto.ReservaResponse;
import com.iesvdc.dam.acceso.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
public class ReservasController {

  private final ReservaService service;

  public ReservasController(ReservaService service) {
    this.service = service;
  }

  /**
   * Filtros opcionales:
   * - ?usuarioId=...
   * - ?instalacionId=...&dia=YYYY-MM-DD
   * - ?dia=YYYY-MM-DD
   */
  @GetMapping
  public List<ReservaResponse> listar(@RequestParam(required = false) String usuarioId,
                                      @RequestParam(required = false) String instalacionId,
                                      @RequestParam(required = false) LocalDate dia) {
    return service.listar(usuarioId, instalacionId, dia);
  }

  @GetMapping("/{id}")
  public ReservaResponse obtener(@PathVariable String id) {
    return service.obtener(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ReservaResponse crear(@Valid @RequestBody ReservaCreateRequest req) {
    return service.crear(req);
  }

  @PutMapping("/{id}")
  public ReservaResponse actualizar(@PathVariable String id, @Valid @RequestBody ReservaCreateRequest req) {
    return service.actualizar(id, req);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void borrar(@PathVariable String id) {
    service.borrar(id);
  }
}