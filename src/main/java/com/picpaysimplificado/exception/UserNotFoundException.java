package com.picpaysimplificado.exception;

public class UserNotFoundException extends Exception {
    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException() {
        super("Usuário não encontrado");
    }
}
