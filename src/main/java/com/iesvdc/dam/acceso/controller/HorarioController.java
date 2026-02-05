package com.iesvdc.dam.acceso.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.iesvdc.dam.acceso.model.Horario;
import com.iesvdc.dam.acceso.model.Instalacion;
import com.iesvdc.dam.acceso.service.HorarioService;
import com.iesvdc.dam.acceso.service.InstalacionService;
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

    /**
     * Precondiciones: Al crear un horario no se puede solapar con horarios existentes
     * para la instalación a la que le estamos asignando ese horario.
     * @param horario Da de alta un nuevo horario para la instalación que contiene
     * @return El mismo horario pero con los ObjectID nuevos de la BBDD
     */
    @PostMapping({"","/"})
    @ResponseStatus(HttpStatus.CREATED)
    public Horario save(
        @RequestBody Horario horario){
        
        Optional<Instalacion> inst = instalacionService.findById(horario.getInstalacion().getId());
        horario.setInstalacion(inst.get());
        return horarioService.save(horario);
    
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id){
        horarioService.deleteById(id);
    }
}
