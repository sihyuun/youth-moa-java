package io.github.sihyuuun.youthmoa.user;

import java.util.List;

/**
 * F-signup-terms-agreement: 필수 약관 동의 누락 시 서비스에서 던지는 예외.
 *
 * <p>컨트롤러가 {@code BindingResult.rejectValue("agreements", ...)} 로 매핑한다. UserService.signUp 내부에서도
 * 방어적으로 재검증하여 컨트롤러가 검증을 건너뛴 경우 (예: 다른 진입점) 데이터 무결성을 보장한다.
 */
public class TermsAgreementException extends RuntimeException {

  private final List<String> missingCodes;

  public TermsAgreementException(List<String> missingCodes) {
    super("필수 약관 미동의: " + missingCodes);
    this.missingCodes = List.copyOf(missingCodes);
  }

  public List<String> getMissingCodes() {
    return missingCodes;
  }
}
