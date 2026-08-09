package com.picpaysimplificado.services;

import com.picpaysimplificado.DTOs.TransactionDTO;
import com.picpaysimplificado.domain.transaction.Transaction;
import com.picpaysimplificado.domain.user.User;
import com.picpaysimplificado.domain.user.UserType;
import com.picpaysimplificado.exception.TransactionNotAuthorizedException;
import com.picpaysimplificado.repositories.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final String AUTHORIZATION_URL = "https://run.mocky.io/v3/fake-url";

    @Mock
    private UserService userService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private NotificationService notificationService;

    private TransactionService transactionService;

    private User sender;
    private User receiver;
    private TransactionDTO transactionDTO;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService();
        ReflectionTestUtils.setField(transactionService, "userService", userService);
        ReflectionTestUtils.setField(transactionService, "transactionRepository", transactionRepository);
        ReflectionTestUtils.setField(transactionService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(transactionService, "notificationService", notificationService);
        ReflectionTestUtils.setField(transactionService, "authorizationUrl", AUTHORIZATION_URL);

        sender = new User(1L, "John", "Doe", "12345678900",
                "john@email.com", "password", new BigDecimal("100.00"), UserType.COMMON);
        receiver = new User(2L, "Jane", "Doe", "98765432100",
                "jane@email.com", "password", new BigDecimal("50.00"), UserType.COMMON);
        transactionDTO = new TransactionDTO(new BigDecimal("30.00"), sender.getId(), receiver.getId());
    }

    private void mockAuthorization(HttpStatus status, String message) {
        Map<String, String> body = message == null ? null : Map.of("message", message);
        when(restTemplate.getForEntity(AUTHORIZATION_URL, Map.class))
                .thenReturn(new ResponseEntity<>(body, status));
    }

    @Test
    void createTransaction_shouldPersistTransactionAndUpdateBalances_whenAuthorized() throws Exception {
        when(userService.findUserById(sender.getId())).thenReturn(sender);
        when(userService.findUserById(receiver.getId())).thenReturn(receiver);
        mockAuthorization(HttpStatus.OK, "Autorizado");

        Transaction result = transactionService.createTransaction(transactionDTO);

        assertThat(result.getAmount()).isEqualTo(transactionDTO.value());
        assertThat(result.getSender()).isEqualTo(sender);
        assertThat(result.getReceiver()).isEqualTo(receiver);
        assertThat(sender.getBalance()).isEqualByComparingTo("70.00");
        assertThat(receiver.getBalance()).isEqualByComparingTo("80.00");

        verify(transactionRepository, times(1)).save(result);
        verify(userService, times(1)).saveUser(sender);
        verify(userService, times(1)).saveUser(receiver);
        verify(notificationService, times(1)).sendNotification(eq(sender), anyString());
        verify(notificationService, times(1)).sendNotification(eq(receiver), anyString());
    }

    @Test
    void createTransaction_shouldThrowTransactionNotAuthorizedException_whenAuthorizationDenied() throws Exception {
        when(userService.findUserById(sender.getId())).thenReturn(sender);
        when(userService.findUserById(receiver.getId())).thenReturn(receiver);
        mockAuthorization(HttpStatus.OK, "Não autorizado");

        assertThrows(TransactionNotAuthorizedException.class,
                () -> transactionService.createTransaction(transactionDTO));

        verify(transactionRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(userService, never()).saveUser(org.mockito.ArgumentMatchers.any());
        verify(notificationService, never()).sendNotification(org.mockito.ArgumentMatchers.any(), anyString());
    }

    @Test
    void authorizedTransaction_shouldReturnTrue_whenResponseIsOkAndMessageIsAutorizado() {
        mockAuthorization(HttpStatus.OK, "Autorizado");

        boolean result = transactionService.authorizedTransaction(sender, transactionDTO.value());

        assertThat(result).isTrue();
    }

    @Test
    void authorizedTransaction_shouldReturnFalse_whenResponseIsOkButMessageIsNotAutorizado() {
        mockAuthorization(HttpStatus.OK, "Não autorizado");

        boolean result = transactionService.authorizedTransaction(sender, transactionDTO.value());

        assertThat(result).isFalse();
    }

    @Test
    void authorizedTransaction_shouldReturnFalse_whenResponseStatusIsNotOk() {
        mockAuthorization(HttpStatus.BAD_REQUEST, null);

        boolean result = transactionService.authorizedTransaction(sender, transactionDTO.value());

        assertThat(result).isFalse();
    }
}
