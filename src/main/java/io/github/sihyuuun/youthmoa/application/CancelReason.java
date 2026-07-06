package io.github.sihyuuun.youthmoa.application;

import java.util.Arrays;
import java.util.Optional;

/**
 * D5: 신청 취소 사유 코드.
 *
 * <p>DB 에는 {@link Application#cancelReason} String 컬럼에 label 로 저장한다 (OTHER 인 경우 사용자 입력 텍스트를 이어붙임).
 * enum 자체를 컬럼으로 두지 않는 이유: 관리자 통계는 별도 티켓(D5c) 에서 다룰 예정이며 지금은 표시용.
 */
public enum CancelReason {
  CHANGE_MIND("단순 변심"),
  SCHEDULE_CONFLICT("일정이 맞지 않음"),
  DUPLICATE_APPLY("중복 신청"),
  PERSONAL("개인 사유"),
  OTHER("기타");

  private final String label;

  CancelReason(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public static Optional<CancelReason> fromCode(String code) {
    if (code == null) return Optional.empty();
    return Arrays.stream(values()).filter(r -> r.name().equals(code)).findFirst();
  }
}
