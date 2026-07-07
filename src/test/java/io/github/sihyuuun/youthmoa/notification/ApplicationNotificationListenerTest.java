package io.github.sihyuuun.youthmoa.notification;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationService;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * F2b: {@link ApplicationNotificationListener} 통합 테스트.
 *
 * <p>ApplicationService 를 호출해 상태를 변경하면 AFTER_COMMIT 리스너가 실제로 Notification 을 저장하는지 검증한다.
 *
 * <p>@SpringBootTest 는 클래스 레벨 트랜잭션이 없으므로, ApplicationService 의 @Transactional 이 정상 커밋되어
 * AFTER_COMMIT 리스너가 실행된다.
 */
@SpringBootTest
@ActiveProfiles("e2e")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ApplicationNotificationListenerTest {

  @Autowired ApplicationService applicationService;
  @Autowired ApplicationRepository applicationRepository;
  @Autowired UserRepository userRepository;
  @Autowired ProgramRepository programRepository;
  @Autowired NotificationRepository notificationRepository;

  private User user;
  private User admin;
  private Program program;
  private Application pending;

  @BeforeEach
  void seed() {
    user =
        userRepository.save(
            User.builder()
                .email("f2b-listener-user@test.com")
                .password("hashed")
                .name("신청자")
                .role(UserRole.USER)
                .build());
    admin =
        userRepository.save(
            User.builder()
                .email("f2b-listener-admin@test.com")
                .password("hashed")
                .name("관리자")
                .role(UserRole.ADMIN)
                .build());

    LocalDate today = LocalDate.now();
    program =
        programRepository.save(
            Program.builder()
                .title("리스너테스트 프로그램")
                .organization("내일스퀘어")
                .category("취업")
                .region("수원시")
                .content("c")
                .startDate(today.minusDays(5))
                .endDate(today.plusDays(10))
                .capacity(30)
                .build());

    pending =
        applicationRepository.save(
            Application.builder().user(user).program(program).applyReason("신청합니다").build());
  }

  @Test
  @DisplayName("approve 커밋 후 APPLICATION_APPROVED 알림이 신청자에게 저장된다")
  void approve_creates_notification() {
    applicationService.approve(pending.getId(), admin.getEmail());

    List<Notification> all = notificationRepository.findAll();
    List<Notification> forUser =
        all.stream().filter(n -> n.getUser().getId().equals(user.getId())).toList();

    assertThat(forUser).hasSize(1);
    Notification n = forUser.get(0);
    assertThat(n.getType()).isEqualTo(NotificationType.APPLICATION_APPROVED);
    assertThat(n.getTitle()).isEqualTo("신청이 승인되었습니다");
    assertThat(n.getMessage()).contains("리스너테스트 프로그램").contains("승인");
    assertThat(n.getLink()).isEqualTo("/apply/complete?applicationId=" + pending.getId());
    assertThat(n.isRead()).isFalse();
  }

  @Test
  @DisplayName("reject 커밋 후 APPLICATION_REJECTED 알림 (사유 포함)")
  void reject_creates_notification() {
    applicationService.reject(pending.getId(), admin.getEmail(), "요건 미충족");

    List<Notification> forUser =
        notificationRepository.findAll().stream()
            .filter(n -> n.getUser().getId().equals(user.getId()))
            .toList();
    assertThat(forUser).hasSize(1);
    Notification n = forUser.get(0);
    assertThat(n.getType()).isEqualTo(NotificationType.APPLICATION_REJECTED);
    assertThat(n.getMessage()).contains("요건 미충족");
    assertThat(n.getLink()).isEqualTo("/programs/" + program.getId());
  }

  @Test
  @DisplayName("cancel 커밋 후 APPLICATION_CANCELLED 알림 (본인)")
  void cancel_creates_notification() {
    applicationService.cancel(pending.getId(), user.getEmail());

    List<Notification> forUser =
        notificationRepository.findAll().stream()
            .filter(n -> n.getUser().getId().equals(user.getId()))
            .toList();
    assertThat(forUser).hasSize(1);
    Notification n = forUser.get(0);
    assertThat(n.getType()).isEqualTo(NotificationType.APPLICATION_CANCELLED);
    assertThat(n.getLink()).isEqualTo("/programs/" + program.getId());
  }

  @Test
  @DisplayName("idempotent 호출은 알림도 1건만 생성")
  void idempotent_no_duplicate_notification() {
    applicationService.approve(pending.getId(), admin.getEmail());
    applicationService.approve(pending.getId(), admin.getEmail());

    // 시드 알림 포함되므로 user 로 필터 (다른 seed user 의 알림 제외)
    long approvedCount =
        notificationRepository.findAll().stream()
            .filter(n -> n.getUser().getId().equals(user.getId()))
            .filter(n -> n.getType() == NotificationType.APPLICATION_APPROVED)
            .count();
    assertThat(approvedCount).isEqualTo(1);
  }
}
