package io.github.sihyuuun.youthmoa.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyCodeRequest {
  @NotBlank(message = "핸드폰 번호를 입력해주세요.")
  private String phone;

  @NotBlank(message = "인증번호를 입력해주세요.")
  @Pattern(regexp = "^\\d{6}$", message = "인증번호는 6자리 숫자여야 합니다.")
  private String code;
}
