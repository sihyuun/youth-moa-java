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

  /** 신청 정리 요청 바디. programId 는 optional (null 이면 해당 유저 전체). */
  public record ResetApplicationsRequest(@NotBlank String userEmail, Long programId) {}
}
