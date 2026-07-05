package com.example.digitalbank.event;

import com.example.digitalbank.domain.Notification;
import com.example.digitalbank.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
public class NotificationListener {
    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationRepository notificationRepository;

    public NotificationListener(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTransferCompleted(TransferCompletedEvent event) {
        log.info("onTransferCompleted invoked for transfer {}", event.transferId());
        notificationRepository.save(build(event.fromAccountId(),
                "You sent " + event.amount() + " to account " + event.toAccountId()));
        notificationRepository.save(build(event.toAccountId(),
                "You received " + event.amount() + " from account " + event.fromAccountId()));
        log.info("onTransferCompleted finished for transfer {}", event.transferId());
    }

    private Notification build(UUID accountId, String message) {
        Notification notification = new Notification();
        notification.setAccountId(accountId);
        notification.setMessage(message);
        return notification;
    }
}