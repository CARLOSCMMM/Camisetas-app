package com.iesvdc.dam.acceso.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.iesvdc.dam.acceso.model.Horario;
import com.iesvdc.dam.acceso.repository.HorarioRepository;

@Service
public class HorarioService {
    @Autowired
    HorarioRepository horarioRepository;

    public List<Horario> findAll(){
        return horarioRepository.findAll();
    }

    public Horario save(Horario horario){
        return horarioRepository.save(horario);
    }

    

}
