package com.picpaysimplificado.services;

import com.picpaysimplificado.DTOs.UserDTO;
import com.picpaysimplificado.domain.user.User;
import com.picpaysimplificado.domain.user.UserType;
import com.picpaysimplificado.exception.InsufficientBalanceException;
import com.picpaysimplificado.exception.UnauthorizedUserException;
import com.picpaysimplificado.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.picpaysimplificado.repositories.UserRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    @Autowired private UserRepository userRepository;

    public void validateTransaction(User sender, BigDecimal amount) throws Exception {
        if(sender.getUserType() == UserType.MERCHANT) {
            throw new UnauthorizedUserException();
        }

        if(sender.getBalance().compareTo(amount)  < 0) {
            throw new InsufficientBalanceException();
        }
    }

    public User findUserById(Long id) throws Exception {
        return this.userRepository.findUserById(id).orElseThrow(UserNotFoundException::new);
    }

    public User createUser(UserDTO data) {
        User newUser = new User(data);
        this.saveUser(newUser);

        return newUser;
    }

    public List<User> getAllUsers(){
        return this.userRepository.findAll();
    }

    public void saveUser(User user) {
        this.userRepository.save(user);
    }
}
