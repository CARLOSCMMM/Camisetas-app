package com.iesvdc.dam.acceso.repository;

import com.iesvdc.dam.acceso.model.Horario;
import com.iesvdc.dam.acceso.model.Instalacion;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalTime;
import java.util.List;


public interface HorarioRepository extends MongoRepository<Horario, String>{
    
    List<Horario> findByInstalacion(Instalacion instalacion);

   
    // Buscar instalaciones entre dos horas concretas */
    boolean existsByInstalacion_IdAndHoraInicioLessThanEqualAndHoraFinGreaterThanEqual(
        String instalacionId,      
        LocalTime horaInicio,
        LocalTime horaFin
    );

    boolean existsByInstalacion_IdAndHoraInicioLessThanAndHoraFinGreaterThan(
        String instalacionId,      
        LocalTime horaInicio,
        LocalTime horaFin
    );

    boolean existsByInstalacion_IdAndHoraInicioLessThanAndHoraFinGreaterThanEqual(
        String instalacionId,      
        LocalTime horaInicio,
        LocalTime horaFin
    );

    boolean existsByInstalacion_IdAndHoraInicioLessThanEqualAndHoraFinGreaterThan(
        String instalacionId,      
        LocalTime horaInicio,
        LocalTime horaFin
    );
}
