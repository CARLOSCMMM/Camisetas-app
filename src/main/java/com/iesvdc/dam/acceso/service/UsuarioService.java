package com.iesvdc.dam.acceso.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.iesvdc.dam.acceso.model.Usuario;
import com.iesvdc.dam.acceso.repository.UsuarioRepository;
import com.iesvdc.dam.acceso.web.BadRequestException;
import com.iesvdc.dam.acceso.web.NotFoundException;
// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
        // usuario.setPassword(new BCryptPasswordEncoder().encode(usuario.getPassword()));
        return usuarioRepository.save(usuario);
    }

    public Usuario updateById(String id, Usuario usuario) {
        Optional<Usuario> oUsuario = usuarioRepository.findById(id);
        if (oUsuario.isPresent()) {
            usuario.setId(id);
            return usuarioRepository.save(usuario);
        } else {
            throw new NotFoundException("Usuario no encontrado: " + id);
        }
    }

    public Usuario updateById(Usuario oldUsuario, Usuario usuario) {
        return updateById(oldUsuario.getId(), usuario);
    }

    public void deleteById(String id) {
        try {
            usuarioRepository.deleteById(id);
        } catch (Exception e) {            
            throw new BadRequestException("Error eliminando usuario. El ID es obligatorio");
        }
    }

}
