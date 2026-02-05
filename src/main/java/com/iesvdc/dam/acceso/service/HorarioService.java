package com.iesvdc.dam.acceso.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.iesvdc.dam.acceso.model.Horario;
import com.iesvdc.dam.acceso.repository.HorarioRepository;
import com.iesvdc.dam.acceso.web.BadRequestException;
import com.iesvdc.dam.acceso.web.ConflictException;

@Service
public class HorarioService {
    @Autowired
    HorarioRepository horarioRepository;

    public List<Horario> findAll(){
        return horarioRepository.findAll();
    }

    public Horario save(Horario horario){
        if (horarioRepository.existsByInstalacion_IdAndHoraInicioLessThanAndHoraFinGreaterThan(
            horario.getInstalacion().getId(),
            horario.getHoraInicio(),
            horario.getHoraInicio())
            |
            horarioRepository.existsByInstalacion_IdAndHoraInicioLessThanEqualAndHoraFinGreaterThanEqual(
            horario.getInstalacion().getId(),
            horario.getHoraFin(),
            horario.getHoraFin()) 
        ) {
            throw new ConflictException("No se pueden solapar horarios en la misma instalación");                
        } else {              
            return horarioRepository.save(horario);                       
        }
    }
    
    public void deleteById(String id) {
        try {
            horarioRepository.deleteById(id);
        } catch(Exception e) {
            throw new BadRequestException("El horario a borrar es obligatorio, no puede ser null.");
        }
    }
}
