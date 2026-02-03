package com.iesvdc.dam.acceso.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.iesvdc.dam.acceso.model.Horario;
import com.iesvdc.dam.acceso.model.Instalacion;
import com.iesvdc.dam.acceso.service.HorarioService;
import com.iesvdc.dam.acceso.service.InstalacionService;
import com.iesvdc.dam.acceso.web.BadRequestException;
import com.iesvdc.dam.acceso.web.NotFoundException;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/horarios")
public class HorarioController {
    
    @Autowired 
    HorarioService horarioService;

    @Autowired 
    InstalacionService instalacionService;

    @GetMapping({"","/"})
    public List<Horario> findAll() {
        return horarioService.findAll();
    }

    @PostMapping({"","/"})
    @ResponseStatus(HttpStatus.CREATED)
    public Horario save(
        @RequestBody Horario horario){
        try {            
            Optional<Instalacion> inst = instalacionService.findById(horario.getInstalacion().getId());
            if (inst.isPresent()) {
                horario.setInstalacion(inst.get());                
                return horarioService.save(horario);
            } else {
                // error
                throw new NotFoundException(
                    "Instalación no encontrada.");
            }
        } catch (Exception e) {
            throw new BadRequestException("Formato de hora inválido.");
        }
        
    }

}
