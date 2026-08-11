package com.picpaysimplificado.infra;

import com.picpaysimplificado.DTOs.ExceptionDTO;
import com.picpaysimplificado.exception.InvalidCpfException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ControllerExceptionHandlerTest {

    private final ControllerExceptionHandler handler = new ControllerExceptionHandler();

    @Test
    void handleDuplicateEntry_shouldReturnBadRequestWithFriendlyMessage() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException("duplicate key");

        ResponseEntity<ExceptionDTO> response = handler.handleDuplicateEntry(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Usuário já cadastrado");
        assertThat(response.getBody().statusCode()).isEqualTo("400");
    }

    @Test
    void handleNotFoundEntity_shouldReturnNotFound() {
        EntityNotFoundException exception = new EntityNotFoundException("not found");

        ResponseEntity response = handler.handleNotFoundEntity(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleGenericException_shouldReturnInternalServerErrorWithExceptionMessage() {
        Exception exception = new Exception("something went wrong");

        ResponseEntity<ExceptionDTO> response = handler.handleGenericException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("something went wrong");
        assertThat(response.getBody().statusCode()).isEqualTo("500");
    }

    @Test
    void handleGenericException_shouldReturnInternalServerErrorWithFriendlyMessage_whenExceptionIsInvalidCpf() {
        InvalidCpfException exception = new InvalidCpfException();

        ResponseEntity<ExceptionDTO> response = handler.handleGenericException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("CPF inválido");
        assertThat(response.getBody().statusCode()).isEqualTo("500");
    }
}