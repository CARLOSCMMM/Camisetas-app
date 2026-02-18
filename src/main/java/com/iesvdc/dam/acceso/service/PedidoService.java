package com.iesvdc.dam.acceso.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.iesvdc.dam.acceso.dto.PedidoCreateRequest;
import com.iesvdc.dam.acceso.model.Camiseta;
import com.iesvdc.dam.acceso.model.Pedido;
import com.iesvdc.dam.acceso.model.PedidoUsuario;
import com.iesvdc.dam.acceso.model.Usuario;
import com.iesvdc.dam.acceso.repository.CamisetaRepository;
import com.iesvdc.dam.acceso.repository.PedidoRepository;
import com.iesvdc.dam.acceso.repository.UsuarioRepository;
import com.iesvdc.dam.acceso.web.BadRequestException;
import com.iesvdc.dam.acceso.web.NotFoundException;

@Service
public class PedidoService {

  @Autowired
  private PedidoRepository pedidoRepository;

  @Autowired
  private UsuarioRepository usuarioRepository;

  @Autowired
  private CamisetaRepository camisetaRepository;

  public List<Pedido> findAll() {
    return pedidoRepository.findAll();
  }

  public Optional<Pedido> findById(String id) {
    return pedidoRepository.findById(id);
  }

  public Pedido create(PedidoCreateRequest request) {
    if (request == null) {
      throw new BadRequestException("El pedido es obligatorio");
    }
    if (request.getUsuarioId() == null || request.getUsuarioId().isBlank()) {
      throw new BadRequestException("usuarioId es obligatorio");
    }
    if (request.getCamisetasIds() == null || request.getCamisetasIds().isEmpty()) {
      throw new BadRequestException("camisetasIds es obligatorio");
    }

    Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
        .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + request.getUsuarioId()));

    List<Camiseta> camisetas = camisetaRepository.findAllById(request.getCamisetasIds());
    Set<String> found = camisetas.stream()
        .map(Camiseta::getId)
        .collect(Collectors.toCollection(HashSet::new));
    List<String> missing = request.getCamisetasIds().stream()
        .distinct()
        .filter(id -> !found.contains(id))
        .toList();
    if (!missing.isEmpty()) {
      throw new NotFoundException("Camiseta(s) no encontrada(s): " + String.join(", ", missing));
    }

    PedidoUsuario usuarioPedido = new PedidoUsuario(
        usuario.getId(),
        usuario.getNombre(),
        usuario.getEmail(),
        usuario.getRol());

    Map<String, Camiseta> camisetasById = camisetas.stream()
        .collect(Collectors.toMap(Camiseta::getId, camiseta -> camiseta));
    List<Camiseta> camisetasPedido = request.getCamisetasIds().stream()
        .map(camisetasById::get)
        .toList();

    Pedido pedido = new Pedido();
    pedido.setUsuario(usuarioPedido);
    pedido.setCamisetas(camisetasPedido);
    pedido.setFechaCreacion(LocalDateTime.now());

    return pedidoRepository.save(pedido);
  }

  public void deleteById(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException("El ID del pedido a eliminar es obligatorio");
    }
    if (pedidoRepository.existsById(id)) {
      pedidoRepository.deleteById(id);
    } else {
      throw new NotFoundException("Pedido no encontrado: " + id);
    }
  }
}
