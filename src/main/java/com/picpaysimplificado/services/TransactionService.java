package com.picpaysimplificado.services;

import com.picpaysimplificado.DTOs.TransactionDTO;
import com.picpaysimplificado.domain.transaction.Transaction;
import com.picpaysimplificado.domain.user.User;
import com.picpaysimplificado.exception.TransactionNotAuthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.picpaysimplificado.repositories.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Service
public class TransactionService {
    @Autowired private UserService userService;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private RestTemplate restTemplate;
    @Autowired private NotificationService notificationService;

    @Value("${transaction.authorization.url}")
    private String authorizationUrl;

    @Transactional(rollbackFor = Exception.class)
    public Transaction createTransaction(TransactionDTO transactionDTO) throws Exception {
        User sender = this.userService.findUserById(transactionDTO.senderId());
        User receiver = this.userService.findUserById(transactionDTO.receiverId());

        userService.validateTransaction(sender, transactionDTO.value());

        if(!authorizedTransaction(sender, transactionDTO.value())) {
            throw new TransactionNotAuthorizedException();
        }

        Transaction transaction = new Transaction();
        transaction.setAmount(transactionDTO.value());
        transaction.setSender(sender);
        transaction.setReceiver(receiver);
        transaction.setTimestamp(LocalDateTime.now());

        sender.setBalance(sender.getBalance().subtract(transactionDTO.value()));
        receiver.setBalance(receiver.getBalance().add(transactionDTO.value()));

        this.transactionRepository.save(transaction);
        this.userService.saveUser(sender);
        this.userService.saveUser(receiver);

        this.notificationService.sendNotification(sender, "Transação realizada com sucesso");
        this.notificationService.sendNotification(receiver, "Transação recebida com sucesso");

        return transaction;
    }

    public boolean authorizedTransaction(User sender, BigDecimal value) {
        ResponseEntity<Map> authorizationResponse = restTemplate.getForEntity(authorizationUrl, Map.class);
        if(authorizationResponse.getStatusCode() == HttpStatus.OK) {
            return Objects.requireNonNull(authorizationResponse.getBody()).get("message").equals("Autorizado");
        } else return false;
    }
}
