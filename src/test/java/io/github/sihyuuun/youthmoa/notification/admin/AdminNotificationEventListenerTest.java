package io.github.sihyuuun.youthmoa.notification.admin;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationService;
import io.github.sihyuuun.youthmoa.application.ApplyRequest;
import io.github.sihyuuun.youthmoa.center.Center;
import io.github.sihyuuun.youthmoa.center.CenterRepository;
import io.github.sihyuuun.youthmoa.notification.Notification;
import io.github.sihyuuun.youthmoa.notification.NotificationRepository;
import io.github.sihyuuun.youthmoa.notification.NotificationType;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.user.SignUpRequest;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import io.github.sihyuuun.youthmoa.user.UserService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * A7 admin 헤더 알림 벨 (2026-09-17): admin fan-out 리스너 통합 테스트.
 *
 * <p>{@code ApplicationService.apply} / {@code UserService.signUp} 호출 후 AFTER_COMMIT 리스너가 실제 관리자에게
 * Notification row 를 fan-out INSERT 하는지 검증한다. Resolver 정책 (B-1 · SYSTEM_ADMIN 전원) 도 함께 확인.
 */
@SpringBootTest
@ActiveProfiles("e2e")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AdminNotificationEventListenerTest {

  @Autowired ApplicationService applicationService;
  @Autowired UserService userService;
  @Autowired ApplicationRepository applicationRepository;
  @Autowired UserRepository userRepository;
  @Autowired ProgramRepository programRepository;
  @Autowired CenterRepository centerRepository;
  @Autowired NotificationRepository notificationRepository;

  private User applicant;
  private User centerAdminA;
  private User centerAdminBOther;
  private User systemAdmin1;
  private User systemAdmin2;
  private Center centerA;
  private Center centerB;
  private Program programA;

  @BeforeEach
  void seed() {
    centerA =
        centerRepository.save(Center.builder().name("내일스퀘어").region("수원시").isActive(true).build());
    centerB =
        centerRepository.save(Center.builder().name("타센터").region("성남시").isActive(true).build());

    applicant =
        userRepository.save(
            User.builder()
                .email("a7-applicant@test.com")
                .password("hashed")
                .name("신청자")
                .role(UserRole.USER)
                .build());
    centerAdminA =
        userRepository.save(
            User.builder()
                .email("a7-center-admin-a@test.com")
                .password("hashed")
                .name("센터관리자A")
                .role(UserRole.CENTER_ADMIN)
                .center(centerA)
                .build());
    centerAdminBOther =
        userRepository.save(
            User.builder()
                .email("a7-center-admin-b@test.com")
                .password("hashed")
                .name("센터관리자B")
                .role(UserRole.CENTER_ADMIN)
                .center(centerB)
                .build());
    systemAdmin1 =
        userRepository.save(
            User.builder()
                .email("a7-sys-admin-1@test.com")
                .password("hashed")
                .name("시스템관리자1")
                .role(UserRole.SYSTEM_ADMIN)
                .build());
    systemAdmin2 =
        userRepository.save(
            User.builder()
                .email("a7-sys-admin-2@test.com")
                .password("hashed")
                .name("시스템관리자2")
                .role(UserRole.SYSTEM_ADMIN)
                .build());

    LocalDate today = LocalDate.now();
    programA =
        programRepository.save(
            Program.builder()
                .title("A7 프로그램")
                // A9-a: Center FK 로 전환. organization 은 Builder 에서 center.name 으로 자동 동기화됨.
                .center(centerA)
                .category("취업")
                .region("수원시")
                .content("c")
                .startDate(today.minusDays(1))
                .endDate(today.plusDays(30))
                .capacity(30)
                .build());
  }

  @Test
  @DisplayName(
      "A9-a: apply 성공 시 program.center 매칭 CENTER_ADMIN 에게만 NEW_APPLICATION 발행 (SYSTEM_ADMIN 제외)")
  void apply_creates_notification_for_matching_center_admin_only() {
    ApplyRequest req = new ApplyRequest();
    req.setApplyReason("잘 부탁드립니다");

    applicationService.apply(applicant.getEmail(), programA.getId(), req);

    List<Notification> newApplication =
        notificationRepository.findAll().stream()
            .filter(n -> n.getType() == NotificationType.NEW_APPLICATION)
            .toList();

    // QC B-1: 매칭 CENTER_ADMIN 1건만. 다른 센터 관리자·SYSTEM_ADMIN 은 제외.
    assertThat(newApplication).hasSize(1);
    Notification n = newApplication.get(0);
    assertThat(n.getUser().getId()).isEqualTo(centerAdminA.getId());
    assertThat(n.getTitle()).isEqualTo("새 신청이 접수됐어요");
    assertThat(n.getMessage()).contains("A7 프로그램");
    assertThat(n.getLink()).contains("/admin/programs/").contains("/applications");
    assertThat(n.isRead()).isFalse();

    // 다른 센터 CENTER_ADMIN · SYSTEM_ADMIN 은 수신하지 않음
    List<Notification> other =
        newApplication.stream()
            .filter(
                x ->
                    x.getUser().getId().equals(centerAdminBOther.getId())
                        || x.getUser().getId().equals(systemAdmin1.getId())
                        || x.getUser().getId().equals(systemAdmin2.getId()))
            .toList();
    assertThat(other).isEmpty();
  }

  @Test
  @DisplayName("apply cancel 후 재신청도 NEW_APPLICATION 알림을 재발행한다")
  void reapply_also_creates_notification() {
    ApplyRequest req = new ApplyRequest();
    req.setApplyReason("첫 신청");
    Application app = applicationService.apply(applicant.getEmail(), programA.getId(), req);
    applicationService.cancel(app.getId(), applicant.getEmail());

    ApplyRequest req2 = new ApplyRequest();
    req2.setApplyReason("재신청");
    applicationService.apply(applicant.getEmail(), programA.getId(), req2);

    long newAppCount =
        notificationRepository.findAll().stream()
            .filter(n -> n.getType() == NotificationType.NEW_APPLICATION)
            .filter(n -> n.getUser().getId().equals(centerAdminA.getId()))
            .count();
    assertThat(newAppCount).isEqualTo(2);
  }

  @Test
  @DisplayName("signUp 성공 시 SYSTEM_ADMIN 전원에게만 NEW_USER 알림 fan-out (CENTER_ADMIN 제외)")
  void signup_creates_notification_for_system_admins_only() {
    SignUpRequest req = new SignUpRequest();
    req.setEmail("a7-new-signup@test.com");
    req.setPassword("Passw0rd!");
    req.setName("신규유저");
    req.setPhone("01099998888");
    // DataInitializer 가 SERVICE·PRIVACY 필수 약관을 시드하므로 동의 필요.
    Map<String, Boolean> agreements = new java.util.HashMap<>();
    agreements.put("SERVICE", true);
    agreements.put("PRIVACY", true);
    req.setAgreements(agreements);

    userService.signUp(req);

    List<Notification> newUser =
        notificationRepository.findAll().stream()
            .filter(n -> n.getType() == NotificationType.NEW_USER)
            .toList();

    // 내가 만든 SYSTEM_ADMIN 2명은 반드시 포함되어야 함. DataInitializer 가 시드하는 SYSTEM_ADMIN 이 있으면 count 는 그보다 클 수
    // 있음.
    assertThat(newUser)
        .extracting(n -> n.getUser().getId())
        .contains(systemAdmin1.getId(), systemAdmin2.getId());
    // CENTER_ADMIN 은 절대 수신 X (Qn-E)
    assertThat(newUser)
        .extracting(n -> n.getUser().getId())
        .doesNotContain(centerAdminA.getId(), centerAdminBOther.getId());
    assertThat(newUser)
        .allSatisfy(
            n -> {
              assertThat(n.getTitle()).isEqualTo("새 회원이 가입했어요");
              assertThat(n.getLink()).startsWith("/admin/users/");
              assertThat(n.isRead()).isFalse();
            });
  }

  @Test
  @DisplayName("A9-a: center FK 미할당 프로그램 신청 시 알림 수신자 없음 (로그만 남고 apply 는 성공)")
  void apply_with_unmatched_organization_creates_no_notifications() {
    LocalDate today = LocalDate.now();
    // center 미할당 (organization 문자열만 있는 상태 — A9-a 이전 형태 재현)
    Program orphan =
        programRepository.save(
            Program.builder()
                .title("orphan")
                .organization("없는센터명")
                .category("취업")
                .region("수원시")
                .content("c")
                .startDate(today.minusDays(1))
                .endDate(today.plusDays(30))
                .capacity(30)
                .build());

    ApplyRequest req = new ApplyRequest();
    req.setApplyReason("test");
    Application saved = applicationService.apply(applicant.getEmail(), orphan.getId(), req);

    assertThat(saved.getId()).isNotNull();
    long newAppCount =
        notificationRepository.findAll().stream()
            .filter(n -> n.getType() == NotificationType.NEW_APPLICATION)
            .count();
    assertThat(newAppCount).isZero();
  }
}
