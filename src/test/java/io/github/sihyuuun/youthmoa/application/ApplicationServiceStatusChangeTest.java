package io.github.sihyuuun.youthmoa.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sihyuuun.youthmoa.application.event.ApplicationApprovedEvent;
import io.github.sihyuuun.youthmoa.application.event.ApplicationCancelledEvent;
import io.github.sihyuuun.youthmoa.application.event.ApplicationRejectedEvent;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

/**
 * F2b: 상태 변경 서비스 메서드가 도메인 이벤트를 발행하는지 검증.
 *
 * <p>{@code @RecordApplicationEvents} — Spring TestContext 가 발행된 모든 ApplicationEvent 를 캡처. {@code
 * ApplicationEvents} 는 발행 시점에 즉시 기록되므로 트랜잭션 롤백 후에도 검증 가능.
 *
 * <p>{@code @Transactional} — 각 테스트 후 롤백해 seed 데이터 unique 충돌 방지. AFTER_COMMIT 리스너는 롤백 시 실행되지 않으나
 * ApplicationEvents 는 publish 시점 캡처라 이벤트 발행 여부는 검증 가능.
 */
@SpringBootTest
@ActiveProfiles("e2e")
@RecordApplicationEvents
@Transactional
class ApplicationServiceStatusChangeTest {

  @Autowired ApplicationService applicationService;
  @Autowired ApplicationRepository applicationRepository;
  @Autowired UserRepository userRepository;
  @Autowired ProgramRepository programRepository;
  @Autowired ApplicationEvents events;

  private User user;
  private User admin;
  private Program program;
  private Application pending;

  @BeforeEach
  void seed() {
    user =
        userRepository.save(
            User.builder()
                .email("f2b-user@test.com")
                .password("hashed")
                .name("신청자")
                .role(UserRole.USER)
                .build());
    admin =
        userRepository.save(
            User.builder()
                .email("f2b-admin@test.com")
                .password("hashed")
                .name("관리자")
                .role(UserRole.ADMIN)
                .build());

    LocalDate today = LocalDate.now();
    program =
        programRepository.save(
            Program.builder()
                .title("F2b 테스트 프로그램")
                .organization("내일스퀘어")
                .category("취업")
                .region("수원시")
                .content("c")
                .requirements("r")
                .startDate(today.minusDays(5))
                .endDate(today.plusDays(10))
                .capacity(30)
                .build());

    pending =
        applicationRepository.save(
            Application.builder().user(user).program(program).applyReason("신청합니다").build());
  }

  @Test
  @DisplayName("approve 시 ApplicationApprovedEvent 1건이 발행되고 payload 가 일치한다")
  void approve_publishes_event() {
    applicationService.approve(pending.getId(), admin.getEmail());

    long count = events.stream(ApplicationApprovedEvent.class).count();
    assertThat(count).isEqualTo(1);

    ApplicationApprovedEvent event =
        events.stream(ApplicationApprovedEvent.class).findFirst().orElseThrow();
    assertThat(event.applicationId()).isEqualTo(pending.getId());
    assertThat(event.userId()).isEqualTo(user.getId());
    assertThat(event.programId()).isEqualTo(program.getId());
    assertThat(event.programTitle()).isEqualTo("F2b 테스트 프로그램");

    Application reloaded = applicationRepository.findById(pending.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
  }

  @Test
  @DisplayName("reject 시 ApplicationRejectedEvent 발행 + rejectReason payload 포함")
  void reject_publishes_event() {
    applicationService.reject(pending.getId(), admin.getEmail(), "요건 미충족");

    ApplicationRejectedEvent event =
        events.stream(ApplicationRejectedEvent.class).findFirst().orElseThrow();
    assertThat(event.applicationId()).isEqualTo(pending.getId());
    assertThat(event.userId()).isEqualTo(user.getId());
    assertThat(event.rejectReason()).isEqualTo("요건 미충족");

    Application reloaded = applicationRepository.findById(pending.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
  }

  @Test
  @DisplayName("cancel (본인) 시 ApplicationCancelledEvent 발행")
  void cancel_publishes_event() {
    applicationService.cancel(pending.getId(), user.getEmail());

    long count = events.stream(ApplicationCancelledEvent.class).count();
    assertThat(count).isEqualTo(1);

    Application reloaded = applicationRepository.findById(pending.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
  }

  @Test
  @DisplayName("cancel: 본인이 아니면 IllegalStateException + 이벤트 미발행")
  void cancel_by_other_user_blocked() {
    User other =
        userRepository.save(
            User.builder()
                .email("f2b-other@test.com")
                .password("hashed")
                .name("타인")
                .role(UserRole.USER)
                .build());

    assertThatThrownBy(() -> applicationService.cancel(pending.getId(), other.getEmail()))
        .isInstanceOf(IllegalStateException.class);

    assertThat(events.stream(ApplicationCancelledEvent.class).count()).isZero();
  }

  @Test
  @DisplayName("idempotent: 이미 APPROVED 상태에서 approve 재호출 시 이벤트 미발행")
  void approve_idempotent() {
    applicationService.approve(pending.getId(), admin.getEmail());
    applicationService.approve(pending.getId(), admin.getEmail());

    assertThat(events.stream(ApplicationApprovedEvent.class).count()).isEqualTo(1);
  }

  @Test
  @DisplayName("존재하지 않는 applicationId → IllegalArgumentException + 이벤트 미발행")
  void approve_unknown_id() {
    assertThatThrownBy(() -> applicationService.approve(999_999L, admin.getEmail()))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(events.stream(ApplicationApprovedEvent.class).count()).isZero();
  }
}
