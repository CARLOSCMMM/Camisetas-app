package com.iesvdc.dam.acceso.controller;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.iesvdc.dam.acceso.model.Horario;
import com.iesvdc.dam.acceso.model.Instalacion;
import com.iesvdc.dam.acceso.service.HorarioService;
import com.iesvdc.dam.acceso.service.InstalacionService;
import com.iesvdc.dam.acceso.web.BadRequestException;
import com.iesvdc.dam.acceso.web.NotFoundException;

import jakarta.validation.Valid;

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
        @RequestBody java.util.Map<String, String> body) {
        String instalacionId = body.get("instalacion");
        String horaInicio = body.get("horaInicio");
        String horaFin = body.get("horaFin");

        if (instalacionId == null || instalacionId.isBlank()) {
            throw new BadRequestException("instalacion es obligatoria");
        }
        if (horaInicio == null || horaInicio.isBlank()) {
            throw new BadRequestException("horaInicio es obligatoria");
        }
        if (horaFin == null || horaFin.isBlank()) {
            throw new BadRequestException("horaFin es obligatoria");
        }
        Horario hor = new Horario();
        try {
            hor.setHoraInicio(LocalTime.parse(horaInicio));
            hor.setHoraFin(LocalTime.parse(horaFin));
            Optional<Instalacion> inst = instalacionService.findById(instalacionId);
            if (inst.isPresent()) {
                hor.setInstalacion(inst.get());                
                return horarioService.save(hor);
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
