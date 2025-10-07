package com.picpaysimplificado.exception;

public class TransactionNotAuthorizedException extends Exception {
    public TransactionNotAuthorizedException(String message) {
        super(message);
    }

    public TransactionNotAuthorizedException() {
        super("Transação não autorizada");
    }
}