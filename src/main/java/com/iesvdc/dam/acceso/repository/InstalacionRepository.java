package com.iesvdc.dam.acceso.repository;


import com.iesvdc.dam.acceso.model.Instalacion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface InstalacionRepository extends MongoRepository<Instalacion, String> {

  List<Instalacion> findByCiudadIgnoreCase(String ciudad);

  List<Instalacion> findByNombreContainingIgnoreCase(String q);
}
