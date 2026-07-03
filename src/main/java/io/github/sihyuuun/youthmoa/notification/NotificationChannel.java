package io.github.sihyuuun.youthmoa.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * D1b: 사용자 알림 수신 채널.
 *
 * <p>enum 순서(KAKAO → SMS → EMAIL) 가 신청 완료 페이지 부제 문구 조립 순서와 동일하게 유지된다.
 */
@Getter
@RequiredArgsConstructor
public enum NotificationChannel {
  KAKAO("카카오톡"),
  SMS("문자"),
  EMAIL("이메일");

  private final String label;
}
