package io.github.sihyuuun.youthmoa.admin;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * A-admin-notice-attachment QA 반려 P1 대응 (2026-09-04): 관리자 서비스에서 던지는 {@link
 * IllegalArgumentException} 은 사용자 입력 위반 (확장자·크기·필수값 등) 이므로 500 이 아닌 400 으로 매핑. HTMX fragment 응답
 * 시나리오는 별도 티켓에서 확장.
 */
@Slf4j
@ControllerAdvice(basePackages = "io.github.sihyuuun.youthmoa.admin")
public class AdminExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
    log.warn("Admin bad request: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("error", e.getMessage() == null ? "잘못된 요청이에요." : e.getMessage()));
  }
}
