package com.iesvdc.dam.acceso.repository;

import com.iesvdc.dam.acceso.model.Reserva;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReservaRepository extends MongoRepository<Reserva, String> {

  List<Reserva> findByUsuarioId(String usuarioId);

  List<Reserva> findByHorario_InstalacionSnapshot_InstalacionId(String instalacionId);

  List<Reserva> findByHorario_Dia(LocalDate dia);

  List<Reserva> findByHorario_InstalacionSnapshot_InstalacionIdAndHorario_Dia(String instalacionId, LocalDate dia);
}