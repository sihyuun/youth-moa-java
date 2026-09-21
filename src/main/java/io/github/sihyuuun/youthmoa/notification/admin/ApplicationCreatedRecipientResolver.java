package io.github.sihyuuun.youthmoa.notification.admin;

import io.github.sihyuuun.youthmoa.application.event.ApplicationCreatedEvent;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * A7 admin 헤더 알림 벨 QC (2026-09-17): NEW_APPLICATION 수신자 결정 전략.
 *
 * <p>A9-a (2026-09-21): B-1 (organization 문자열 매칭) → B-3 (Center FK 매칭) 전환. {@code event.centerId} 와
 * {@code CENTER_ADMIN.center.id} 가 일치하는 활성 관리자에게만 fan-out. SYSTEM_ADMIN 은 QC B-1 결정 승계에 따라 알림에서 제외
 * (성수기 발송량 감소 + A6 대시보드로 커버).
 *
 * <h2>centerId 가 null 인 경우</h2>
 *
 * <p>Program.center FK 가 미할당(backfill 이전 · 초기 상태) 이면 매칭 대상 없음 → 빈 리스트 반환. A9-a Backfill 이 부팅 시 모든
 * 프로그램에 FK 를 채우므로 정상 상태에서는 발생하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class ApplicationCreatedRecipientResolver
    implements NotificationRecipientResolver<ApplicationCreatedEvent> {

  private final UserRepository userRepository;

  @Override
  public List<User> resolve(ApplicationCreatedEvent event) {
    Long centerId = event.centerId();
    if (centerId == null) {
      return List.of();
    }
    return userRepository.findByRoleAndIsActiveTrueAndCenter_Id(UserRole.CENTER_ADMIN, centerId);
  }
}
