package io.github.sihyuuun.youthmoa.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * D5 신청 취소 요청.
 *
 * <p>reasonCode 필수 (radio 5종 중 하나). reasonCode=OTHER 인 경우 reasonText 필수 (최대 100자). Controller 에서
 * 조합해 Application.cancelReason 에 저장.
 */
@Getter
@Setter
@NoArgsConstructor
public class CancelRequest {

  @NotBlank(message = "취소 사유를 선택해주세요.")
  private String reasonCode;

  @Size(max = 100, message = "취소 사유는 최대 100자까지 입력할 수 있어요.")
  private String reasonText;
}
