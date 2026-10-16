package io.github.sihyuuun.youthmoa.notification.sse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * A7-realtime (2026-10-15): admin 알림 SSE emitter 레지스트리.
 *
 * <p>관리자 사용자 id 를 키로 활성 {@link SseEmitter} 목록을 보관한다. 한 관리자가 여러 탭을 열면 각 탭이 개별 emitter 로 등록된다
 * (CopyOnWriteArrayList — iteration 중 removal 안전).
 *
 * <p>스코프: 단일 인스턴스 메모리. 수평 확장 시 Redis pub/sub 로 broadcast 를 fan-out 해야 하며, 그 전까지는 범위 밖
 * (A7-realtime-scaling 로 이월).
 */
@Slf4j
@Component
public class SseConnectionRegistry {

  private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

  /** 활성 emitter 등록. onCompletion/onError/onTimeout 콜백에서 자동 제거. */
  public void register(Long userId, SseEmitter emitter) {
    emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
    emitter.onCompletion(() -> remove(userId, emitter, "complete"));
    emitter.onTimeout(
        () -> {
          emitter.complete();
          remove(userId, emitter, "timeout");
        });
    emitter.onError(
        e -> {
          remove(userId, emitter, "error");
        });
    log.debug("[A7-sse] registered userId={} totalForUser={}", userId, countFor(userId));
  }

  /** 명시적 unregister (controller 종료 경로용). */
  public void unregister(Long userId, SseEmitter emitter) {
    remove(userId, emitter, "manual");
  }

  private void remove(Long userId, SseEmitter emitter, String reason) {
    CopyOnWriteArrayList<SseEmitter> list = emitters.get(userId);
    if (list == null) return;
    boolean removed = list.remove(emitter);
    if (list.isEmpty()) {
      emitters.remove(userId);
    }
    if (removed) {
      log.debug(
          "[A7-sse] removed userId={} reason={} remaining={}", userId, reason, countFor(userId));
    }
  }

  /** 특정 관리자에게 이벤트 전송. send 실패한 emitter 는 즉시 제거. */
  public void broadcast(Long userId, String eventName, String data) {
    CopyOnWriteArrayList<SseEmitter> list = emitters.get(userId);
    if (list == null || list.isEmpty()) return;
    for (SseEmitter emitter : list) {
      try {
        emitter.send(SseEmitter.event().name(eventName).data(data));
      } catch (IOException | IllegalStateException e) {
        log.debug(
            "[A7-sse] broadcast failed userId={} event={} reason={}",
            userId,
            eventName,
            e.getMessage());
        remove(userId, emitter, "send-failed");
        try {
          emitter.complete();
        } catch (Exception ignore) {
          // 이미 종료된 emitter 는 무시
        }
      }
    }
  }

  /** heartbeat 등 전체 활성 emitter 에 공통 이벤트 전송. */
  public void broadcastAll(String eventName, String data) {
    for (Map.Entry<Long, CopyOnWriteArrayList<SseEmitter>> entry : emitters.entrySet()) {
      Long userId = entry.getKey();
      for (SseEmitter emitter : List.copyOf(entry.getValue())) {
        try {
          emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException | IllegalStateException e) {
          remove(userId, emitter, "heartbeat-failed");
          try {
            emitter.complete();
          } catch (Exception ignore) {
            // 이미 종료된 emitter 는 무시
          }
        }
      }
    }
  }

  /** 특정 관리자의 활성 연결 수. 테스트/관측용. */
  public int countFor(Long userId) {
    CopyOnWriteArrayList<SseEmitter> list = emitters.get(userId);
    return list == null ? 0 : list.size();
  }

  /** 전체 활성 연결 수. 테스트/관측용. */
  public int countAll() {
    return emitters.values().stream().mapToInt(List::size).sum();
  }
}
