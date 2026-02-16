package com.iesvdc.dam.acceso.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.iesvdc.dam.acceso.model.Camiseta;
import com.iesvdc.dam.acceso.service.CamisetaService;
import com.iesvdc.dam.acceso.web.NotFoundException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/camisetas")
public class CamisetaController {

    @Autowired
    private CamisetaService camisetaService;

    @GetMapping({"","/"})
    public List<Camiseta> findAll() {
        return camisetaService.findAll();
    }

    @PostMapping({"","/"})
    @ResponseStatus(HttpStatus.CREATED)
    public Camiseta save(@Valid @RequestBody Camiseta camiseta) {
        return camisetaService.save(camiseta);
    }

    @GetMapping("/{id}")
    public Camiseta findById(@PathVariable String id) {
        return camisetaService.findById(id)
            .orElseThrow(() -> new NotFoundException("Camiseta no encontrada: " + id));
    }

    @PutMapping("/{id}")
    public Camiseta update(
        @PathVariable String id,
        @Valid @RequestBody Camiseta camiseta) {

        return camisetaService.updateById(id, camiseta);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        camisetaService.deleteById(id);
    }
}
