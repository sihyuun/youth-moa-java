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
import io.github.sihyuuun.youthmoa.notification.NotificationService;
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
  @Autowired NotificationService notificationService;

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
                .createdBy(centerAdminA)
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
  @DisplayName(
      "apply cancel 후 5분 내 재신청은 병합되어 row 1건 (occurrenceCount=2) — A7-rate-limit (2026-09-29)")
  void reapply_within_window_merges_into_single_row() {
    // A7-rate-limit 도입 전에는 재신청마다 별개 row 를 기대했으나, 지금은 같은 (type, dedupKey) 5분 window 안에서 병합된다.
    // window 밖 재신청 시나리오는 아래 reapply_after_window_creates_new_row 가 커버.
    ApplyRequest req = new ApplyRequest();
    req.setApplyReason("첫 신청");
    Application app = applicationService.apply(applicant.getEmail(), programA.getId(), req);
    applicationService.cancel(app.getId(), applicant.getEmail());

    ApplyRequest req2 = new ApplyRequest();
    req2.setApplyReason("재신청");
    applicationService.apply(applicant.getEmail(), programA.getId(), req2);

    List<Notification> newApp =
        notificationRepository.findAll().stream()
            .filter(n -> n.getType() == NotificationType.NEW_APPLICATION)
            .filter(n -> n.getUser().getId().equals(centerAdminA.getId()))
            .toList();
    assertThat(newApp).hasSize(1);
    assertThat(newApp.get(0).getOccurrenceCount()).isEqualTo(2);
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

  // A9-b (2026-09-22): center FK NOT NULL 승격으로 "center 미할당 프로그램" 시나리오는 스키마상 성립 불가 →
  // 기존 apply_with_unmatched_organization_creates_no_notifications 케이스 폐기. 미매칭 CENTER_ADMIN
  // 계정 시나리오 (Program.center.id 는 있으나 그 센터의 CENTER_ADMIN 이 0명) 는 기본 케이스
  // apply_creates_notification_for_matching_center_admin_only 의 부정 assertion 이 이미 커버한다.

  // ── A7-rate-limit (2026-09-29) 병합 검증 ─────────────────────────────
  // Q3 그룹기준 = type + sourceId. window=5분(기본).
  // 검증 4가지: (a) 같은 programId 재신청 → 병합 (b) 다른 programId → 독립 (c) window 밖 → 신규
  //           (d) 사용자 트랙 create → 병합 안 됨

  @Test
  @DisplayName(
      "A7-rate-limit (a) 같은 programId 5분 내 2회 신청 → row 1건, occurrenceCount=2, isRead=false 리셋")
  void reapply_within_window_merges_into_same_row() {
    ApplyRequest req1 = new ApplyRequest();
    req1.setApplyReason("첫 신청");
    Application app1 = applicationService.apply(applicant.getEmail(), programA.getId(), req1);

    // 첫 알림을 읽음 처리 후 → 병합 시 isRead=false 리셋 검증
    List<Notification> firstBatch =
        notificationRepository.findAll().stream()
            .filter(n -> n.getType() == NotificationType.NEW_APPLICATION)
            .toList();
    assertThat(firstBatch).hasSize(1);
    Long firstId = firstBatch.get(0).getId();
    notificationService.markAsRead(firstId, centerAdminA.getId());

    applicationService.cancel(app1.getId(), applicant.getEmail());
    ApplyRequest req2 = new ApplyRequest();
    req2.setApplyReason("재신청");
    applicationService.apply(applicant.getEmail(), programA.getId(), req2);

    List<Notification> newApplication =
        notificationRepository.findAll().stream()
            .filter(n -> n.getType() == NotificationType.NEW_APPLICATION)
            .filter(n -> n.getUser().getId().equals(centerAdminA.getId()))
            .toList();

    // 병합 → row 1건 유지, count=2, isRead=false 리셋
    assertThat(newApplication).hasSize(1);
    Notification merged = newApplication.get(0);
    assertThat(merged.getId()).isEqualTo(firstId);
    assertThat(merged.getOccurrenceCount()).isEqualTo(2);
    assertThat(merged.isRead()).isFalse();
    assertThat(merged.getDedupKey()).isEqualTo("NEW_APPLICATION:" + programA.getId());
  }

  @Test
  @DisplayName("A7-rate-limit (b) 다른 programId 신청 → row 2건 (독립 dedupKey)")
  void different_programs_do_not_merge() {
    // programB (centerA 소속, 같은 centerAdminA 가 수신)
    LocalDate today = LocalDate.now();
    Program programB =
        programRepository.save(
            Program.builder()
                .title("A7 프로그램 B")
                .center(centerA)
                .category("취업")
                .region("수원시")
                .content("c")
                .startDate(today.minusDays(1))
                .endDate(today.plusDays(30))
                .capacity(30)
                .createdBy(centerAdminA)
                .build());

    ApplyRequest reqA = new ApplyRequest();
    reqA.setApplyReason("A 신청");
    applicationService.apply(applicant.getEmail(), programA.getId(), reqA);

    ApplyRequest reqB = new ApplyRequest();
    reqB.setApplyReason("B 신청");
    applicationService.apply(applicant.getEmail(), programB.getId(), reqB);

    List<Notification> newApplication =
        notificationRepository.findAll().stream()
            .filter(n -> n.getType() == NotificationType.NEW_APPLICATION)
            .filter(n -> n.getUser().getId().equals(centerAdminA.getId()))
            .toList();

    // 각 program 별 dedupKey 가 다르므로 독립 row 2건
    assertThat(newApplication).hasSize(2);
    assertThat(newApplication)
        .extracting(Notification::getDedupKey)
        .containsExactlyInAnyOrder(
            "NEW_APPLICATION:" + programA.getId(), "NEW_APPLICATION:" + programB.getId());
    assertThat(newApplication).allMatch(n -> n.getOccurrenceCount() == 1);
  }

  @Test
  @DisplayName("A7-rate-limit (c) window 밖 (>5분) 재신청 → 신규 row 생성 (병합 안 됨)")
  void reapply_after_window_creates_new_row() {
    ApplyRequest req1 = new ApplyRequest();
    req1.setApplyReason("첫 신청");
    Application app1 = applicationService.apply(applicant.getEmail(), programA.getId(), req1);

    // 첫 알림의 lastOccurredAt 을 6분 전으로 강제 이동 (window 5분 밖)
    List<Notification> firstBatch =
        notificationRepository.findAll().stream()
            .filter(n -> n.getType() == NotificationType.NEW_APPLICATION)
            .toList();
    assertThat(firstBatch).hasSize(1);
    Notification first = firstBatch.get(0);
    // 시각 강제 이동: e2e 프로파일 H2 이므로 DATEADD 사용. @SpringBootTest 는 기본 트랜잭션이 없으므로
    // TransactionTemplate 로 native UPDATE 를 감싼다.
    Long firstId = first.getId();
    transactionTemplate.execute(
        status -> {
          entityManagerHolder
              .createNativeQuery(
                  "UPDATE notification SET last_occurred_at ="
                      + " DATEADD('MINUTE', -6, last_occurred_at) WHERE id = :id")
              .setParameter("id", firstId)
              .executeUpdate();
          return null;
        });

    applicationService.cancel(app1.getId(), applicant.getEmail());
    ApplyRequest req2 = new ApplyRequest();
    req2.setApplyReason("재신청");
    applicationService.apply(applicant.getEmail(), programA.getId(), req2);

    List<Notification> newApplication =
        notificationRepository.findAll().stream()
            .filter(n -> n.getType() == NotificationType.NEW_APPLICATION)
            .filter(n -> n.getUser().getId().equals(centerAdminA.getId()))
            .toList();

    // window 밖 → 새 row → 총 2건 (각 count=1)
    assertThat(newApplication).hasSize(2);
    assertThat(newApplication).allMatch(n -> n.getOccurrenceCount() == 1);
  }

  @Test
  @DisplayName("A7-rate-limit (d) 사용자 트랙 create() 는 dedupKey=null 이라 병합되지 않는다")
  void user_track_create_does_not_merge() {
    // NotificationService.create (사용자 트랙) 는 dedupKey 를 세팅하지 않으므로 병합 후보 조회에서 제외됨.
    // 같은 유저 같은 타입으로 2건 연속 발행 → 각각 독립 row.
    notificationService.create(
        applicant.getId(), NotificationType.APPLICATION_APPROVED, "승인 알림", "메시지 1", "/link");
    notificationService.create(
        applicant.getId(), NotificationType.APPLICATION_APPROVED, "승인 알림", "메시지 2", "/link");

    List<Notification> approved =
        notificationRepository.findAll().stream()
            .filter(n -> n.getType() == NotificationType.APPLICATION_APPROVED)
            .filter(n -> n.getUser().getId().equals(applicant.getId()))
            .toList();

    // Q5: 사용자 트랙 무영향 — dedupKey=null 이라 병합 안 됨. row 2건, 각 count=1.
    assertThat(approved).hasSize(2);
    assertThat(approved).allMatch(n -> n.getOccurrenceCount() == 1);
    assertThat(approved).allMatch(n -> n.getDedupKey() == null);
  }

  // native SQL 실행용 EntityManager (테스트 시각 조작).
  @jakarta.persistence.PersistenceContext
  private jakarta.persistence.EntityManager entityManagerHolder;

  // A7-rate-limit: native UPDATE 를 트랜잭션 경계 안에서 실행하기 위한 helper.
  @Autowired
  private org.springframework.transaction.support.TransactionTemplate transactionTemplate;
}
