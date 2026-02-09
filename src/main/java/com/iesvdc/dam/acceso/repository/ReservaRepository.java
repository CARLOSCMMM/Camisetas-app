package com.iesvdc.dam.acceso.repository;

import com.iesvdc.dam.acceso.model.Reserva;

import java.time.LocalDate;

import org.springframework.data.mongodb.repository.MongoRepository;


public interface ReservaRepository extends MongoRepository<Reserva, String> {

    boolean existsByHorario_IdAndFechaReserva(String horarioId, LocalDate fechaReserva);
}