package com.picpaysimplificado.services;

import com.picpaysimplificado.DTOs.UserDTO;
import com.picpaysimplificado.domain.user.User;
import com.picpaysimplificado.domain.user.UserType;
import com.picpaysimplificado.exception.InsufficientBalanceException;
import com.picpaysimplificado.exception.UnauthorizedUserException;
import com.picpaysimplificado.exception.UserNotFoundException;
import com.picpaysimplificado.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User commonUser;

    @BeforeEach
    void setUp() {
        commonUser = new User(1L, "John", "Doe", "12345678900",
                "john@email.com", "password", new BigDecimal("100.00"), UserType.COMMON);
    }

    @Test
    void validateTransaction_shouldThrowUnauthorizedUserException_whenSenderIsMerchant() {
        User merchant = new User(2L, "Store", "Inc", "00000000000",
                "store@email.com", "password", new BigDecimal("1000.00"), UserType.MERCHANT);

        assertThrows(UnauthorizedUserException.class,
                () -> userService.validateTransaction(merchant, new BigDecimal("10.00")));
    }

    @Test
    void validateTransaction_shouldThrowInsufficientBalanceException_whenBalanceIsLowerThanAmount() {
        assertThrows(InsufficientBalanceException.class,
                () -> userService.validateTransaction(commonUser, new BigDecimal("200.00")));
    }

    @Test
    void validateTransaction_shouldNotThrow_whenSenderIsCommonAndHasEnoughBalance() {
        assertDoesNotThrow(() -> userService.validateTransaction(commonUser, new BigDecimal("50.00")));
    }

    @Test
    void findUserById_shouldReturnUser_whenUserExists() throws Exception {
        when(userRepository.findUserById(1L)).thenReturn(Optional.of(commonUser));

        User result = userService.findUserById(1L);

        assertThat(result).isEqualTo(commonUser);
    }

    @Test
    void findUserById_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findUserById(anyLong())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.findUserById(99L));
    }

    @Test
    void createUser_shouldBuildUserFromDtoAndSaveIt() {
        UserDTO userDTO = new UserDTO("Jane", "Doe", "98765432100",
                new BigDecimal("300.00"), "jane@email.com", "password", UserType.COMMON);

        User result = userService.createUser(userDTO);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(captor.capture());

        assertThat(result.getFirstName()).isEqualTo(userDTO.firstName());
        assertThat(result.getEmail()).isEqualTo(userDTO.email());
        assertThat(captor.getValue()).isEqualTo(result);
    }

    @Test
    void getAllUsers_shouldReturnAllUsersFromRepository() {
        when(userRepository.findAll()).thenReturn(List.of(commonUser));

        List<User> result = userService.getAllUsers();

        assertThat(result).containsExactly(commonUser);
    }

    @Test
    void saveUser_shouldDelegateToRepository() {
        userService.saveUser(commonUser);

        verify(userRepository, times(1)).save(commonUser);
    }
}