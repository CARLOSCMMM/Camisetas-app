package com.iesvdc.dam.acceso.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import com.iesvdc.dam.acceso.model.Reserva;
import com.iesvdc.dam.acceso.repository.ReservaRepository;

@Service
public class ReservaService {
    
    @Autowired 
    ReservaRepository reservaRepository;

    public List<Reserva> findAll(){
        return reservaRepository.findAll();
    }

    /**
    * No se pueden solapar reservas en el tiempo
    */
    public Reserva add(@RequestBody Reserva reserva){
        return reservaRepository.save(reserva);
    }
}