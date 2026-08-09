package com.picpaysimplificado.services;

import com.picpaysimplificado.DTOs.NotificationDTO;
import com.picpaysimplificado.domain.user.User;
import com.picpaysimplificado.domain.user.UserType;
import com.picpaysimplificado.exception.NotificationServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(1L, "John", "Doe", "12345678900",
                "john@email.com", "password", new BigDecimal("100.00"), UserType.COMMON);
    }

    @Test
    void sendNotification_shouldNotThrow_whenResponseStatusIsOk() {
        when(restTemplate.postForEntity(anyString(), any(NotificationDTO.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        assertDoesNotThrow(() -> notificationService.sendNotification(user, "Transação realizada com sucesso"));
    }

    @Test
    void sendNotification_shouldSendMessageWithUserEmailAndGivenMessage() throws NotificationServiceUnavailableException {
        when(restTemplate.postForEntity(anyString(), any(NotificationDTO.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        notificationService.sendNotification(user, "Transação realizada com sucesso");

        ArgumentCaptor<NotificationDTO> captor = ArgumentCaptor.forClass(NotificationDTO.class);
        org.mockito.Mockito.verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(String.class));

        assertThat(captor.getValue().email()).isEqualTo(user.getEmail());
        assertThat(captor.getValue().message()).isEqualTo("Transação realizada com sucesso");
    }

    @Test
    void sendNotification_shouldThrowNotificationServiceUnavailableException_whenResponseStatusIsNotOk() {
        when(restTemplate.postForEntity(anyString(), any(NotificationDTO.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(NotificationServiceUnavailableException.class,
                () -> notificationService.sendNotification(user, "Transação realizada com sucesso"));
    }
}