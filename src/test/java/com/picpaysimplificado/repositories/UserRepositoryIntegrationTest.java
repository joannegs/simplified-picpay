package com.picpaysimplificado.repositories;

import com.picpaysimplificado.domain.user.User;
import com.picpaysimplificado.domain.user.UserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private User buildUser(String document, String email) {
        return new User(null, "John", "Doe", document, email,
                "password", new BigDecimal("100.00"), UserType.COMMON);
    }

    @Test
    void save_shouldPersistUserAndGenerateId() {
        User user = buildUser("12345678900", "john@email.com");

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(userRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void findUserById_shouldReturnUser_whenUserExists() {
        User saved = userRepository.save(buildUser("12345678900", "john@email.com"));

        Optional<User> result = userRepository.findUserById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("john@email.com");
    }

    @Test
    void findUserById_shouldReturnEmpty_whenUserDoesNotExist() {
        Optional<User> result = userRepository.findUserById(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void findUserByDocument_shouldReturnUser_whenUserExists() {
        userRepository.save(buildUser("12345678900", "john@email.com"));

        Optional<User> result = userRepository.findUserByDocument("12345678900");

        assertThat(result).isPresent();
        assertThat(result.get().getDocument()).isEqualTo("12345678900");
    }

    @Test
    void findUserByDocument_shouldReturnEmpty_whenUserDoesNotExist() {
        Optional<User> result = userRepository.findUserByDocument("00000000000");

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_shouldReturnAllPersistedUsers() {
        userRepository.save(buildUser("12345678900", "john@email.com"));
        userRepository.save(buildUser("98765432100", "jane@email.com"));

        List<User> result = userRepository.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void save_shouldThrowDataIntegrityViolationException_whenDocumentIsDuplicated() {
        userRepository.saveAndFlush(buildUser("12345678900", "john@email.com"));

        User duplicated = buildUser("12345678900", "other@email.com");

        assertThrows(DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(duplicated));
    }

    @Test
    void save_shouldThrowDataIntegrityViolationException_whenEmailIsDuplicated() {
        userRepository.saveAndFlush(buildUser("12345678900", "john@email.com"));

        User duplicated = buildUser("98765432100", "john@email.com");

        assertThrows(DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(duplicated));
    }

    @Test
    void deleteById_shouldRemoveUser() {
        User saved = userRepository.save(buildUser("12345678900", "john@email.com"));

        userRepository.deleteById(saved.getId());

        assertThat(userRepository.findById(saved.getId())).isEmpty();
    }
}
