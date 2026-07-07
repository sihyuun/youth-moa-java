package io.github.sihyuuun.youthmoa.application;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 신청 폼 DTO — prototype.html ProgramApply (line 1108~1197) 기준 (3단계 위저드).
 *
 * <ul>
 *   <li>applyReason: 지원 동기 (선택, 최대 1000자) — F0c-remainder Q2 결정에 따라 필수 해제
 *   <li>privacyAgreed: 개인정보 수집 동의 (필수)
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
}
