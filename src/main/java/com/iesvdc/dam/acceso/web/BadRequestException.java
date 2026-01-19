package com.iesvdc.dam.acceso.web;

public class BadRequestException extends RuntimeException {
  public BadRequestException(String message) { super(message); }
}