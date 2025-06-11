package com.project.Teaming.global.messageQueue.consumer;

import com.project.Teaming.global.event.NotificationEvent;
import com.project.Teaming.global.sse.dto.EventPayload;
import com.project.Teaming.global.sse.dto.EventWithTeamPayload;
import com.project.Teaming.global.sse.entity.Notification;
import com.project.Teaming.global.sse.repository.EmitterRepository;
import com.project.Teaming.global.sse.repository.NotificationRepository;
import com.project.Teaming.global.sse.service.SseEmitterService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQNotificationConsumer {

    private final SseEmitterService sseEmitterService;
    private final EmitterRepository emitterRepository;
    private final NotificationRepository notificationRepository;

    @Value("${server.id}") // application.yml에서 설정된 SERVER_ID 값을 주입
    private String serverId;

    @Transactional(readOnly = true)
    @RabbitListener(queues = {"${server.id}"},
            containerFactory = "rabbitListenerContainerFactory")
    public void receiveNotification(NotificationEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {

        try {
            List<Notification> notifications = notificationRepository.findAllById(event.getNotificationIds());
            notifications.forEach(notification -> {
                Long userId = notification.getUser().getId();
                SseEmitter emitter = emitterRepository.findById(userId);

                if (emitter != null) {
                    // ✅ SSE 연결된 서버라면 알림 전송
                    sendNotification(userId, notification);
                    log.info("🚀 SSE 알림 전송 완료 → User: {}", userId);
                } else {
                    log.warn("⚠️ SSE 연결 없음 ");
                }
            });
            channel.basicAck(tag, false);
        } catch (Exception e) {
            channel.basicNack(tag, false,false);
            log.error("DLQ로 메세지 이동됨: {}", e.getMessage(), e);
        }
    }


        private void sendNotification(Long userId, Notification notification){
            try {
                Object payload = (notification.getTeamId() != null) ?
                        EventWithTeamPayload.builder()
                                .userId(userId)
                                .type(notification.getType())
                                .category(notification.getCategory())
                                .teamId(notification.getTeamId())
                                .createdAt(notification.getCreatedAt().toString())
                                .message(notification.getMessage())
                                .isRead(notification.isRead())
                                .build() :
                        EventPayload.builder()
                                .userId(userId)
                                .type(notification.getType())
                                .category(notification.getCategory())
                                .createdAt(notification.getCreatedAt().toString())
                                .message(notification.getMessage())
                                .isRead(notification.isRead())
                                .build();

                sseEmitterService.sendToClient(userId, payload);
            } catch (Exception e) {
                log.error("❌ SSE 알림 전송 실패: {}", e.getMessage(), e);
            }
        }
    }
