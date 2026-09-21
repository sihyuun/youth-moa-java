package io.github.sihyuuun.youthmoa.test;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.common.DataInitializer;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * E2E 테스트 전용 fixture endpoint.
 *
 * <p>목적: e2e Playwright spec 의 seed self-pollution (fix-e2e-seed-pollution) 해소. 반복 실행 시 사전에 신청 row
 * 를 정리해 fresh state 를 보장한다.
 *
 * <p>격리: {@code @Profile("e2e")} — bootrun-e2e.cmd 로 기동한 e2e 프로파일에서만 Bean 등록. local/prod 프로파일에서는
 * 컴포넌트 스캔 대상에서 제외되어 endpoint 존재 자체가 성립하지 않는다. {@link
 * io.github.sihyuuun.youthmoa.common.config.SecurityConfig} 는 e2e 프로파일에서만 {@code /__test__/**} 를
 * permitAll 로 매칭한다.
 *
 * <p>회귀 방지: {@code TestFixtureProfileGuardTest} 가 기본 프로파일에서 이 Bean 미등록을 assert.
 */
@Slf4j
@RestController
@RequestMapping("/__test__")
@Profile("e2e")
@RequiredArgsConstructor
public class TestFixtureController {

  private final ApplicationRepository applicationRepository;
  private final UserRepository userRepository;

  @PersistenceContext private EntityManager entityManager;

  /**
   * 특정 유저의 신청 row 를 삭제한다.
   *
   * <p>요청 바디: {@code {"userEmail": "seed30@youth-moa.test", "programId": 7}}. programId 가 null 이면
   * 해당 유저의 전체 신청 삭제.
   *
   * <p>알림 부수효과: {@link io.github.sihyuuun.youthmoa.notification.ApplicationNotificationListener} 는
   * APPROVED / REJECTED / CANCELLED 이벤트에만 반응한다. {@code apply()} 성공 시점엔 이벤트 발행이 없어 Notification row
   * 가 만들어지지 않으므로 이 endpoint 는 Application row 삭제만 수행한다.
   *
   * @return 204 No Content (idempotent — 대상 없어도 성공)
   */
  @PostMapping("/reset-applications")
  @Transactional
  public ResponseEntity<Void> resetApplications(@RequestBody ResetApplicationsRequest request) {
    User user =
        userRepository
            .findByEmail(request.userEmail())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "test fixture: user not found email=" + request.userEmail()));

    List<Application> targets;
    if (request.programId() == null) {
      targets = applicationRepository.findAllByUserOrderByAppliedAtDesc(user);
    } else {
      targets =
          applicationRepository.findAllByUserOrderByAppliedAtDesc(user).stream()
              .filter(a -> a.getProgram().getId().equals(request.programId()))
              .toList();
    }
    if (!targets.isEmpty()) {
      // F0c-dynamic-fields (Qn-7, 2026-09-08): apply_answer.application_id FK 가 걸려 있으므로
      // application 을 삭제하기 전에 하위 답변을 먼저 삭제해야 함 (동적 필드 시드 도입으로 실측 회귀).
      List<Long> targetIds = targets.stream().map(Application::getId).toList();
      entityManager
          .createNativeQuery("DELETE FROM apply_answer WHERE application_id IN (:ids)")
          .setParameter("ids", targetIds)
          .executeUpdate();
      applicationRepository.deleteAllInBatch(targets);
    }
    log.info(
        "[test-fixture] reset-applications userEmail={} programId={} deleted={}",
        request.userEmail(),
        request.programId(),
        targets.size());
    return ResponseEntity.noContent().build();
  }

  /**
   * A-admin-notice-attachment E2E seed-pollution 해소.
   *
   * <p>배경: admin-notice-form / admin-notice-upload / admin-notice-rbac spec 이 {@code POST
   * /admin/notices} 로 임시 공지를 생성하고 정리 없이 종료 → notices.spec.ts:78 페이지네이션 테스트 (page 2 = 2건 기대) 가 오염된
   * 상태로 실행되어 6건이 나오는 회귀 발생.
   *
   * <p>정책: {@code id > SEED_NOTICE_COUNT} 인 공지만 삭제. 시드 12건은 auto-increment 로 id 1~12 를 확보하므로 id 기준
   * 필터가 안전하다. 이전 정책 (createdBy != sysadmin) 은 form/upload spec 이 sysadmin 세션으로 생성한 oo 공지를 잡지 못해
   * notices.spec.ts:78 페이지네이션 회귀를 방치했음.
   *
   * <p>FK: {@code notice_attachment.notice_id} 는 ON DELETE CASCADE 가 걸려 있지 않으므로 (V1 baseline · V4)
   * attachment 를 먼저 삭제한 뒤 notice 를 삭제한다.
   *
   * @return 204 No Content (idempotent — 대상 없어도 성공)
   */
  @PostMapping("/reset-notices")
  @Transactional
  public ResponseEntity<Void> resetNotices() {
    long seedCount = DataInitializer.SEED_NOTICE_COUNT;
    int deletedAttachments =
        entityManager
            .createNativeQuery(
                "DELETE FROM notice_attachment WHERE notice_id IN (SELECT id FROM notice WHERE id > :seedCount)")
            .setParameter("seedCount", seedCount)
            .executeUpdate();
    int deletedNotices =
        entityManager
            .createNativeQuery("DELETE FROM notice WHERE id > :seedCount")
            .setParameter("seedCount", seedCount)
            .executeUpdate();
    log.info(
        "[test-fixture] reset-notices seedCount={} deletedNotices={} deletedAttachments={}",
        seedCount,
        deletedNotices,
        deletedAttachments);
    return ResponseEntity.noContent().build();
  }

  /**
   * A-admin-terms-crud (Qn-8 A, 2026-09-04): admin-term-form E2E 가 신규 약관을 생성 후 정리 없이 종료하면 다음 spec
   * (특히 signup 회귀 · admin-term-list 개수 기대) 가 오염된다.
   *
   * <p>정책: {@code id > SEED_TERM_COUNT} 인 term 만 삭제 + 관련 user_agreement 도 함께 삭제. 시드 2건은
   * auto-increment 로 id 1~2 확보. FK 순서 준수 (user_agreements 먼저 → terms).
   *
   * @return 204 No Content (idempotent — 대상 없어도 성공)
   */
  @PostMapping("/reset-terms")
  @Transactional
  public ResponseEntity<Void> resetTerms() {
    long seedCount = DataInitializer.SEED_TERM_COUNT;
    int deletedAgreements =
        entityManager
            .createNativeQuery(
                "DELETE FROM user_agreements WHERE term_id IN (SELECT id FROM terms WHERE id > :seedCount)")
            .setParameter("seedCount", seedCount)
            .executeUpdate();
    int deletedTerms =
        entityManager
            .createNativeQuery("DELETE FROM terms WHERE id > :seedCount")
            .setParameter("seedCount", seedCount)
            .executeUpdate();
    log.info(
        "[test-fixture] reset-terms seedCount={} deletedTerms={} deletedAgreements={}",
        seedCount,
        deletedTerms,
        deletedAgreements);
    return ResponseEntity.noContent().build();
  }

  /**
   * F0c-dynamic-fields (Qn-7 A, 2026-09-08): admin-dynamic-field E2E 가 신규 필드를 생성 후 정리하지 않으면 다음 spec
   * 에서 apply flow 렌더가 오염된다 (특히 시드 프로그램 #7 에 dynamic field 개수 검증).
   *
   * <p>정책: {@code id > SEED_APPLY_QUESTION_COUNT} 인 apply_question row + 관련 apply_answer 삭제. 시드 3건은
   * auto-increment 로 id 1~3 확보. FK 순서 준수 (apply_answer 먼저 → apply_question).
   *
   * @return 204 No Content (idempotent — 대상 없어도 성공)
   */
  @PostMapping("/reset-apply-questions")
  @Transactional
  public ResponseEntity<Void> resetApplyQuestions() {
    long seedCount = DataInitializer.SEED_APPLY_QUESTION_COUNT;
    int deletedAnswers =
        entityManager
            .createNativeQuery(
                "DELETE FROM apply_answer WHERE question_id IN (SELECT id FROM apply_question WHERE id > :seedCount)")
            .setParameter("seedCount", seedCount)
            .executeUpdate();
    int deletedQuestions =
        entityManager
            .createNativeQuery("DELETE FROM apply_question WHERE id > :seedCount")
            .setParameter("seedCount", seedCount)
            .executeUpdate();
    log.info(
        "[test-fixture] reset-apply-questions seedCount={} deletedQuestions={} deletedAnswers={}",
        seedCount,
        deletedQuestions,
        deletedAnswers);
    return ResponseEntity.noContent().build();
  }

  /**
   * A3-1 admin-program-form (Qn-6 A, 2026-09-10): admin-program-form-create/edit/delete E2E 가 신규
   * 프로그램을 생성 후 정리하지 않으면 다음 spec (특히 admin-programs-list 개수 검증 · seed 기반 apply flow) 이 오염된다.
   *
   * <p>정책: {@code id > SEED_PROGRAM_COUNT} 인 program row 만 삭제. FK: {@code application.program_id},
   * {@code apply_question.program_id}, {@code apply_answer(via application)} — 순서 준수 (application
   * 먼저, 그 다음 apply_question, 마지막 program).
   *
   * @return 204 No Content (idempotent — 대상 없어도 성공)
   */
  @PostMapping("/reset-programs")
  @Transactional
  public ResponseEntity<Void> resetPrograms() {
    long seedCount = DataInitializer.SEED_PROGRAM_COUNT;
    // apply_answer -> application 순서. apply_answer 는 application_id FK. application 은 program_id
    // FK.
    int deletedAnswers =
        entityManager
            .createNativeQuery(
                "DELETE FROM apply_answer WHERE application_id IN "
                    + "(SELECT id FROM application WHERE program_id IN "
                    + "(SELECT id FROM program WHERE id > :seedCount))")
            .setParameter("seedCount", seedCount)
            .executeUpdate();
    int deletedApplications =
        entityManager
            .createNativeQuery(
                "DELETE FROM application WHERE program_id IN "
                    + "(SELECT id FROM program WHERE id > :seedCount)")
            .setParameter("seedCount", seedCount)
            .executeUpdate();
    // A4 admin-program-detail (Qn-8 A): 시드 프로그램의 신청에 대해서도 admin_note 초기화 (E2E 각 spec 독립성).
    entityManager
        .createNativeQuery(
            "UPDATE application SET admin_note = NULL WHERE program_id <= :seedCount")
        .setParameter("seedCount", seedCount)
        .executeUpdate();
    int deletedQuestions =
        entityManager
            .createNativeQuery(
                "DELETE FROM apply_question WHERE program_id IN "
                    + "(SELECT id FROM program WHERE id > :seedCount)")
            .setParameter("seedCount", seedCount)
            .executeUpdate();
    int deletedBookmarks =
        entityManager
            .createNativeQuery("DELETE FROM bookmark WHERE program_id > :seedCount")
            .setParameter("seedCount", seedCount)
            .executeUpdate();
    // A3-2 (2026-09-11 · Δ-reset A): course · program_attachment cascade
    int deletedCourses =
        entityManager
            .createNativeQuery("DELETE FROM course WHERE program_id > :seedCount")
            .setParameter("seedCount", seedCount)
            .executeUpdate();
    int deletedAttachments =
        entityManager
            .createNativeQuery("DELETE FROM program_attachment WHERE program_id > :seedCount")
            .setParameter("seedCount", seedCount)
            .executeUpdate();
    int deletedPrograms =
        entityManager
            .createNativeQuery("DELETE FROM program WHERE id > :seedCount")
            .setParameter("seedCount", seedCount)
            .executeUpdate();
    log.info(
        "[test-fixture] reset-programs seedCount={} deletedPrograms={} deletedApplications={}"
            + " deletedAnswers={} deletedQuestions={} deletedBookmarks={} deletedCourses={}"
            + " deletedAttachments={}",
        seedCount,
        deletedPrograms,
        deletedApplications,
        deletedAnswers,
        deletedQuestions,
        deletedBookmarks,
        deletedCourses,
        deletedAttachments);
    return ResponseEntity.noContent().build();
  }

  /** 신청 정리 요청 바디. programId 는 optional (null 이면 해당 유저 전체). */
  public record ResetApplicationsRequest(@NotBlank String userEmail, Long programId) {}

  /**
   * A8 admin-bulk-csv (2026-09-17 · Qn-5 A): bulk 시나리오 격리 endpoint.
   *
   * <p>기존 reset-users + reset-programs 를 순차 호출한 것과 동일한 효과. bulk spec 이 여러 도메인을 한 번에 초기화할 때 매번 두
   * endpoint 를 부르는 번거로움을 줄인다. idempotent.
   *
   * @return 204 No Content
   */
  @PostMapping("/reset-bulk-fixtures")
  @Transactional
  public ResponseEntity<Void> resetBulkFixtures() {
    // Users 활성화 · 감사 컬럼 초기화 (reset-users 정합)
    int users =
        entityManager
            .createNativeQuery(
                "UPDATE users SET is_active = TRUE, deactivated_at = NULL,"
                    + " deactivated_by = NULL, deactivation_reason = NULL, admin_note = NULL")
            .executeUpdate();
    // Programs 활성화 (bulk deactivate 시나리오 반복 지원)
    int programs =
        entityManager
            .createNativeQuery("UPDATE program SET is_active = TRUE WHERE id <= :seedCount")
            .setParameter("seedCount", DataInitializer.SEED_PROGRAM_COUNT)
            .executeUpdate();
    // Applications 상태를 PENDING 으로 원복 (bulk approve 반복 지원 · APPROVED → PENDING)
    // 시드 신청 데이터만 대상.
    int apps =
        entityManager
            .createNativeQuery(
                "UPDATE application SET status = 'PENDING', processed_by = NULL,"
                    + " processed_at = NULL, reject_reason = NULL"
                    + " WHERE status = 'APPROVED' AND program_id <= :seedCount")
            .setParameter("seedCount", DataInitializer.SEED_PROGRAM_COUNT)
            .executeUpdate();
    log.info(
        "[test-fixture] reset-bulk-fixtures users={} programs={} apps={}", users, programs, apps);
    return ResponseEntity.noContent().build();
  }

  /**
   * A5 admin-users (2026-09-15 · Qn-6 A): admin-users E2E 격리용 reset endpoint.
   *
   * <p>정책:
   *
   * <ul>
   *   <li>시드 관리자 (sysadmin/center1/center2) 는 유지
   *   <li>기존 시드 사용자의 is_active/deactivated_* 컬럼 초기화 (활성=true · 감사 컬럼 NULL · admin_note NULL)
   *   <li>role 은 시드 초기값 유지 (SYSTEM_ADMIN → sysadmin, CENTER_ADMIN → center1/2, USER → seed*)
   * </ul>
   *
   * <p>주의: {@code deactivated_by} 는 self-referential FK 이므로 deactivated_by 를 먼저 NULL 로 만든 후 다른 user
   * 삭제 등도 가능. 여기서는 삭제하지 않고 컬럼만 초기화.
   *
   * @return 204 No Content (idempotent)
   */
  @PostMapping("/reset-users")
  @Transactional
  public ResponseEntity<Void> resetUsers() {
    int reset =
        entityManager
            .createNativeQuery(
                "UPDATE users SET is_active = TRUE, deactivated_at = NULL,"
                    + " deactivated_by = NULL, deactivation_reason = NULL, admin_note = NULL")
            .executeUpdate();
    log.info("[test-fixture] reset-users updatedRows={}", reset);
    return ResponseEntity.noContent().build();
  }

  /** 특정 프로그램 신청 상태 원복 요청 바디. */
  public record ResetApplicationStatusRequest(Long programId) {}

  /**
   * A8 admin-bulk-csv (2026-09-21 · A8-e2e-suite): 신청 일괄 승인 시나리오 정밀 격리 endpoint.
   *
   * <p>배경: {@link #resetBulkFixtures()} 는 {@code program_id <= SEED_PROGRAM_COUNT} 전체의 APPROVED 를
   * PENDING 으로 되돌린다. 그러나 시드 규약상 programs[0](id=1) 는 seed1~28 이 APPROVED 상태여야 하며 (apply / mypage
   * spec 이 이 불변식에 의존), reset-bulk-fixtures 를 bulk-approve 후 호출하면 programs[0] 의 APPROVED 시드까지
   * PENDING 으로 오염시켜 후속 spec 을 깨뜨린다.
   *
   * <p>이 endpoint 는 <b>지정한 programId 한 건</b>의 APPROVED 신청만 PENDING 으로 원복한다. bulk-approve 테스트가 시드
   * 규약상 전부 PENDING 인 programs[2](id=3) 를 대상으로 승인한 뒤, 그 프로그램만 정밀 복원하는 용도. 다른 프로그램의 시드 상태는 건드리지 않는다.
   *
   * @return 204 No Content (idempotent)
   */
  @PostMapping("/reset-application-status")
  @Transactional
  public ResponseEntity<Void> resetApplicationStatus(
      @RequestBody ResetApplicationStatusRequest req) {
    int reset =
        entityManager
            .createNativeQuery(
                "UPDATE application SET status = 'PENDING', processed_by = NULL,"
                    + " processed_at = NULL, reject_reason = NULL"
                    + " WHERE program_id = :pid AND status = 'APPROVED'")
            .setParameter("pid", req.programId())
            .executeUpdate();
    log.info(
        "[test-fixture] reset-application-status programId={} updatedRows={}",
        req.programId(),
        reset);
    return ResponseEntity.noContent().build();
  }
}
