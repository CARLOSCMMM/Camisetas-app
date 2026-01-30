package com.iesvdc.dam.acceso.repository;

import com.iesvdc.dam.acceso.model.Horario;
import com.iesvdc.dam.acceso.model.Instalacion;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;


public interface HorarioRepository extends MongoRepository<Horario, String>{
    
    List<Horario> findByInstalacion(Instalacion instalacion);

    //* TODO:
    // Buscar instalaciones entre dos horas concretas */
    
}
