package com.picpaysimplificado.controllers;

import com.picpaysimplificado.DTOs.TransactionDTO;
import com.picpaysimplificado.domain.transaction.Transaction;
import com.picpaysimplificado.services.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    @Test
    void createTransaction_shouldReturnCreatedTransactionWithStatus201() throws Exception {
        TransactionDTO transactionDTO = new TransactionDTO(new BigDecimal("30.00"), 1L, 2L);
        Transaction transaction = new Transaction();
        transaction.setAmount(transactionDTO.value());
        transaction.setTimestamp(LocalDateTime.now());
        when(transactionService.createTransaction(transactionDTO)).thenReturn(transaction);

        ResponseEntity<Transaction> response = transactionController.createTransaction(transactionDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(transaction);
    }
}