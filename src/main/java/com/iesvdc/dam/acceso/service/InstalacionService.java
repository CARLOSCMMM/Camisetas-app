package com.iesvdc.dam.acceso.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.iesvdc.dam.acceso.model.Instalacion;
import com.iesvdc.dam.acceso.repository.InstalacionRepository;
import com.iesvdc.dam.acceso.web.NotFoundException;

@Service
public class InstalacionService {
    
    @Autowired
    InstalacionRepository instalacionRepository;

    public List<Instalacion> findAll(){
        return instalacionRepository.findAll();
    }

    public Optional<Instalacion> findById(String id){
        return instalacionRepository.findById(id);
    }

    public Instalacion save(Instalacion instalacion){
        if (instalacion.getId().length()<5) {
            instalacion.setId(null);
        }
        return instalacionRepository.save(instalacion);
    }

    public void delete(Instalacion instalacion){
        if (instalacion.getId()!=null){
            deleteById(instalacion.getId());
        } else {
            throw new NotFoundException(
                "Instalación sin ID, no puedo buscarla.");
        }
    }

    public void deleteById(String id){
        if (findById(id).isPresent()){
            instalacionRepository.deleteById(id);
        } else {
            throw new NotFoundException(
                "Instalación no encontrada: " + id);
        }
    }

    public Instalacion updateById(String id, Instalacion instalacion){
        Optional<Instalacion> oInstalacion = findById(id);
        if(oInstalacion.isPresent()){
            instalacion.setId(id);
            return instalacionRepository.save(instalacion);
        } else {
            throw new NotFoundException(
                "Instalación no encontrada: " + id);
        }
    }

    public Instalacion updateById(Instalacion oldInstalacion, Instalacion instalacion){
        return updateById(oldInstalacion.getId(), instalacion);
    }

}
