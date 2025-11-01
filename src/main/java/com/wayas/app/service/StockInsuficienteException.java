package com.wayas.app.service;

public class StockInsuficienteException extends Exception {
    public StockInsuficienteException(String message) {
        super(message);
    }
}