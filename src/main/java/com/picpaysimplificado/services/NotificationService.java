package com.picpaysimplificado.services;


import com.picpaysimplificado.DTOs.NotificationDTO;
import com.picpaysimplificado.domain.user.User;
import com.picpaysimplificado.exception.NotificationServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class NotificationService {

    @Autowired
    private RestTemplate restTemplate;

    public void sendNotification(User user, String message) throws NotificationServiceUnavailableException {
        String email = user.getEmail();
        NotificationDTO notificationDTO = new NotificationDTO(email, message);
        ResponseEntity<String> notificationResponse = restTemplate.postForEntity("https://run.mocky.io/v3/54dc2cf1-3add-45b5-b5a9-6bf7e7f1f4a6", notificationDTO, String.class);

        if(!(notificationResponse.getStatusCode() == HttpStatus.OK)) {
            throw new NotificationServiceUnavailableException();
        }
    }
}
