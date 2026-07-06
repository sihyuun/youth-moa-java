package io.github.sihyuuun.youthmoa.application;

import io.github.sihyuuun.youthmoa.application.event.ApplicationApprovedEvent;
import io.github.sihyuuun.youthmoa.application.event.ApplicationCancelledEvent;
import io.github.sihyuuun.youthmoa.application.event.ApplicationRejectedEvent;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.program.ProgramStatus;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationService {

  private final ApplicationRepository applicationRepository;
  private final ProgramRepository programRepository;
  private final UserRepository userRepository;

  /**
   * Spring 표준 도메인 이벤트 퍼블리셔.
   *
   * <p>{@code publishEvent} 호출 시점엔 아직 트랜잭션 커밋 전이며,
   * {@code @TransactionalEventListener(AFTER_COMMIT)} 리스너는 실제 커밋 후에 실행된다. 롤백되면 리스너는 호출되지 않는다.
   */
  private final ApplicationEventPublisher eventPublisher;

  /**
   * 프로그램 신청.
   *
   * <ul>
   *   <li>이미 PENDING / APPROVED 상태 신청 있으면 → 차단
   *   <li>REJECTED 상태 신청 있으면 → 차단 (재신청 불가)
   *   <li>CANCELLED 상태 신청 있으면 → 같은 row 재활용 (PENDING 으로 복귀)
   *   <li>없으면 → 신규 row 생성
   * </ul>
   */
  @Transactional
  public Application apply(String userEmail, Long programId, ApplyRequest request) {
    User user =
        userRepository
            .findByEmail(userEmail)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userEmail));

    Program program =
        programRepository
            .findById(programId)
            .orElseThrow(() -> new IllegalArgumentException("프로그램을 찾을 수 없습니다: " + programId));

    if (!program.isActive()) {
      throw new IllegalStateException("비활성 상태의 프로그램은 신청할 수 없습니다.");
    }
    if (program.getStatus() != ProgramStatus.ACTIVE) {
      throw new IllegalStateException("현재 모집 중인 프로그램이 아닙니다.");
    }

    Optional<Application> existing = applicationRepository.findByUserAndProgram(user, program);
    if (existing.isPresent()) {
      Application app = existing.get();
      switch (app.getStatus()) {
        case PENDING, APPROVED -> throw new IllegalStateException("이미 신청한 프로그램입니다.");
        case REJECTED -> throw new IllegalStateException("이미 반려된 신청이 있어 다시 신청할 수 없습니다.");
        case CANCELLED -> {
          app.reapply(request.getApplyReason());
          return app;
        }
      }
    }

    Application application =
        Application.builder()
            .user(user)
            .program(program)
            .applyReason(request.getApplyReason())
            .build();
    return applicationRepository.save(application);
  }

  /**
   * 신청 승인 (관리자). 상태 전이 후 {@link ApplicationApprovedEvent} 발행.
   *
   * <p>이미 APPROVED 상태이면 no-op (idempotent) — 이벤트도 발행하지 않는다.
   */
  @Transactional
  public void approve(Long applicationId, String adminEmail) {
    Application application = loadWithProgramAndUser(applicationId);
    User admin = loadUser(adminEmail);

    if (application.getStatus() == ApplicationStatus.APPROVED) {
      return; // idempotent
    }
    application.approve(admin);

    eventPublisher.publishEvent(
        new ApplicationApprovedEvent(
            application.getId(),
            application.getUser().getId(),
            application.getProgram().getId(),
            application.getProgram().getTitle()));
  }

  /** 신청 반려 (관리자). 상태 전이 후 {@link ApplicationRejectedEvent} 발행. */
  @Transactional
  public void reject(Long applicationId, String adminEmail, String reason) {
    Application application = loadWithProgramAndUser(applicationId);
    User admin = loadUser(adminEmail);

    if (application.getStatus() == ApplicationStatus.REJECTED) {
      return; // idempotent
    }
    application.reject(admin, reason);

    eventPublisher.publishEvent(
        new ApplicationRejectedEvent(
            application.getId(),
            application.getUser().getId(),
            application.getProgram().getId(),
            application.getProgram().getTitle(),
            reason));
  }

  /**
   * 신청 취소 (신청자 본인).
   *
   * <p>본인이 아니면 {@link IllegalStateException}. 이미 CANCELLED 이면 no-op.
   */
  @Transactional
  public void cancel(Long applicationId, String userEmail) {
    cancel(applicationId, userEmail, null);
  }

  /** D5: 취소 사유와 함께 신청 취소. reason 은 label + optional 텍스트 조합 문자열. */
  @Transactional
  public void cancel(Long applicationId, String userEmail, String reason) {
    Application application = loadWithProgramAndUser(applicationId);
    User user = loadUser(userEmail);

    if (!application.getUser().getId().equals(user.getId())) {
      throw new IllegalStateException("본인의 신청만 취소할 수 있습니다.");
    }
    if (application.getStatus() == ApplicationStatus.CANCELLED) {
      return; // idempotent
    }
    application.cancel(reason);

    eventPublisher.publishEvent(
        new ApplicationCancelledEvent(
            application.getId(),
            application.getUser().getId(),
            application.getProgram().getId(),
            application.getProgram().getTitle(),
            reason));
  }

  private Application loadWithProgramAndUser(Long applicationId) {
    return applicationRepository
        .findWithProgramAndUserById(applicationId)
        .orElseThrow(() -> new IllegalArgumentException("신청을 찾을 수 없습니다: " + applicationId));
  }

  private User loadUser(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
  }
}
