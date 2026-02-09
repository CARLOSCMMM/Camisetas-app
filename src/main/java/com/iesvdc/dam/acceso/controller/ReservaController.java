package com.iesvdc.dam.acceso.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iesvdc.dam.acceso.model.Reserva;
import com.iesvdc.dam.acceso.service.ReservaService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;




@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    @Autowired 
    ReservaService reservaService;

    @GetMapping({"","/"})
    public List<Reserva> findAll(){
        return reservaService.findAll();
    }
    /**
     * Precondiciones: Al crear una reserva no se puede solapar con otra.
     * @param reserva Da de alta la reserva
     * @return El mismo objeto pero con los ObjectID nuevos de la BBDD
     */
    @PostMapping({"","/"})
    @ResponseStatus(HttpStatus.CREATED)
    public Reserva add(@RequestBody Reserva reserva){
        return reservaService.add(reserva);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id){
        reservaService.deleteById(id);
    }

}
