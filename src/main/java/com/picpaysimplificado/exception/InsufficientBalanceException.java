package com.picpaysimplificado.exception;

public class InsufficientBalanceException extends Exception {
    public InsufficientBalanceException(String message) {
        super(message);
    }

    public InsufficientBalanceException() {
        super("Saldo insuficiente");
    }
}