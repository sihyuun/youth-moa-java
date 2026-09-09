package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.program.ProgramStatus;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A2 (2026-09-09 · Qn-1~9 모두 A): 관리자 프로그램 목록·상세 조회 서비스.
 *
 * <ul>
 *   <li>Qn-1 A — Program-Center FK 미도입, {@link AdminScope} organization 문자열 매칭
 *   <li>Qn-3 A — 필터 5종 (전체/OPEN/UPCOMING/ENDED/SUSPENDED) + 검색 (title + organization)
 *   <li>Qn-4 A — 페이지당 10건 (AdminNoticeController 와 일관 — 20건 대신 10건: 프로그램은 카드 정보량이 더 커서 시각적 부담)
 *   <li>Qn-5 A — 최신순 createdAt DESC
 *   <li>Qn-6 A — LIKE %q% title / organization OR (lower 매칭)
 * </ul>
 *
 * <p>RBAC (Qn-1 A): SYSTEM_ADMIN 전체, CENTER_ADMIN 은 organization = 자기 센터명 매칭. 미매칭 프로그램 상세 접근은
 * IllegalAccessError 로 신호하여 Controller 에서 AccessDeniedException 으로 승격.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProgramService {

  /** Qn-4 A. 프로그램은 카드 정보량이 커 10건이 시각적으로 적절. */
  public static final int ADMIN_PAGE_SIZE = 10;

  private final ProgramRepository programRepository;
  private final ApplicationRepository applicationRepository;
  private final AdminScope adminScope;

  /**
   * 목록 조회.
   *
   * @param q 검색어 (title/organization LIKE, null/blank 허용)
   * @param status 상태 필터 ("OPEN"|"UPCOMING"|"ENDED"|"SUSPENDED"|null|"") - null/blank/ALL 은 미적용
   * @param page 0-based 페이지 인덱스
   */
  public Page<Program> list(String q, String status, int page) {
    Pageable pageable =
        PageRequest.of(Math.max(0, page), ADMIN_PAGE_SIZE, Sort.by(Sort.Order.desc("createdAt")));
    Specification<Program> spec = Specification.allOf();
    spec = spec.and(scopeSpec());
    spec = spec.and(keywordSpec(q));
    spec = spec.and(statusSpec(status));
    return programRepository.findAll(spec, pageable);
  }

  /** 상세 조회. RBAC 필터 미매칭이면 IllegalAccessError (컨트롤러가 403 승격). */
  public Program find(Long id) {
    Program p =
        programRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로그램이에요: " + id));
    String scope = adminScope.effectiveCenterName();
    if (scope != null && !scope.equals(p.getOrganization())) {
      throw new IllegalAccessError("자신의 센터 프로그램만 조회할 수 있어요.");
    }
    return p;
  }

  /** programId 리스트에 대한 신청수 (PENDING + APPROVED) 배치 조회. 목록 렌더 N+1 방지. */
  public Map<Long, Long> countAppliedByProgramIds(List<Long> programIds) {
    Map<Long, Long> counts = new HashMap<>();
    if (programIds == null || programIds.isEmpty()) return counts;
    List<Object[]> rows =
        applicationRepository.countByProgramIdsAndStatuses(
            programIds, List.of(ApplicationStatus.PENDING, ApplicationStatus.APPROVED));
    for (Object[] row : rows) {
      counts.put((Long) row[0], (Long) row[1]);
    }
    return counts;
  }

  /** 단일 프로그램 신청수 (상세 페이지). */
  public long countApplied(Program program) {
    return applicationRepository.countByProgramAndStatusIn(
        program, List.of(ApplicationStatus.PENDING, ApplicationStatus.APPROVED));
  }

  // ================= Specifications =================

  private Specification<Program> scopeSpec() {
    String scope = adminScope.effectiveCenterName();
    if (scope == null) return (root, query, cb) -> cb.conjunction();
    return (root, query, cb) -> cb.equal(root.get("organization"), scope);
  }

  private Specification<Program> keywordSpec(String q) {
    if (q == null || q.isBlank()) return (root, query, cb) -> cb.conjunction();
    String pattern = "%" + q.toLowerCase() + "%";
    return (root, query, cb) ->
        cb.or(
            cb.like(cb.lower(root.get("title")), pattern),
            cb.like(cb.lower(root.get("organization")), pattern));
  }

  /**
   * 상태 필터. Program.getStatus() 는 런타임 파생이라 DB 필터로 표현하려면 각 상태의 조건을 직접 옮겨야 한다.
   *
   * <ul>
   *   <li>SUSPENDED = isActive=false
   *   <li>ENDED = isActive=true AND endDate &lt; today
   *   <li>UPCOMING = isActive=true AND startDate &gt; today
   *   <li>OPEN = isActive=true AND (startDate NULL OR &lt;= today) AND (endDate NULL OR &gt;=
   *       today)
   * </ul>
   */
  private Specification<Program> statusSpec(String status) {
    if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status))
      return (root, query, cb) -> cb.conjunction();
    ProgramStatus s;
    try {
      s = ProgramStatus.valueOf(status.toUpperCase());
    } catch (IllegalArgumentException e) {
      return (root, query, cb) -> cb.conjunction();
    }
    LocalDate today = LocalDate.now();
    return switch (s) {
      case SUSPENDED -> (root, query, cb) -> cb.isFalse(root.get("isActive"));
      case ENDED ->
          (root, query, cb) ->
              cb.and(cb.isTrue(root.get("isActive")), cb.lessThan(root.get("endDate"), today));
      case UPCOMING ->
          (root, query, cb) ->
              cb.and(cb.isTrue(root.get("isActive")), cb.greaterThan(root.get("startDate"), today));
      case OPEN ->
          (root, query, cb) ->
              cb.and(
                  cb.isTrue(root.get("isActive")),
                  cb.or(
                      cb.isNull(root.get("startDate")),
                      cb.lessThanOrEqualTo(root.get("startDate"), today)),
                  cb.or(
                      cb.isNull(root.get("endDate")),
                      cb.greaterThanOrEqualTo(root.get("endDate"), today)));
    };
  }
}
