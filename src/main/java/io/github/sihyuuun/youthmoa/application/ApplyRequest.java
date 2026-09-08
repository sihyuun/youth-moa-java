package io.github.sihyuuun.youthmoa.application;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * 신청 폼 DTO — prototype.html ProgramApply (line 1108~1197) 기준 (3단계 위저드).
 *
 * <ul>
 *   <li>applyReason: 지원 동기 (선택, 최대 1000자) — F0c-remainder Q2 결정에 따라 필수 해제
 *   <li>privacyAgreed: 개인정보 수집 동의 (필수)
 *   <li>F0c-dynamic-fields (2026-09-08 · Qn-6 A · Qn-Δ A · Qn-11 B): 동적 응답 Map. TEXT/DROPDOWN 은
 *       {@link #dynamicAnswers} (questionId → String). ATTACHMENT 는 MultipartFile 이라 controller 가
 *       별도로 {@code MultipartHttpServletRequest} 에서 추출.
 * </ul>
 */
@Getter
@Setter
public class ApplyRequest {

  /**
   * 지원 동기. prototype 상 placeholder "(선택)". 서버 상한만 유지 (1000자). @NotBlank·@Size(min) 은 F0c-remainder
   * 로 제거됨.
   */
  @Size(max = 1000, message = "지원 동기는 1000자 이하로 작성해야 합니다.")
  private String applyReason;

  /**
   * prototype 의 'agreed' — 개인정보 수집 동의 필수.
   *
   * <p>@AssertTrue 를 field 에 부착하면 error field name 이 field 이름과 일치 (`privacyAgreed`) → Thymeleaf
   * `#fields.hasErrors('privacyAgreed')` 로 접근 가능. 별도 `isPrivacyAccepted()` 게터를 두면 property 이름이
   * `privacyAccepted` 로 파생되어 템플릿 참조가 어긋남.
   */
  @AssertTrue(message = "개인정보 수집 동의가 필요합니다.")
  private boolean privacyAgreed;

  /**
   * F0c-dynamic-fields: 동적 필드 응답 (TEXT/DROPDOWN). {@code name="dynamicAnswers[questionId]"} 형식으로
   * multipart form 필드에서 바인딩. ATTACHMENT 응답은 별도 MultipartFile 이라 controller 가
   * MultipartHttpServletRequest.getFile 로 추출.
   */
  private Map<Long, String> dynamicAnswers = new HashMap<>();
}
