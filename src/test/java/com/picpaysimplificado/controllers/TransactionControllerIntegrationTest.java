package com.picpaysimplificado.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.picpaysimplificado.DTOs.TransactionDTO;
import com.picpaysimplificado.domain.user.User;
import com.picpaysimplificado.domain.user.UserType;
import com.picpaysimplificado.repositories.TransactionRepository;
import com.picpaysimplificado.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransactionControllerIntegrationTest {

    private static final String NOTIFICATION_URL = "https://run.mocky.io/v3/54dc2cf1-3add-45b5-b5a9-6bf7e7f1f4a6";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private RestTemplate restTemplate;

    @Value("${transaction.authorization.url}")
    private String authorizationUrl;

    private MockRestServiceServer mockServer;
    private User sender;
    private User receiver;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        sender = userRepository.save(new User(null, "John", "Doe", "12345678900",
                "john@email.com", "password", new BigDecimal("100.00"), UserType.COMMON));
        receiver = userRepository.save(new User(null, "Jane", "Doe", "98765432100",
                "jane@email.com", "password", new BigDecimal("50.00"), UserType.COMMON));
    }

    private void mockAuthorization(String message) {
        mockServer.expect(requestTo(authorizationUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"message\":\"" + message + "\"}", MediaType.APPLICATION_JSON));
    }

    private void mockAuthorizationServiceRespondsWithNonOkStatus() {
        mockServer.expect(requestTo(authorizationUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.ACCEPTED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{}"));
    }

    private void mockNotification(HttpStatus status) {
        mockServer.expect(requestTo(NOTIFICATION_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(status)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("ignored"));
    }

    private TransactionDTO dtoFor(BigDecimal value, Long senderId, Long receiverId) {
        return new TransactionDTO(value, senderId, receiverId);
    }

    @Test
    void createTransaction_shouldPersistTransactionAndUpdateBalances_whenAuthorized() throws Exception {
        mockAuthorization("Autorizado");
        mockNotification(HttpStatus.OK);
        mockNotification(HttpStatus.OK);

        TransactionDTO dto = dtoFor(new BigDecimal("30.00"), sender.getId(), receiver.getId());

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.amount").value(30.00));

        mockServer.verify();
        assertThat(userRepository.findById(sender.getId()).get().getBalance()).isEqualByComparingTo("70.00");
        assertThat(userRepository.findById(receiver.getId()).get().getBalance()).isEqualByComparingTo("80.00");
        assertThat(transactionRepository.findAll()).hasSize(1);
    }

    @Test
    void createTransaction_shouldReturnInternalServerError_whenSenderNotFound() throws Exception {
        TransactionDTO dto = dtoFor(new BigDecimal("30.00"), 999L, receiver.getId());

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Usuário não encontrado"));

        mockServer.verify();
    }

    @Test
    void createTransaction_shouldReturnInternalServerError_whenReceiverNotFound() throws Exception {
        TransactionDTO dto = dtoFor(new BigDecimal("30.00"), sender.getId(), 999L);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Usuário não encontrado"));

        mockServer.verify();
    }

    @Test
    void createTransaction_shouldReturnInternalServerError_whenSenderIsMerchant() throws Exception {
        User merchant = userRepository.save(new User(null, "Store", "Inc", "00000000000",
                "store@email.com", "password", new BigDecimal("1000.00"), UserType.MERCHANT));
        TransactionDTO dto = dtoFor(new BigDecimal("30.00"), merchant.getId(), receiver.getId());

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Usuário não autorizado a realizar transação"));

        mockServer.verify();
    }

    @Test
    void createTransaction_shouldReturnInternalServerError_whenBalanceIsInsufficient() throws Exception {
        TransactionDTO dto = dtoFor(new BigDecimal("500.00"), sender.getId(), receiver.getId());

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Saldo insuficiente"));

        mockServer.verify();
    }

    @Test
    void createTransaction_shouldReturnInternalServerError_whenAuthorizationIsDenied() throws Exception {
        mockAuthorization("Não autorizado");
        TransactionDTO dto = dtoFor(new BigDecimal("30.00"), sender.getId(), receiver.getId());

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Transação não autorizada"));

        mockServer.verify();
        assertThat(transactionRepository.findAll()).isEmpty();
    }

    @Test
    void createTransaction_shouldReturnInternalServerError_whenAuthorizationServiceRespondsWithError() throws Exception {
        mockAuthorizationServiceRespondsWithNonOkStatus();
        TransactionDTO dto = dtoFor(new BigDecimal("30.00"), sender.getId(), receiver.getId());

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Transação não autorizada"));

        mockServer.verify();
    }

    @Test
    void createTransaction_shouldRollbackBalancesAndTransaction_whenNotificationServiceIsUnavailable() throws Exception {
        userRepository.delete(sender);
        userRepository.delete(receiver);
        User rollbackSender = userRepository.save(new User(null, "Rollback", "Sender", "55555555555",
                "rollback.sender@email.com", "password", new BigDecimal("100.00"), UserType.COMMON));
        User rollbackReceiver = userRepository.save(new User(null, "Rollback", "Receiver", "66666666666",
                "rollback.receiver@email.com", "password", new BigDecimal("50.00"), UserType.COMMON));

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        mockAuthorization("Autorizado");
        mockNotification(HttpStatus.ACCEPTED);

        TransactionDTO dto = dtoFor(new BigDecimal("30.00"), rollbackSender.getId(), rollbackReceiver.getId());

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Serviço de notificação indisponível no momento"));

        mockServer.verify();

        TestTransaction.end();
        TestTransaction.start();

        assertThat(userRepository.findById(rollbackSender.getId()).get().getBalance()).isEqualByComparingTo("100.00");
        assertThat(userRepository.findById(rollbackReceiver.getId()).get().getBalance()).isEqualByComparingTo("50.00");
        assertThat(transactionRepository.findAll()).isEmpty();

        userRepository.delete(rollbackSender);
        userRepository.delete(rollbackReceiver);
        TestTransaction.flagForCommit();
        TestTransaction.end();
    }
}
