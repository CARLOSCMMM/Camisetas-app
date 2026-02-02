package com.iesvdc.dam.acceso.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iesvdc.dam.acceso.model.Instalacion;
import com.iesvdc.dam.acceso.service.InstalacionService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/instalaciones")
public class InstalacionController {
    
    @Autowired
    private InstalacionService instalacionService;

    @GetMapping({"","/"})
    public List<Instalacion> findAll() {
        return instalacionService.findAll();
    }
    
}
