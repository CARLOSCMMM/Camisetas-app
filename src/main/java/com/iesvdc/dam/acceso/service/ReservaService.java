package com.iesvdc.dam.acceso.service;

import com.iesvdc.dam.acceso.dto.*;
import com.iesvdc.dam.acceso.model.*;
import com.iesvdc.dam.acceso.repository.*;
import com.iesvdc.dam.acceso.web.BadRequestException;
import com.iesvdc.dam.acceso.web.ConflictException;
import com.iesvdc.dam.acceso.web.NotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class ReservaService {

  private final ReservaRepository reservaRepo;
  private final UsuarioRepository usuarioRepo;
  private final InstalacionRepository instalacionRepo;

  public ReservaService(ReservaRepository reservaRepo, UsuarioRepository usuarioRepo, InstalacionRepository instalacionRepo) {
    this.reservaRepo = reservaRepo;
    this.usuarioRepo = usuarioRepo;
    this.instalacionRepo = instalacionRepo;
  }

  public List<ReservaResponse> listar(String usuarioId, String instalacionId, LocalDate dia) {
    List<Reserva> data;

    if (usuarioId != null && !usuarioId.isBlank() && dia != null) {
      data = reservaRepo.findByHorario_InstalacionSnapshot_InstalacionIdAndHorario_Dia(usuarioId, dia);
    } else if (usuarioId != null && !usuarioId.isBlank()) {
      data = reservaRepo.findByUsuarioId(usuarioId);
    } else if (instalacionId != null && !instalacionId.isBlank() && dia != null) {
      data = reservaRepo.findByHorario_InstalacionSnapshot_InstalacionIdAndHorario_Dia(instalacionId, dia);
    } else if (dia != null) {
      data = reservaRepo.findByHorario_Dia(dia);
    } else {
      data = reservaRepo.findAll();
    }

    return data.stream().map(this::toResponse).toList();
  }

  public ReservaResponse obtener(String id) {
    Reserva r = reservaRepo.findById(id).orElseThrow(() -> new NotFoundException("Reserva no encontrada: " + id));
    return toResponse(r);
  }

  public ReservaResponse crear(ReservaCreateRequest req) {
    validarRango(req.horaInicio(), req.horaFin());

    // referencias deben existir
    if (!usuarioRepo.existsById(req.usuarioId())) {
      throw new NotFoundException("Usuario no encontrado: " + req.usuarioId());
    }

    Instalacion inst = instalacionRepo.findById(req.instalacionId())
        .orElseThrow(() -> new NotFoundException("Instalación no encontrada: " + req.instalacionId()));

    // evitar solape
    comprobarSolape(inst.getId(), req.dia(), req.horaInicio(), req.horaFin(), null);

    // snapshot embebido
    InstalacionSnapshot snap = new InstalacionSnapshot(inst.getId(), inst.getNombre(), inst.getDireccion(), inst.getCiudad());
    Horario horario = new Horario(req.dia(), req.horaInicio(), req.horaFin(), snap);

    Reserva r = new Reserva(null, Instant.now(), horario, req.usuarioId());
    r = reservaRepo.save(r);
    return toResponse(r);
  }

  public ReservaResponse actualizar(String id, ReservaCreateRequest req) {
    validarRango(req.horaInicio(), req.horaFin());

    Reserva r = reservaRepo.findById(id).orElseThrow(() -> new NotFoundException("Reserva no encontrada: " + id));

    if (!usuarioRepo.existsById(req.usuarioId())) {
      throw new NotFoundException("Usuario no encontrado: " + req.usuarioId());
    }

    Instalacion inst = instalacionRepo.findById(req.instalacionId())
        .orElseThrow(() -> new NotFoundException("Instalación no encontrada: " + req.instalacionId()));

    comprobarSolape(inst.getId(), req.dia(), req.horaInicio(), req.horaFin(), r.getId());

    InstalacionSnapshot snap = new InstalacionSnapshot(inst.getId(), inst.getNombre(), inst.getDireccion(), inst.getCiudad());
    r.setHorario(new Horario(req.dia(), req.horaInicio(), req.horaFin(), snap));
    r.setUsuarioId(req.usuarioId());
    r = reservaRepo.save(r);
    return toResponse(r);
  }

  public void borrar(String id) {
    if (!reservaRepo.existsById(id)) throw new NotFoundException("Reserva no encontrada: " + id);
    reservaRepo.deleteById(id);
  }

  private void validarRango(LocalTime ini, LocalTime fin) {
    if (ini == null || fin == null || !ini.isBefore(fin)) {
      throw new BadRequestException("El rango horario no es válido (horaInicio debe ser anterior a horaFin)");
    }
  }

  private void comprobarSolape(String instalacionId, LocalDate dia, LocalTime ini, LocalTime fin, String excluirReservaId) {
    List<Reserva> delDia = reservaRepo.findByHorario_InstalacionSnapshot_InstalacionIdAndHorario_Dia(instalacionId, dia);

    boolean solapa = delDia.stream()
        .filter(r -> excluirReservaId == null || !r.getId().equals(excluirReservaId))
        .anyMatch(r -> r.getHorario().getHoraInicio().isBefore(fin) && ini.isBefore(r.getHorario().getHoraFin()));

    if (solapa) throw new ConflictException("Existe una reserva que solapa en esa instalación y franja horaria");
  }

  private ReservaResponse toResponse(Reserva r) {
    InstalacionSnapshot s = r.getHorario().getInstalacionSnapshot();
    InstalacionSnapshotResponse snap = new InstalacionSnapshotResponse(
        s.getInstalacionId(), s.getNombre(), s.getDireccion(), s.getCiudad()
    );

    return new ReservaResponse(
        r.getId(),
        r.getFechaReserva(),
        r.getUsuarioId(),
        r.getHorario().getDia(),
        r.getHorario().getHoraInicio(),
        r.getHorario().getHoraFin(),
        snap
    );
  }
}