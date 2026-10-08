package io.github.sihyuuun.youthmoa.application;

import io.github.sihyuuun.youthmoa.application.event.ApplicationApprovedEvent;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FOLLOW-waitlist-auto-approve (2026-10-08): 대기자(PENDING) 선입선출 1건 자동 승격 서비스.
 *
 * <p>Q2 A — 트리거 시점: APPROVED→CANCELLED/REJECTED 발생 직후. apply() 시점 자동 승인은 하지 않음 (approvalMode=AUTO 가
 * 담당).
 *
 * <p>Q3 A — processedBy: 기존 sysadmin 재활용 + {@code adminNote = "SYSTEM 자동 승인 (대기자 승격)"}.
 *
 * <p>Q4 A — OFF→ON 전환 시 기존 PENDING 일괄 승급 금지. 이 서비스는 이벤트 단위 호출만 처리.
 *
 * <p>별도 Service 로 분리한 이유: {@link io.github.sihyuuun.youthmoa.admin.AdminApplicationService} 와
 * {@link ApplicationService} 가 공유하면서, 두 서비스가 서로 참조하면 순환 의존이 생긴다. 승격 로직만 떼내 양쪽에서 depend-on.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WaitlistPromotionService {

  /** Q3 A: sysadmin 재활용. DataInitializer 시드 이메일. */
  public static final String SYSTEM_ADMIN_EMAIL = "sysadmin@youth-moa.test";

  /** Q3 A: adminNote 에 기록될 승격 마커 문자열. 리포트/감사에서 조회 가능. */
  public static final String PROMOTION_NOTE = "SYSTEM 자동 승인 (대기자 승격)";

  private final ApplicationRepository applicationRepository;
  private final ProgramRepository programRepository;
  private final UserRepository userRepository;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * 승격 트리거. 호출 측은 상태 전이 + 이벤트 발행을 완료한 뒤 호출한다.
   *
   * <ul>
   *   <li>program.autoApproveWhenFull == false → no-op
   *   <li>program.capacity == null → no-op (정원 없는 프로그램은 "꽉 참" 개념 없음)
   *   <li>APPROVED 수가 이미 capacity 이상 → no-op (방어적 — 정상 흐름에선 공석 생긴 직후라 성립 안 함)
   *   <li>PENDING 없음 → no-op
   *   <li>조건 충족 시 가장 오래된 PENDING 1건을 sysadmin 명의로 APPROVED 로 전이 + adminNote 기록 + {@link
   *       ApplicationApprovedEvent} 발행
   * </ul>
   *
   * <p>트랜잭션 전파: {@code REQUIRED} (기본). 호출 측 트랜잭션에 참여하여 상태 전이와 승격이 한 원자 단위로 커밋/롤백된다.
   */
  @Transactional
  public void promoteIfEligible(Long programId) {
    Program program =
        programRepository.findById(programId).orElse(null);
    if (program == null) return;
    if (!program.isAutoApproveWhenFull()) return;
    if (program.getCapacity() == null) return;

    long approvedCount =
        applicationRepository.countByProgramAndStatusIn(program, List.of(ApplicationStatus.APPROVED));
    if (approvedCount >= program.getCapacity()) {
      // 방어적 — 정상 흐름에선 CANCELLED/REJECTED 로 공석 하나 생긴 직후이므로 approvedCount < capacity 성립.
      // approvedCount >= capacity 라면 승격을 추가하면 정원 초과가 되므로 skip.
      return;
    }

    Optional<Application> candidate =
        applicationRepository.findFirstByProgramIdAndStatusOrderByAppliedAtAsc(
            programId, ApplicationStatus.PENDING);
    if (candidate.isEmpty()) return;

    User sysadmin =
        userRepository
            .findByEmail(SYSTEM_ADMIN_EMAIL)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "sysadmin 시드가 존재해야 합니다 (" + SYSTEM_ADMIN_EMAIL + ")"));

    Application promoted = candidate.get();
    promoted.approve(sysadmin);
    promoted.updateAdminNote(PROMOTION_NOTE);

    log.info(
        "[waitlist-promotion] programId={} promoted applicationId={} userId={}",
        programId,
        promoted.getId(),
        promoted.getUser().getId());

    // 사용자 알림 자동 (ApplicationNotificationListener 가 수신). Q3 A 범위에선 전용 알림 메시지는 이월,
    // "승인 완료" 알림을 공유한다.
    eventPublisher.publishEvent(
        new ApplicationApprovedEvent(
            promoted.getId(),
            promoted.getUser().getId(),
            program.getId(),
            program.getTitle()));
  }
}
