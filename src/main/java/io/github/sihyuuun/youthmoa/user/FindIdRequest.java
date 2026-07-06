package io.github.sihyuuun.youthmoa.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * F0i: 아이디 찾기 폼 DTO (이름 + 휴대폰).
 *
 * <p>휴대폰은 하이픈이 있어도/없어도 허용하고, 서비스단에서 숫자만 남기고 저장값과 비교한다.
 */
@Getter
@Setter
public class FindIdRequest {

  @NotBlank(message = "이름을 입력해주세요.")
  @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
  private String name;

  @NotBlank(message = "핸드폰 번호를 입력해주세요.")
  @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
  private String phone;
}
