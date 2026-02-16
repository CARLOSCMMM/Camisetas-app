package com.iesvdc.dam.acceso.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.iesvdc.dam.acceso.model.Usuario;
import com.iesvdc.dam.acceso.service.UsuarioService;
import com.iesvdc.dam.acceso.web.NotFoundException;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;




@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    
    @Autowired
    UsuarioService usuarioService;

    @GetMapping({"","/"})
    public List<Usuario> findAll() {
        return usuarioService.findAll();
    }

    @PostMapping({"","/"})
    public Usuario save(
        @Valid @RequestBody Usuario usuario) {        
        
        return usuarioService.add(usuario);
    }

    @GetMapping("/{id}")
    public Usuario findById(@PathVariable String id) {
        return usuarioService.findById(id)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + id));
    }

    @PutMapping("/{id}")
    public Usuario update(
        @PathVariable String id,
        @Valid @RequestBody Usuario usuario) {

        return usuarioService.updateById(id, usuario);
    }
    
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id){
        usuarioService.deleteById(id);    
    }
    
}
