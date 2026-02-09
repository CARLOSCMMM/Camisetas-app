package com.iesvdc.dam.acceso.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import com.iesvdc.dam.acceso.model.Horario;
import com.iesvdc.dam.acceso.model.Reserva;
import com.iesvdc.dam.acceso.repository.HorarioRepository;
import com.iesvdc.dam.acceso.repository.ReservaRepository;
import com.iesvdc.dam.acceso.web.BadRequestException;

@Service
public class ReservaService {

    @Autowired
    ReservaRepository reservaRepository;
    @Autowired
    HorarioRepository horarioRepository;

    public List<Reserva> findAll() {
        return reservaRepository.findAll();
    }

    /**
     * No se pueden solapar reservas en el tiempo
     */
    public Reserva add(Reserva reserva) {
        if (reserva.getHorario() == null || reserva.getFechaReserva() == null) {
            throw new BadRequestException("El horario y la fecha de la reserva son obligatorios");
        }
        Optional<Horario> horarioOpt = horarioRepository.findById(reserva.getHorario().getId());
        if (horarioOpt.isEmpty()) {
            throw new BadRequestException("El horario indicado no existe");
        }
    
        if (reservaRepository.existsByHorario_IdAndFechaReserva(
                reserva.getHorario().getId(),
                reserva.getFechaReserva())) {
            throw new BadRequestException("No se pueden solapar reservas en el mismo horario y día");
        } else {
            reserva.setHorario(horarioOpt.get());
            return reservaRepository.save(reserva);
        }        
    }

    public void deleteById(String id) {
        if (id == null) {
            throw new BadRequestException("El ID de la reserva a eliminar es obligatorio");
        }
        reservaRepository.deleteById(id);
    }
}