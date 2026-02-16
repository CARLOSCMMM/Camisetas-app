package com.iesvdc.dam.acceso.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.iesvdc.dam.acceso.model.Camiseta;
import com.iesvdc.dam.acceso.repository.CamisetaRepository;
import com.iesvdc.dam.acceso.web.NotFoundException;

@Service
public class CamisetaService {

    @Autowired
    private CamisetaRepository camisetaRepository;

    public List<Camiseta> findAll() {
        return camisetaRepository.findAll();
    }

    public Optional<Camiseta> findById(String id) {
        return camisetaRepository.findById(id);
    }

    public Camiseta save(Camiseta camiseta) {
        return camisetaRepository.save(camiseta);
    }

    public void delete(Camiseta camiseta) {
        if (camiseta.getId() != null) {
            deleteById(camiseta.getId());
        } else {
            throw new NotFoundException("Camiseta sin ID, no puedo buscarla.");
        }
    }

    public void deleteById(String id) {
        if (findById(id).isPresent()) {
            camisetaRepository.deleteById(id);
        } else {
            throw new NotFoundException("Camiseta no encontrada: " + id);
        }
    }

    public Camiseta updateById(String id, Camiseta camiseta) {
        Optional<Camiseta> oCamiseta = findById(id);
        if (oCamiseta.isPresent()) {
            camiseta.setId(id);
            return camisetaRepository.save(camiseta);
        } else {
            throw new NotFoundException("Camiseta no encontrada: " + id);
        }
    }

    public Camiseta updateById(Camiseta oldCamiseta, Camiseta camiseta) {
        return updateById(oldCamiseta.getId(), camiseta);
    }
}
