package com.picpaysimplificado.exception;

public class NotificationServiceUnavailableException extends Exception {
    public NotificationServiceUnavailableException(String message) {
        super(message);
    }

    public NotificationServiceUnavailableException() {
        super("Serviço de notificação indisponível no momento");
    }
}