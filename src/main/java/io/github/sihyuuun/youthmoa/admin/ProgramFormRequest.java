package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.program.ApprovalMode;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
  private LocalDate startDate;
  private LocalDate endDate;
  private String description;
  private String content;

  // 탭 2 — 신청 정보
  private LocalDate applyStartDate;
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
}
