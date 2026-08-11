package com.picpaysimplificado.exception;

public class InvalidCpfException extends Exception {
    public InvalidCpfException(String message) {
        super(message);
    }

    public InvalidCpfException() {
        super("CPF inválido");
    }
}
