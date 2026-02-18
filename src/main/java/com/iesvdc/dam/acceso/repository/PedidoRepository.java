package com.iesvdc.dam.acceso.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.iesvdc.dam.acceso.model.Pedido;

public interface PedidoRepository extends MongoRepository<Pedido, String> {
}
