package com.iesvdc.dam.acceso.service;


import com.iesvdc.dam.acceso.dto.UsuarioRequest;
import com.iesvdc.dam.acceso.dto.UsuarioResponse;
import com.iesvdc.dam.acceso.model.Usuario;
import com.iesvdc.dam.acceso.repository.UsuarioRepository;
import com.iesvdc.dam.acceso.web.ConflictException;
import com.iesvdc.dam.acceso.web.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

  private final UsuarioRepository repo;

  public UsuarioService(UsuarioRepository repo) {
    this.repo = repo;
  }

  public List<UsuarioResponse> listar() {
    return repo.findAll().stream().map(this::toResponse).toList();
  }

  public UsuarioResponse obtener(String id) {
    Usuario u = repo.findById(id).orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + id));
    return toResponse(u);
  }

  public UsuarioResponse crear(UsuarioRequest req) {
    if (repo.findByEmailIgnoreCase(req.email()).isPresent()) {
      throw new ConflictException("Ya existe un usuario con ese email");
    }
    Usuario u = new Usuario(null, req.nombre(), req.email());
    u = repo.save(u);
    return toResponse(u);
  }

  public UsuarioResponse actualizar(String id, UsuarioRequest req) {
    Usuario u = repo.findById(id).orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + id));

    if (!u.getEmail().equalsIgnoreCase(req.email()) && repo.findByEmailIgnoreCase(req.email()).isPresent()) {
      throw new ConflictException("Ya existe un usuario con ese email");
    }

    u.setNombre(req.nombre());
    u.setEmail(req.email());
    u = repo.save(u);
    return toResponse(u);
  }

  public void borrar(String id) {
    if (!repo.existsById(id)) throw new NotFoundException("Usuario no encontrado: " + id);
    repo.deleteById(id);
  }

  private UsuarioResponse toResponse(Usuario u) {
    return new UsuarioResponse(u.getId(), u.getNombre(), u.getEmail());
  }
}
