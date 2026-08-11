package com.picpaysimplificado.services;

import com.picpaysimplificado.exception.InvalidCpfException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CpfValidationServiceTest {

    private final CpfValidationService cpfValidationService = new CpfValidationService();

    @ParameterizedTest
    @ValueSource(strings = {"12345678909", "98765432100"})
    void validate_shouldNotThrow_whenCpfIsValid(String cpf) {
        assertDoesNotThrow(() -> cpfValidationService.validate(cpf));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678900", "11111111111", "00000000000", "123456789", "abcdefghijk"})
    void validate_shouldThrowInvalidCpfException_whenCpfIsInvalid(String cpf) {
        assertThrows(InvalidCpfException.class, () -> cpfValidationService.validate(cpf));
    }

    @Test
    void validate_shouldThrowInvalidCpfException_whenCpfIsNull() {
        assertThrows(InvalidCpfException.class, () -> cpfValidationService.validate(null));
    }

    @Test
    void validate_shouldThrowInvalidCpfException_withFriendlyMessage() {
        InvalidCpfException exception = assertThrows(InvalidCpfException.class,
                () -> cpfValidationService.validate("12345678900"));

        assertThat(exception.getMessage()).isEqualTo("CPF inválido");
    }
}
