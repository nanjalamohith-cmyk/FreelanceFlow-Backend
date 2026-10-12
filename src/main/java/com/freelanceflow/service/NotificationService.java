package com.freelanceflow.service;

import com.freelanceflow.dto.NotificationResponse;
import com.freelanceflow.model.Notification;
import com.freelanceflow.model.User;
import com.freelanceflow.repository.NotificationRepository;
import com.freelanceflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;

    public void createNotification(String userId, String type, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .message(message)
                .read(false)
                .build();
        Notification saved = notificationRepository.save(notification);

        // Push real-time alert via WebSocket
        try {
            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/notifications",
                    NotificationResponse.fromNotification(saved)
            );
            // Also broadcast to public user topic as fallback
            messagingTemplate.convertAndSend("/topic/user/" + userId + "/notifications", NotificationResponse.fromNotification(saved));
        } catch (Exception e) {
            log.warn("Failed to send WebSocket notification to user {}: {}", userId, e.getMessage());
        }

        // Send non-fatal email
        userRepository.findById(userId).ifPresent(user -> {
            emailService.sendEmail(user.getEmail(), "FreelanceFlow Alert: " + type, message);
        });
    }

    public List<NotificationResponse> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::fromNotification)
                .toList();
    }

    public void markAsRead(String userId, String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notif -> {
            if (notif.getUserId().equals(userId)) {
                notif.setRead(true);
                notificationRepository.save(notif);
            }
        });
    }

    public void markAllAsRead(String userId) {
        List<Notification> notifs = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        notifs.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifs);
    }
}
