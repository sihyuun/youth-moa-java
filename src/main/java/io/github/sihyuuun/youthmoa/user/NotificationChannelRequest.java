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

  // 채널 3필드는 폼이 항상 6개 checkbox 전송하는 계약 기반 primitive 유지 (미체크=false 의도)
  private boolean kakao;
  private boolean sms;
  private boolean email;

  // D5 알림 항목 (prototype L1572~1577).
  // Boolean 래퍼로 nullable 처리: 부분 저장 클라이언트(외부 API·캐시된 이전 폼) 가
  // 항목 필드를 제외하고 전송하면 null → UserService 에서 기존값 유지. 데이터 파괴 방어.
  private Boolean remindD1;
  private Boolean waitlistEmpty;
  private Boolean newProgramNews;
}
