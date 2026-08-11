package com.picpaysimplificado.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.picpaysimplificado.DTOs.UserDTO;
import com.picpaysimplificado.domain.user.User;
import com.picpaysimplificado.domain.user.UserType;
import com.picpaysimplificado.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private UserDTO buildUserDTO(String document, String email) {
        return new UserDTO("John", "Doe", document, new BigDecimal("100.00"),
                email, "password", UserType.COMMON);
    }

    @Test
    void createUser_shouldPersistUserAndReturnStatus201() throws Exception {
        UserDTO userDTO = buildUserDTO("12345678909", "john@email.com");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.email").value("john@email.com"))
                .andExpect(jsonPath("$.balance").value(100.00));

        assertThat(userRepository.findUserByDocument("12345678909")).isPresent();
    }

    @Test
    void createUser_shouldReturnBadRequest_whenDocumentAlreadyExists() throws Exception {
        userRepository.save(new User(
                buildUserDTO("12345678909", "existing@email.com")));

        UserDTO duplicated = buildUserDTO("12345678909", "new@email.com");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicated)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Usuário já cadastrado"))
                .andExpect(jsonPath("$.statusCode").value("400"));
    }

    @Test
    void createUser_shouldReturnBadRequest_whenEmailAlreadyExists() throws Exception {
        userRepository.save(new User(
                buildUserDTO("11111111111", "john@email.com")));

        UserDTO duplicated = buildUserDTO("11122233396", "john@email.com");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicated)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Usuário já cadastrado"))
                .andExpect(jsonPath("$.statusCode").value("400"));
    }

    @Test
    void createUser_shouldReturnInternalServerError_whenDocumentIsNotAValidCpf() throws Exception {
        UserDTO userDTO = buildUserDTO("12345678900", "invalid-cpf@email.com");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("CPF inválido"))
                .andExpect(jsonPath("$.statusCode").value("500"));

        assertThat(userRepository.findUserByDocument("12345678900")).isEmpty();
    }

    @Test
    void getAllUsers_shouldReturnEmptyList_whenNoUsersExist() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllUsers_shouldReturnAllPersistedUsers() throws Exception {
        userRepository.save(new User(
                buildUserDTO("12345678900", "john@email.com")));
        userRepository.save(new User(
                buildUserDTO("98765432100", "jane@email.com")));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].email").value(
                        containsInAnyOrder("john@email.com", "jane@email.com")));
    }
}
