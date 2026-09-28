package io.github.sihyuuun.youthmoa.notification.admin;

import io.github.sihyuuun.youthmoa.application.event.ApplicationCreatedEvent;
import io.github.sihyuuun.youthmoa.notification.watch.ProgramWatchRepository;
import io.github.sihyuuun.youthmoa.user.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * A7-watcher-ui (2026-09-28 · B-3-B): NEW_APPLICATION 수신자에 "지켜보기" 등록 admin 축 추가.
 *
 * <p>{@link ApplicationCreatedRecipientResolver} (B-2 · center CENTER_ADMIN) + {@link
 * CreatedByRecipientResolver} (B-3-A · 프로그램 작성자) 와 병행하여 {@link CompositeApplicationCreatedResolver}
 * 가 세 결과를 distinct union 한다.
 *
 * <h2>정책 (사용자 결정 Q-A7W)</h2>
 *
 * <ul>
 *   <li>Q-A7W-5 순서 A — Composite 는 B-2 → B-3-A → B-3-B 순 삽입. 본 Resolver 자체는 축만 반환
 *   <li>Q-A7W-6 A — 비활성 admin watcher 는 skip (Repository 쿼리에서 {@code admin.isActive=true} 필터).
 *       program_watch row 자체는 유지되며 admin 이 다시 활성화되면 자동 재개
 *   <li>Q-A7W-7 A — 프로그램이 SUSPENDED 여도 발송. 별도 필터 없음
 *   <li>{@code programId} null 방어 — 빈 리스트
 * </ul>
 *
 * <p>예외 격리 — 조회 실패 시 상위 Composite 의 {@code safeResolve} 가 catch 하여 다른 축까지 잃지 않도록 방어된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WatcherRecipientResolver
    implements NotificationRecipientResolver<ApplicationCreatedEvent> {

  private final ProgramWatchRepository programWatchRepository;

  @Override
  public List<User> resolve(ApplicationCreatedEvent event) {
    Long programId = event.programId();
    if (programId == null) {
      return List.of();
    }
    List<User> watchers = programWatchRepository.findAdminsByProgramId(programId);
    log.debug("[A7-B3B] watcher resolve programId={} count={}", programId, watchers.size());
    return watchers;
  }
}
