package io.github.sihyuuun.youthmoa.test;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
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
 * <p>목적: e2e Playwright spec 의 seed self-pollution (fix-e2e-seed-pollution) 해소. 반복 실행 시 사전에 신청 row 를
 * 정리해 fresh state 를 보장한다.
 *
 * <p>격리: {@code @Profile("e2e")} — bootrun-e2e.cmd 로 기동한 e2e 프로파일에서만 Bean 등록. local/prod
 * 프로파일에서는 컴포넌트 스캔 대상에서 제외되어 endpoint 존재 자체가 성립하지 않는다.
 * {@link io.github.sihyuuun.youthmoa.common.config.SecurityConfig} 는 e2e 프로파일에서만 {@code /__test__/**} 를
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

  /**
   * 특정 유저의 신청 row 를 삭제한다.
   *
   * <p>요청 바디: {@code {"userEmail": "seed30@youth-moa.test", "programId": 7}}. programId 가 null 이면 해당
   * 유저의 전체 신청 삭제.
   *
   * <p>알림 부수효과: {@link
   * io.github.sihyuuun.youthmoa.notification.ApplicationNotificationListener} 는 APPROVED / REJECTED /
   * CANCELLED 이벤트에만 반응한다. {@code apply()} 성공 시점엔 이벤트 발행이 없어 Notification row 가 만들어지지 않으므로 이 endpoint 는
   * Application row 삭제만 수행한다.
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

  /** 신청 정리 요청 바디. programId 는 optional (null 이면 해당 유저 전체). */
  public record ResetApplicationsRequest(@NotBlank String userEmail, Long programId) {}
}
