package io.github.sihyuuun.youthmoa.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendCodeRequest {
  @NotBlank(message = "핸드폰 번호를 입력해주세요.")
  private String phone;
}
