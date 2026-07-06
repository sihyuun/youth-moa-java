package io.github.sihyuuun.youthmoa.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * D5 알림 채널 설정 요청.
 *
 * <p>체크박스 3종. 체크되지 않으면 field 가 요청에 없거나 false 로 바인딩된다.
 */
@Getter
@Setter
@NoArgsConstructor
public class NotificationChannelRequest {

  private boolean kakao;
  private boolean sms;
  private boolean email;
}
