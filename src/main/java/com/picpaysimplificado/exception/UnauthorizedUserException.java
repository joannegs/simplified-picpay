package com.picpaysimplificado.exception;

public class UnauthorizedUserException extends Exception {
    public UnauthorizedUserException(String message) {
        super(message);
    }

    public UnauthorizedUserException() {
        super("Usuário não autorizado a realizar transação");
    }
}