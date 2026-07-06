package io.github.sihyuuun.youthmoa.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/** F0i: 비밀번호 찾기 본인 확인 폼 DTO (이메일 + 이름 + 휴대폰). */
@Getter
@Setter
public class FindPasswordRequest {

  @NotBlank(message = "이메일을 입력해주세요.")
  @Email(message = "올바른 이메일 형식이 아닙니다.")
  private String email;

  @NotBlank(message = "이름을 입력해주세요.")
  private String name;

  @NotBlank(message = "핸드폰 번호를 입력해주세요.")
  @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
  private String phone;
}
