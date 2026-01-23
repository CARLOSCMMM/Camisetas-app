package com.iesvdc.dam.acceso.service;



import com.iesvdc.dam.acceso.dto.InstalacionRequest;
import com.iesvdc.dam.acceso.dto.InstalacionResponse;
import com.iesvdc.dam.acceso.model.Instalacion;
import com.iesvdc.dam.acceso.repository.InstalacionRepository;
import com.iesvdc.dam.acceso.web.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InstalacionService {

  private final InstalacionRepository repo;

  public InstalacionService(InstalacionRepository repo) {
    this.repo = repo;
  }

  public List<InstalacionResponse> listar(String ciudad, String q) {
    List<Instalacion> data;
    if (ciudad != null && !ciudad.isBlank()) data = repo.findByCiudadIgnoreCase(ciudad.trim());
    else if (q != null && !q.isBlank()) data = repo.findByNombreContainingIgnoreCase(q.trim());
    else data = repo.findAll();

    return data.stream().map(this::toResponse).toList();
  }

  public InstalacionResponse obtener(String id) {
    Instalacion i = repo.findById(id).orElseThrow(() -> new NotFoundException("Instalación no encontrada: " + id));
    return toResponse(i);
  }

  public InstalacionResponse crear(InstalacionRequest req) {
    Instalacion i = new Instalacion(null, req.nombre(), req.direccion(), req.ciudad());
    i = repo.save(i);
    return toResponse(i);
  }

  public InstalacionResponse actualizar(String id, InstalacionRequest req) {
    Instalacion i = repo.findById(id).orElseThrow(() -> new NotFoundException("Instalación no encontrada: " + id));
    i.setNombre(req.nombre());
    i.setDireccion(req.direccion());
    i.setCiudad(req.ciudad());
    i = repo.save(i);
    return toResponse(i);
  }

  public void borrar(String id) {
    if (!repo.existsById(id)) throw new NotFoundException("Instalación no encontrada: " + id);
    repo.deleteById(id);
  }

  private InstalacionResponse toResponse(Instalacion i) {
    return new InstalacionResponse(i.getId(), i.getNombre(), i.getDireccion(), i.getCiudad());
  }

}