package com.picpaysimplificado.controllers;

import com.picpaysimplificado.DTOs.UserDTO;
import com.picpaysimplificado.domain.user.User;
import com.picpaysimplificado.domain.user.UserType;
import com.picpaysimplificado.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    void createUser_shouldReturnCreatedUserWithStatus201() {
        UserDTO userDTO = new UserDTO("John", "Doe", "12345678900",
                new BigDecimal("100.00"), "john@email.com", "password", UserType.COMMON);
        User createdUser = new User(userDTO);
        when(userService.createUser(userDTO)).thenReturn(createdUser);

        ResponseEntity<User> response = userController.createUser(userDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(createdUser);
    }

    @Test
    void getAllUsers_shouldReturnAllUsersWithStatus200() {
        User user = new User(1L, "John", "Doe", "12345678900",
                "john@email.com", "password", new BigDecimal("100.00"), UserType.COMMON);
        when(userService.getAllUsers()).thenReturn(List.of(user));

        ResponseEntity<List<User>> response = userController.getAllUsers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(user);
    }
}