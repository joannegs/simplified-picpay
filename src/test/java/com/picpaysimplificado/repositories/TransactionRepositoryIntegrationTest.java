package com.picpaysimplificado.repositories;

import com.picpaysimplificado.domain.transaction.Transaction;
import com.picpaysimplificado.domain.user.User;
import com.picpaysimplificado.domain.user.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TransactionRepositoryIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User sender;
    private User receiver;

    @BeforeEach
    void setUp() {
        sender = entityManager.persistAndFlush(new User(null, "John", "Doe", "12345678900",
                "john@email.com", "password", new BigDecimal("100.00"), UserType.COMMON));
        receiver = entityManager.persistAndFlush(new User(null, "Jane", "Doe", "98765432100",
                "jane@email.com", "password", new BigDecimal("50.00"), UserType.COMMON));
    }

    private Transaction buildTransaction() {
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("30.00"));
        transaction.setSender(sender);
        transaction.setReceiver(receiver);
        transaction.setTimestamp(LocalDateTime.now());
        return transaction;
    }

    @Test
    void save_shouldPersistTransactionWithSenderAndReceiver() {
        Transaction saved = transactionRepository.save(buildTransaction());

        assertThat(saved.getId()).isNotNull();

        Transaction found = entityManager.find(Transaction.class, saved.getId());
        assertThat(found.getAmount()).isEqualByComparingTo("30.00");
        assertThat(found.getSender().getId()).isEqualTo(sender.getId());
        assertThat(found.getReceiver().getId()).isEqualTo(receiver.getId());
    }

    @Test
    void findById_shouldReturnTransaction_whenExists() {
        Transaction saved = entityManager.persistAndFlush(buildTransaction());

        Optional<Transaction> result = transactionRepository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    void findById_shouldReturnEmpty_whenTransactionDoesNotExist() {
        Optional<Transaction> result = transactionRepository.findById(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_shouldReturnAllPersistedTransactions() {
        entityManager.persistAndFlush(buildTransaction());
        entityManager.persistAndFlush(buildTransaction());

        List<Transaction> result = transactionRepository.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void deleteById_shouldRemoveTransaction() {
        Transaction saved = entityManager.persistAndFlush(buildTransaction());

        transactionRepository.deleteById(saved.getId());

        assertThat(transactionRepository.findById(saved.getId())).isEmpty();
    }
}
