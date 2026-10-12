package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.application.event.ApplicationRejectedEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.test.context.ActiveProfiles;

/**
 * A5-2 admin-approval-bulk (2026-10-12): {@link AdminApplicationBulkService#bulkReject} 검증.
 *
 * <ul>
 *   <li>Q1 A — 공통 사유 1건을 모든 선택 신청에 적용
 *   <li>Qn-9 — 성공 건마다 {@link ApplicationRejectedEvent} 발송
 *   <li>idempotent — 이미 REJECTED/CANCELLED 는 skip (이벤트 미발행)
 *   <li>잘못된 프로그램 ID → row 실패 (전체는 계속 진행)
 *   <li>reason blank → IllegalArgumentException
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("e2e")
class AdminApplicationBulkRejectServiceTest {

  @Autowired AdminApplicationBulkService bulkService;
  @Autowired ApplicationRepository applicationRepository;
  @Autowired ConfigurableApplicationContext context;

  private final AtomicInteger eventCount = new AtomicInteger(0);

  @BeforeEach
  void setup() {
    eventCount.set(0);
    SmartApplicationListener listener =
        new SmartApplicationListener() {
          @Override
          public boolean supportsEventType(
              Class<? extends org.springframework.context.ApplicationEvent> eventType) {
            return PayloadApplicationEvent.class.isAssignableFrom(eventType);
          }

          @Override
          public void onApplicationEvent(org.springframework.context.ApplicationEvent event) {
            if (event instanceof PayloadApplicationEvent<?> pae
                && pae.getPayload() instanceof ApplicationRejectedEvent) {
              eventCount.incrementAndGet();
            }
          }
        };
    context.addApplicationListener(listener);
  }

  @Test
  void bulkReject_pending_multiple_and_emits_events() {
    // program 2 PENDING 2건 대상
    List<Application> pendings =
        applicationRepository.findAll().stream()
            .filter(a -> a.getProgram().getId() == 2L)
            .filter(a -> a.getStatus() == ApplicationStatus.PENDING)
            .limit(2)
            .toList();
    if (pendings.size() < 2) return; // 시드 상태 의존 시 skip
    List<Long> ids = pendings.stream().map(Application::getId).toList();

    BulkResult result =
        bulkService.bulkReject(2L, ids, "sysadmin@youth-moa.test", "A5-2 테스트 반려 사유");
    assertThat(result.getSuccessCount()).isEqualTo(2);
    assertThat(result.getFailCount()).isEqualTo(0);
    assertThat(eventCount.get()).isEqualTo(2);
  }

  @Test
  void bulkReject_idempotent_skips_already_rejected() {
    // program 1 REJECTED 시드가 있을 수 있다. 없으면 skip
    List<Application> alreadyRejected =
        applicationRepository.findAll().stream()
            .filter(a -> a.getProgram().getId() == 1L)
            .filter(a -> a.getStatus() == ApplicationStatus.REJECTED)
            .limit(2)
            .toList();
    if (alreadyRejected.isEmpty()) return;
    List<Long> ids = alreadyRejected.stream().map(Application::getId).toList();

    BulkResult result =
        bulkService.bulkReject(1L, ids, "sysadmin@youth-moa.test", "회귀 테스트 사유");
    // 이미 REJECTED → idempotent (success 로 카운트, event 미발행)
    assertThat(result.getSuccessCount()).isEqualTo(alreadyRejected.size());
    assertThat(eventCount.get()).isEqualTo(0);
  }

  @Test
  void bulkReject_blank_reason_throws() {
    assertThatThrownBy(
            () -> bulkService.bulkReject(2L, List.of(1L), "sysadmin@youth-moa.test", "   "))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> bulkService.bulkReject(2L, List.of(1L), "sysadmin@youth-moa.test", null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void bulkReject_empty_ids_returns_zero() {
    BulkResult result =
        bulkService.bulkReject(2L, List.of(), "sysadmin@youth-moa.test", "사유");
    assertThat(result.getTotal()).isEqualTo(0);
    assertThat(eventCount.get()).isEqualTo(0);
  }

  @Test
  void bulkReject_wrong_program_id_fails_softly() {
    List<Application> pendings =
        applicationRepository.findAll().stream()
            .filter(a -> a.getProgram().getId() == 2L)
            .filter(a -> a.getStatus() == ApplicationStatus.PENDING)
            .limit(1)
            .toList();
    if (pendings.isEmpty()) return;
    Long aid = pendings.get(0).getId();
    // program 1 로 요청하면 program 2 의 신청은 fail
    BulkResult result =
        bulkService.bulkReject(1L, List.of(aid), "sysadmin@youth-moa.test", "잘못된 programId 테스트");
    assertThat(result.getFailCount()).isEqualTo(1);
    assertThat(eventCount.get()).isEqualTo(0);
  }
}
