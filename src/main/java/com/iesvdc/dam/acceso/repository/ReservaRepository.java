package com.iesvdc.dam.acceso.repository;

import com.iesvdc.dam.acceso.model.Reserva;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface ReservaRepository extends MongoRepository<Reserva, String> {

  
}