package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.program.ApprovalMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * A3-1 admin-program-form (2026-09-10): 관리자 프로그램 등록/편집 폼 요청 DTO.
 *
 * <p>3탭 (정보 / 신청 / 약관) 모든 필드를 한 번의 POST 로 수신. Bean Validation 대신 서비스 계층에서 IllegalArgumentException
 * 으로 400 매핑 ({@link AdminExceptionHandler} 재활용) — admin-notice/term 패턴 승계 (Qn-Δ5 A).
 *
 * <p>Thymeleaf {@code th:field="*{...}"} 바인딩을 위해 {@code @Setter} 사용 (도메인 엔티티 아님).
 */
@Getter
@Setter
@NoArgsConstructor
public class ProgramFormRequest {

  // 탭 1 — 프로그램 정보
  private String title;
  private String imageUrl;
  private String category;
  private String organization;
  private String region;

  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate startDate;

  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate endDate;

  private String description;
  private String content;

  // 탭 2 — 신청 정보
  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate applyStartDate;

  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate applyEndDate;

  private String venue;
  private String contact;
  private Integer capacity;
  private ApprovalMode approvalMode;

  // 탭 3 — 약관 정보
  private String termsService;
  private String termsPrivacy;
  private String termsMarketing;

  // 활성 여부 (Qn-Δ6 A: status 수동 편집 가능)
  private boolean active = true;

  // ============== A3-2 admin-program-form-integration (2026-09-11) ==============

  /** F4 인라인 (Qn-A A): 편집 폼 안에서 자격요건 3필드 함께 저장. 별도 페이지도 병존. */
  private String eligibilityAge;

  private String eligibilityRegion;
  private String eligibilityEtc;

  /** 강좌 제공 여부 (Qn-B A). checked 아니면 courses 는 무시. */
  private boolean hasCourses;

  /** 강좌 다중 row (Qn-Δ-encoding A: repeated form fields). 서버가 제출 순서대로 sortOrder 재부여. */
  private List<CourseFormRow> courses = new ArrayList<>();

  /** 신청 질문 다중 row (F0c 인라인 · Qn-A A). id 존재 여부로 upsert · 사라진 id 는 soft delete. */
  private List<ApplyQuestionFormRow> questions = new ArrayList<>();
}
