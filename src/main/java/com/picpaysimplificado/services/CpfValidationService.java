package com.picpaysimplificado.services;

import br.com.caelum.stella.validation.CPFValidator;
import com.picpaysimplificado.exception.InvalidCpfException;
import org.springframework.stereotype.Service;

@Service
public class CpfValidationService {

    public void validate(String cpf) throws InvalidCpfException {
        CPFValidator cpfValidator = new CPFValidator();

        if (cpf == null || !cpfValidator.invalidMessagesFor(cpf).isEmpty()) {
            throw new InvalidCpfException();
        }
    }
}
