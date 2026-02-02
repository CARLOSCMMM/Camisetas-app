package com.iesvdc.dam.acceso.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.iesvdc.dam.acceso.model.Usuario;
import com.iesvdc.dam.acceso.repository.UsuarioRepository;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    public List<Usuario> findAll(){
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> findById(String id) {
        return usuarioRepository.findById(id);
    }

    public Usuario add(Usuario usuario){        
        return usuarioRepository.save(usuario);
    }

}
