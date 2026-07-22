package io.github.sihyuuun.youthmoa.user;

import io.github.sihyuuun.youthmoa.user.dto.SendCodeRequest;
import io.github.sihyuuun.youthmoa.user.dto.VerifyCodeRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * F-signup-01: 휴대폰 인증 API.
 *
 * <p>세션에 인증 성공 시각/번호를 저장해 회원가입 시 UserController.signUp 이 재확인. 하드코딩된 hidden field 는 서버에서 신뢰하지 않는다
 * (변조 방어).
 *
 * <p>CSRF: SecurityConfig 에서 기본 활성. signup.html 의 meta name="_csrf" 값을 fetch 시 X-CSRF-TOKEN 헤더로
 * 전송해야 통과.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/phone")
public class PhoneVerificationController {

  public static final String SESSION_KEY_VERIFIED_AT = "phoneVerifiedAt";
  public static final String SESSION_KEY_VERIFIED_NUMBER = "phoneVerifiedNumber";

  private final PhoneVerificationService phoneVerificationService;
  private final SmsRateLimiter smsRateLimiter;

  /**
   * mock 모드 (youthmoa.coolsms.enabled=false) — 실 SMS 발송 없음. 크레딧 리스크 없어 rate limiter 스킵. 개발·e2e 테스트
   * 반복 편의. 운영 (enabled=true) 에서는 정상 rate limit 적용.
   */
  @org.springframework.beans.factory.annotation.Value("${youthmoa.coolsms.enabled:false}")
  private boolean coolsmsEnabled;

  @PostMapping("/send-code")
  public ResponseEntity<?> sendCode(
      @Valid @RequestBody SendCodeRequest request, HttpServletRequest httpRequest) {
    if (coolsmsEnabled) {
      String ip = clientIp(httpRequest);
      if (!smsRateLimiter.tryAcquire(ip)) {
        return ResponseEntity.status(429).body(Map.of("error", "인증 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."));
      }
    }
    try {
      phoneVerificationService.sendCode(request.getPhone());
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(429).body(Map.of("error", e.getMessage()));
    }
    return ResponseEntity.ok(Map.of("ok", true));
  }

  @PostMapping("/verify-code")
  public ResponseEntity<?> verifyCode(
      @Valid @RequestBody VerifyCodeRequest request, HttpSession session) {
    boolean success = phoneVerificationService.verifyCode(request.getPhone(), request.getCode());
    if (!success) {
      return ResponseEntity.badRequest()
          .body(Map.of("ok", false, "error", "인증번호가 올바르지 않거나 만료되었습니다."));
    }
    String normalized = PhoneVerificationService.normalize(request.getPhone());
    session.setAttribute(SESSION_KEY_VERIFIED_AT, LocalDateTime.now());
    session.setAttribute(SESSION_KEY_VERIFIED_NUMBER, normalized);
    return ResponseEntity.ok(Map.of("ok", true));
  }

  private String clientIp(HttpServletRequest req) {
    String xff = req.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      int comma = xff.indexOf(',');
      return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
    }
    return req.getRemoteAddr();
  }
}
