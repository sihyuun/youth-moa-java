package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.application.ApplyAnswer;
import io.github.sihyuuun.youthmoa.application.ApplyAnswerRepository;
import io.github.sihyuuun.youthmoa.application.event.ApplicationApprovedEvent;
import io.github.sihyuuun.youthmoa.application.event.ApplicationCancelledEvent;
import io.github.sihyuuun.youthmoa.application.event.ApplicationRejectedEvent;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A4 admin-program-detail (2026-09-15 · Qn 24건 모두 A): 관리자 신청 관리 서비스.
 *
 * <ul>
 *   <li>Qn-A A — SYSTEM_ADMIN + CENTER_ADMIN (organization 문자열 매칭 · AdminScope 재활용)
 *   <li>Qn-C A — 관리자 강제 CANCELLED 지원 · {@link Application#forceCancelByAdmin} 재활용 · {@link
 *       ApplicationCancelledEvent} 발행
 *   <li>Qn-1 A — {@code adminNote} 단일 컬럼 (V15)
 *   <li>Qn-2 A — 거절 사유 필수
 *   <li>Qn-3 A — 기존 이벤트 3종 재활용 · 신규 이벤트 없음
 *   <li>Qn-6 A — 페이지당 10건
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminApplicationService {

  /** Qn-6 A: 페이지당 10건. */
  public static final int PAGE_SIZE = 10;

  private final ApplicationRepository applicationRepository;
  private final ProgramRepository programRepository;
  private final UserRepository userRepository;
  private final ApplyAnswerRepository applyAnswerRepository;
  private final AdminScope adminScope;
  private final ApplicationEventPublisher eventPublisher;

  /** 프로그램이 관리자 스코프 내에 있는지 검증. 미매칭이면 IllegalAccessError. */
  public Program assertProgramInScope(Long programId) {
    Program p =
        programRepository
            .findById(programId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로그램이에요: " + programId));
    // A9-a: Center FK 기반 스코프 검증
    Long scopeId = adminScope.effectiveCenterId();
    if (scopeId != null) {
      Long pCenterId = p.getCenter() != null ? p.getCenter().getId() : null;
      if (!scopeId.equals(pCenterId)) {
        throw new IllegalAccessError("자신의 센터 프로그램만 조회할 수 있어요.");
      }
    }
    return p;
  }

  /**
   * 프로그램 신청 목록 조회 (필터·페이지네이션).
   *
   * @param programId 프로그램 ID (스코프 검증 완료 전제)
   * @param status status 필터 (null/blank/ALL 은 미적용)
   * @param q 신청자 이름 or 이메일 LIKE (nullable)
   * @param page 0-based 페이지 인덱스
   */
  public Page<Application> list(Long programId, String status, String q, int page) {
    Pageable pageable =
        PageRequest.of(Math.max(0, page), PAGE_SIZE, Sort.by(Sort.Order.desc("appliedAt")));
    // fetch join user — OSIV=false 환경에서 템플릿 렌더 시 LazyInitializationException 방지 (spec §7).
    // count 쿼리에는 fetch 를 걸면 Hibernate 가 에러를 내므로 resultType 체크 필수.
    Specification<Application> spec =
        (root, query, cb) -> {
          if (query != null && Long.class != query.getResultType()) {
            root.fetch("user");
          }
          return cb.equal(root.get("program").get("id"), programId);
        };
    if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
      try {
        ApplicationStatus s = ApplicationStatus.valueOf(status.toUpperCase());
        spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), s));
      } catch (IllegalArgumentException ignore) {
        // 잘못된 값이면 필터 미적용
      }
    }
    if (q != null && !q.isBlank()) {
      String pattern = "%" + q.trim().toLowerCase() + "%";
      spec =
          spec.and(
              (root, query, cb) -> {
                Predicate nameLike = cb.like(cb.lower(root.get("user").get("name")), pattern);
                Predicate emailLike = cb.like(cb.lower(root.get("user").get("email")), pattern);
                return cb.or(nameLike, emailLike);
              });
    }
    return applicationRepository.findAll(spec, pageable);
  }

  /** 상태별 요약 카운트 맵 (신청 배지용). key = ApplicationStatus, value = count. */
  public Map<ApplicationStatus, Long> summaryCounts(Long programId) {
    Map<ApplicationStatus, Long> map = new HashMap<>();
    for (ApplicationStatus s : ApplicationStatus.values()) {
      map.put(s, applicationRepository.countByProgramIdAndStatus(programId, s));
    }
    return map;
  }

  /** 전체 카운트. */
  public long totalCount(Long programId) {
    return applicationRepository.countByProgramId(programId);
  }

  /** userIds 에 대한 승인 참여횟수 일괄 조회 (N+1 방지). */
  public Map<Long, Long> visitsByUserIds(List<Long> userIds) {
    Map<Long, Long> map = new HashMap<>();
    if (userIds == null || userIds.isEmpty()) return map;
    List<Object[]> rows =
        applicationRepository.countByUserIdInAndStatus(userIds, ApplicationStatus.APPROVED);
    for (Object[] row : rows) {
      map.put((Long) row[0], (Long) row[1]);
    }
    return map;
  }

  /** 상세 조회. 스코프 검증 포함. */
  public Application findById(Long programId, Long applicationId) {
    assertProgramInScope(programId);
    Application app =
        applicationRepository
            .findWithProgramAndUserById(applicationId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신청이에요: " + applicationId));
    if (!app.getProgram().getId().equals(programId)) {
      throw new IllegalArgumentException("해당 프로그램의 신청이 아니에요.");
    }
    return app;
  }

  /** 상세 모달용: 신청 + 답변(F0c) 를 함께 반환. */
  public List<ApplyAnswer> findAnswers(Long applicationId) {
    return applyAnswerRepository.findByApplicationIdOrderByIdAsc(applicationId);
  }

  // ================= 상태 변경 =================

  /** 승인 (Qn-3 A: 기존 이벤트 재활용). idempotent. */
  @Transactional
  public void approve(Long programId, Long applicationId, String adminEmail) {
    Application app = findById(programId, applicationId);
    User admin = loadUser(adminEmail);
    if (app.getStatus() == ApplicationStatus.APPROVED) return;
    app.approve(admin);
    eventPublisher.publishEvent(
        new ApplicationApprovedEvent(
            app.getId(),
            app.getUser().getId(),
            app.getProgram().getId(),
            app.getProgram().getTitle()));
  }

  /** 반려 (Qn-2 A: reason 필수). idempotent. */
  @Transactional
  public void reject(Long programId, Long applicationId, String adminEmail, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("반려 사유를 입력해주세요.");
    }
    Application app = findById(programId, applicationId);
    User admin = loadUser(adminEmail);
    if (app.getStatus() == ApplicationStatus.REJECTED) return;
    app.reject(admin, reason);
    eventPublisher.publishEvent(
        new ApplicationRejectedEvent(
            app.getId(),
            app.getUser().getId(),
            app.getProgram().getId(),
            app.getProgram().getTitle(),
            reason));
  }

  /**
   * 관리자 강제 취소 (Qn-C A). 사유 필수. {@link ApplicationCancelledEvent} 발행 → 사용자 알림 자동
   * (ApplicationNotificationListener).
   */
  @Transactional
  public void forceCancel(Long programId, Long applicationId, String adminEmail, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("취소 사유를 입력해주세요.");
    }
    Application app = findById(programId, applicationId);
    User admin = loadUser(adminEmail);
    if (app.getStatus() == ApplicationStatus.CANCELLED) return;
    String prefixed = "관리자 취소: " + reason;
    app.forceCancelByAdmin(admin, prefixed);
    eventPublisher.publishEvent(
        new ApplicationCancelledEvent(
            app.getId(),
            app.getUser().getId(),
            app.getProgram().getId(),
            app.getProgram().getTitle(),
            prefixed));
  }

  /** 담당자 의견 저장 (Qn-3 A: 이벤트 발행 없음). 상태 미변경. */
  @Transactional
  public void updateNote(Long programId, Long applicationId, String note) {
    if (note != null && note.length() > 1000) {
      throw new IllegalArgumentException("담당자 의견은 1000자 이하로 입력해주세요.");
    }
    Application app = findById(programId, applicationId);
    app.updateAdminNote(note);
  }

  // ================= 내부 헬퍼 =================

  private User loadUser(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없어요: " + email));
  }

  /** ApplicationStatus label 한글 매핑. */
  public static String labelOf(ApplicationStatus s) {
    if (s == null) return "";
    return switch (s) {
      case PENDING -> "대기";
      case APPROVED -> "승인";
      case REJECTED -> "반려";
      case CANCELLED -> "취소";
    };
  }

  /** 목록 렌더용 · 상태 옵션 리스트 (드롭다운). */
  public List<ApplicationStatus> statusOptions() {
    List<ApplicationStatus> list = new ArrayList<>();
    list.add(ApplicationStatus.PENDING);
    list.add(ApplicationStatus.APPROVED);
    list.add(ApplicationStatus.REJECTED);
    list.add(ApplicationStatus.CANCELLED);
    return list;
  }
}
