package io.github.sihyuuun.youthmoa.application;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 신청 폼 DTO — prototype.tsx ProgramApply (line 962~1017) 기준.
 *
 * <ul>
 *   <li>applyReason: 지원 동기 (10~1000자 필수)
 *   <li>privacyAgreed: 개인정보 수집 동의 (필수)
 * </ul>
 */
@Getter
@Setter
public class ApplyRequest {

  @NotBlank(message = "지원 동기를 입력해주세요.")
  @Size(min = 10, max = 1000, message = "지원 동기는 10자 이상 1000자 이하로 작성해주세요.")
  private String applyReason;

  /**
   * prototype.tsx 의 'agreed' — 개인정보 수집 동의 필수.
   *
   * <p>@AssertTrue 를 field 에 부착하면 error field name 이 field 이름과 일치 (`privacyAgreed`) → Thymeleaf
   * `#fields.hasErrors('privacyAgreed')` 로 접근 가능. 별도 `isPrivacyAccepted()` 게터를 두면 property 이름이
   * `privacyAccepted` 로 파생되어 템플릿 참조가 어긋남.
   */
  @AssertTrue(message = "개인정보 수집 동의가 필요합니다.")
  private boolean privacyAgreed;
}
