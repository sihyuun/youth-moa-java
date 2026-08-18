package io.github.sihyuuun.youthmoa.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * D5 개인정보 수정 요청 (Step2).
 *
 * <p>성별은 편집 불가(회원가입 시 확정). 이메일은 readonly.
 *
 * <p>2026-07-31 (fix/password-change-inline): 비밀번호 변경을 별도 페이지 이동에서 인라인 필드로 전환 (wireframe
 * WF-3-003-02 정합). password/passwordConfirm 은 optional — 빈 값이면 변경 안 함. 검증은 UserService 에서 수행 (양쪽 값
 * 공존 · 정책 · 일치).
 */
@Getter
@Setter
@NoArgsConstructor
public class ProfileUpdateRequest {

  @NotBlank(message = "이름을 입력해주세요.")
  @Size(max = 50, message = "이름은 50자 이내여야 합니다.")
  private String name;

  @NotBlank(message = "휴대폰 번호를 입력해주세요.")
  @Pattern(regexp = "^01[0-9]{8,9}$", message = "휴대폰 번호는 숫자만 10~11자리여야 합니다.")
  private String phone;

  @Size(max = 10)
  private String zipcode;

  @Size(max = 255)
  private String address;

  @Size(max = 255)
  private String addressDetail;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate birthDate;

  /** Q-5 (P-Q2): 성별 편집 허용 (prototype tsx L1526). null / MALE / FEMALE. */
  private UserGender gender;

  // F-signup-03: interests 를 2개 컬럼으로 분리 (관심 지역 + 관심 분야).
  private Set<String> interestRegions = new HashSet<>();

  private Set<String> interestCategories = new HashSet<>();

  /** 새 비밀번호 (optional). 빈 값이면 비밀번호 변경 스킵. 검증은 서비스에서 수행. */
  private String password;

  /** 새 비밀번호 확인 (optional). password 가 있을 때만 필수. */
  private String passwordConfirm;
}
