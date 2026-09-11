package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.program.ApplyQuestion;
import io.github.sihyuuun.youthmoa.program.ApplyQuestionRepository;
import io.github.sihyuuun.youthmoa.program.ApprovalMode;
import io.github.sihyuuun.youthmoa.program.Course;
import io.github.sihyuuun.youthmoa.program.CourseRepository;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramEligibility;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.program.ProgramStatus;
import io.github.sihyuuun.youthmoa.program.QuestionType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  /** A3-2 Qn-Δ-B-max A: 강좌 상한 20개. UX·성능 보호. */
  public static final int MAX_COURSES_PER_PROGRAM = 20;

  private final ProgramRepository programRepository;
  private final ApplicationRepository applicationRepository;
  private final AdminScope adminScope;
  private final CourseRepository courseRepository;
  private final ApplyQuestionRepository applyQuestionRepository;
  private final AdminApplyQuestionService adminApplyQuestionService;

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

  // ================= A3-1 admin-program-form CRUD =================

  /**
   * A3-1 (2026-09-10 · Qn-1 A): 관리자 프로그램 신규 등록. SYSTEM_ADMIN 전용 매핑은 컨트롤러의 {@code @PreAuthorize} 로
   * 강제.
   */
  @Transactional
  public Program create(ProgramFormRequest req) {
    validate(req);
    Program program =
        Program.builder()
            .title(req.getTitle().trim())
            .organization(req.getOrganization().trim())
            .category(trimToNull(req.getCategory()))
            .region(trimToNull(req.getRegion()))
            .imageUrl(trimToNull(req.getImageUrl()))
            .description(trimToNull(req.getDescription()))
            .content(req.getContent())
            .startDate(req.getStartDate())
            .endDate(req.getEndDate())
            .applyStartDate(req.getApplyStartDate())
            .applyEndDate(req.getApplyEndDate())
            .venue(trimToNull(req.getVenue()))
            .contact(trimToNull(req.getContact()))
            .capacity(req.getCapacity())
            .approvalMode(
                req.getApprovalMode() != null ? req.getApprovalMode() : ApprovalMode.MANUAL)
            .termsService(trimToNull(req.getTermsService()))
            .termsPrivacy(trimToNull(req.getTermsPrivacy()))
            .termsMarketing(trimToNull(req.getTermsMarketing()))
            .isActive(req.isActive())
            .hasCourses(req.isHasCourses())
            .eligibility(buildEligibility(req))
            .build();
    Program saved = programRepository.save(program);
    upsertCourses(saved, req.getCourses(), req.isHasCourses());
    upsertQuestions(saved, req.getQuestions());
    return saved;
  }

  /** A3-1 (2026-09-10): 편집 저장. */
  @Transactional
  public Program update(Long id, ProgramFormRequest req) {
    validate(req);
    Program program =
        programRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로그램이에요: " + id));
    program.updateFromAdminForm(
        req.getTitle().trim(),
        req.getOrganization().trim(),
        trimToNull(req.getCategory()),
        trimToNull(req.getRegion()),
        trimToNull(req.getImageUrl()),
        trimToNull(req.getDescription()),
        req.getContent(),
        req.getStartDate(),
        req.getEndDate(),
        req.getApplyStartDate(),
        req.getApplyEndDate(),
        trimToNull(req.getVenue()),
        trimToNull(req.getContact()),
        req.getCapacity(),
        req.getApprovalMode() != null ? req.getApprovalMode() : ApprovalMode.MANUAL,
        trimToNull(req.getTermsService()),
        trimToNull(req.getTermsPrivacy()),
        trimToNull(req.getTermsMarketing()),
        req.isActive(),
        req.isHasCourses(),
        buildEligibility(req));
    // A3-2: F4/F0c 인라인 통합 (Qn-A A)
    upsertCourses(program, req.getCourses(), req.isHasCourses());
    upsertQuestions(program, req.getQuestions());
    return program;
  }

  // ================= A3-2 인라인 통합 =================

  /** F4 인라인. 3필드 모두 blank 이면 null (embedded 삭제 · F4 별도 페이지 정책 승계). */
  private ProgramEligibility buildEligibility(ProgramFormRequest req) {
    String age = trimToNull(req.getEligibilityAge());
    String region = trimToNull(req.getEligibilityRegion());
    String etc = trimToNull(req.getEligibilityEtc());
    if (age == null && region == null && etc == null) return null;
    if (age != null && age.length() > 100) {
      throw new IllegalArgumentException("연령 조건은 100자 이하로 입력해주세요.");
    }
    if (region != null && region.length() > 100) {
      throw new IllegalArgumentException("거주지 조건은 100자 이하로 입력해주세요.");
    }
    if (etc != null && etc.length() > 200) {
      throw new IllegalArgumentException("기타 조건은 200자 이하로 입력해주세요.");
    }
    return ProgramEligibility.builder().age(age).region(region).etc(etc).build();
  }

  /**
   * Course upsert. id 있는 row 는 update, 없는 row 는 insert, 목록에서 사라진 기존 id 는 soft delete (deactivate).
   * hasCourses=false 이면 신규 등록 없이 기존 활성 강좌 전량 soft delete. Qn-Δ-sortOrder A: 제출 순서대로 1..N 재부여.
   * Qn-Δ-B-max A: 상한 20개.
   */
  @Transactional
  void upsertCourses(Program program, List<CourseFormRow> rows, boolean hasCourses) {
    List<Course> existing =
        courseRepository.findByProgramIdOrderBySortOrderAscIdAsc(program.getId());
    Map<Long, Course> existingById = new HashMap<>();
    for (Course c : existing) existingById.put(c.getId(), c);

    if (!hasCourses) {
      for (Course c : existing) if (c.isActive()) c.deactivate();
      return;
    }

    List<CourseFormRow> effective = new ArrayList<>();
    for (CourseFormRow r : rows == null ? List.<CourseFormRow>of() : rows) {
      if (r == null || r.isBlank()) continue;
      effective.add(r);
    }
    if (effective.size() > MAX_COURSES_PER_PROGRAM) {
      throw new IllegalArgumentException("강좌는 " + MAX_COURSES_PER_PROGRAM + "개까지만 등록할 수 있어요.");
    }
    for (CourseFormRow r : effective) {
      if (r.getName() == null || r.getName().trim().isEmpty()) {
        throw new IllegalArgumentException("강좌명을 입력해주세요.");
      }
      if (r.getName().length() > 200) {
        throw new IllegalArgumentException("강좌명은 200자 이하로 입력해주세요.");
      }
      if (r.getSchedule() != null && r.getSchedule().length() > 200) {
        throw new IllegalArgumentException("강좌 일정은 200자 이하로 입력해주세요.");
      }
      if (r.getCapacity() != null && r.getCapacity() <= 0) {
        throw new IllegalArgumentException("강좌 정원은 1명 이상이어야 합니다.");
      }
    }

    Set<Long> keptIds = new HashSet<>();
    int order = 1;
    for (CourseFormRow r : effective) {
      String name = r.getName().trim();
      String schedule = trimToNull(r.getSchedule());
      Integer capacity = r.getCapacity();
      if (r.getId() != null && existingById.containsKey(r.getId())) {
        Course c = existingById.get(r.getId());
        c.update(name, schedule, capacity, order);
        c.activate();
        keptIds.add(c.getId());
      } else {
        Course c =
            Course.builder()
                .program(program)
                .name(name)
                .schedule(schedule)
                .capacity(capacity)
                .sortOrder(order)
                .isActive(true)
                .build();
        courseRepository.save(c);
      }
      order++;
    }
    // 사라진 기존 id → soft delete
    for (Course c : existing) {
      if (!keptIds.contains(c.getId()) && c.isActive()) {
        c.deactivate();
      }
    }
  }

  /**
   * ApplyQuestion upsert. F0c 별도 페이지 patch 방식과 병존 (Qn-A A). id 있는 row 는 update, 없는 row 는 insert,
   * 목록에서 사라진 기존 활성 id 는 soft delete.
   */
  @Transactional
  void upsertQuestions(Program program, List<ApplyQuestionFormRow> rows) {
    List<ApplyQuestion> existing =
        applyQuestionRepository.findByProgramIdOrderBySortOrderAscIdAsc(program.getId());
    Map<Long, ApplyQuestion> existingById = new HashMap<>();
    for (ApplyQuestion q : existing) existingById.put(q.getId(), q);

    List<ApplyQuestionFormRow> effective = new ArrayList<>();
    for (ApplyQuestionFormRow r : rows == null ? List.<ApplyQuestionFormRow>of() : rows) {
      if (r == null || r.isBlank()) continue;
      effective.add(r);
    }

    Set<Long> keptIds = new HashSet<>();
    int order = 1;
    for (ApplyQuestionFormRow r : effective) {
      if (r.getFieldType() == null) {
        throw new IllegalArgumentException("질문 유형을 선택해주세요.");
      }
      QuestionType type = r.getFieldType();
      String label = r.getLabel();
      String normalizedOptions = adminApplyQuestionService.normalizeOptions(type, r.getOptions());
      Integer normalizedMaxLength =
          adminApplyQuestionService.normalizeMaxLength(type, r.getMaxLength());
      if (r.getId() != null && existingById.containsKey(r.getId())) {
        ApplyQuestion q = existingById.get(r.getId());
        q.update(type, label.trim(), r.isRequired(), order, normalizedOptions, normalizedMaxLength);
        q.activate();
        keptIds.add(q.getId());
      } else {
        ApplyQuestion q =
            ApplyQuestion.builder()
                .program(program)
                .fieldType(type)
                .label(label.trim())
                .isRequired(r.isRequired())
                .sortOrder(order)
                .options(normalizedOptions)
                .maxLength(normalizedMaxLength)
                .isActive(true)
                .build();
        applyQuestionRepository.save(q);
      }
      order++;
    }
    for (ApplyQuestion q : existing) {
      if (!keptIds.contains(q.getId()) && q.isActive()) q.deactivate();
    }
  }

  /**
   * A3-1 (2026-09-10 · Qn-3 A 재컨펌 · ADMIN-00 §Q10 준수): 소프트 삭제. {@code Program.deactivate()} 로
   * {@code isActive=false} 만 갱신 → 상태가 SUSPENDED 로 파생됨 (Program.getStatus).
   *
   * <p>ADMIN-00 §Q10 원칙 인용: "프로그램·사용자 모두 소프트 삭제, 물리 삭제 미제공". FK (Application·Bookmark) 존재 여부와 무관하게
   * row 는 유지되므로 별도 검사 불필요. 사용자 사이드는 {@code ProgramSpec.isActive()} 로 원천 필터링되어 회귀 없음.
   *
   * <p>초기 구현 (2026-09-10 F1) 은 물리 삭제 + FK 400 방어였으나 정책 위반 · ym-verify FAIL. 본 커밋 (F1-fix) 에서
   * A안(소프트) 로 정정.
   */
  @Transactional
  public void delete(Long id) {
    Program program =
        programRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로그램이에요: " + id));
    program.deactivate();
  }

  /** ProgramFormRequest 필수/유효성 검사. 실패 시 IllegalArgumentException (400 매핑). */
  private void validate(ProgramFormRequest req) {
    if (req == null) {
      throw new IllegalArgumentException("요청 데이터가 비어 있어요.");
    }
    if (isBlank(req.getTitle())) {
      throw new IllegalArgumentException("프로그램 제목을 입력해주세요.");
    }
    if (req.getTitle().trim().length() > 255) {
      throw new IllegalArgumentException("프로그램 제목은 255자 이하여야 합니다.");
    }
    if (isBlank(req.getOrganization())) {
      throw new IllegalArgumentException("청년센터(운영기관) 를 입력해주세요.");
    }
    if (isBlank(req.getContent())) {
      throw new IllegalArgumentException("상세 내용을 입력해주세요.");
    }
    if (req.getApplyStartDate() == null || req.getApplyEndDate() == null) {
      throw new IllegalArgumentException("신청 기간을 입력해주세요.");
    }
    if (req.getApplyStartDate().isAfter(req.getApplyEndDate())) {
      throw new IllegalArgumentException("신청 시작일이 신청 마감일보다 이후일 수 없어요.");
    }
    if (req.getStartDate() != null
        && req.getEndDate() != null
        && req.getStartDate().isAfter(req.getEndDate())) {
      throw new IllegalArgumentException("진행 시작일이 진행 종료일보다 이후일 수 없어요.");
    }
    if (req.getCapacity() != null && req.getCapacity() <= 0) {
      throw new IllegalArgumentException("모집 인원은 1명 이상이어야 합니다.");
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  private static String trimToNull(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
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
