package com.omar.postify.service;

import com.omar.postify.entities.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final long TIMEOUT = 30 * 60 * 1000; // 30 minutes

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String username) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.computeIfAbsent(username, k -> new ArrayList<>()).add(emitter);

        emitter.onTimeout(() -> removeEmitter(username, emitter));
        emitter.onCompletion(() -> removeEmitter(username, emitter));
        emitter.onError(e -> removeEmitter(username, emitter));

        // initial event so client knows stream is alive
        sendEvent(emitter, NotificationPayload.builder()
                .type("init")
                .message("connected")
                .createdAt(LocalDateTime.now())
                .build());

        return emitter;
    }

    public void notifyUser(User user, String type, String message, String link) {
        if (user == null) return;
        NotificationPayload payload = NotificationPayload.builder()
                .type(type)
                .message(message)
                .link(link)
                .createdAt(LocalDateTime.now())
                .build();

        List<SseEmitter> userEmitters = emitters.get(user.getUsername());
        if (userEmitters == null) return;

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : userEmitters) {
            if (!sendEvent(emitter, payload)) {
                dead.add(emitter);
            }
        }
        userEmitters.removeAll(dead);
    }

    private boolean sendEvent(SseEmitter emitter, NotificationPayload payload) {
        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(payload));
            return true;
        } catch (IOException e) {
            emitter.complete();
            return false;
        }
    }

    private void removeEmitter(String username, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(username);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class NotificationPayload {
        private String type;
        private String message;
        private String link;
        private LocalDateTime createdAt;
    }
}
