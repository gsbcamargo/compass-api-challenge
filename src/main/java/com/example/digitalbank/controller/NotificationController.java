package com.example.digitalbank.controller;

import com.example.digitalbank.dto.response.NotificationResponse;
import com.example.digitalbank.exception.ForbiddenAccessException;
import com.example.digitalbank.repository.NotificationRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/{accountId}")
    public List<NotificationResponse> list(@PathVariable UUID accountId, Authentication authentication) {
        if (!accountId.equals(authentication.getPrincipal())) {
            throw new ForbiddenAccessException("You can only view your own notifications");
        }
        return notificationRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
                .map(notification ->
                        new NotificationResponse(
                                notification.getId(), notification.getMessage(), notification.getCreatedAt())).
                toList();
    }
}
