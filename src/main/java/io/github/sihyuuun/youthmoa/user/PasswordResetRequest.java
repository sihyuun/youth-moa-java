package io.github.sihyuuun.youthmoa.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** F0i: 비밀번호 재설정 폼 DTO — signup 비밀번호 정책과 동일. */
@Getter
@Setter
public class PasswordResetRequest {

  @NotBlank(message = "새 비밀번호를 입력해주세요.")
  @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
  @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "비밀번호는 영문과 숫자를 모두 포함해야 합니다.")
  private String password;

  @NotBlank(message = "새 비밀번호 확인을 입력해주세요.")
  private String passwordConfirm;
}
