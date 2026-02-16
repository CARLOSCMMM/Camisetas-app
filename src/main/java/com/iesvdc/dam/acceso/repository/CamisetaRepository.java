package com.iesvdc.dam.acceso.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.iesvdc.dam.acceso.model.Camiseta;

public interface CamisetaRepository extends MongoRepository<Camiseta, String>{
    
}
