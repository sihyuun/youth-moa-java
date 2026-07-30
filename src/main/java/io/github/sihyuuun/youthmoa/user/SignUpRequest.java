package io.github.sihyuuun.youthmoa.user;

import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * 회원가입 폼 DTO — prototype.tsx SignupScreen (line 1414~1539) 기준.
 *
 * <h3>검증 그룹 (입력 여부 → 형식 순서)</h3>
 *
 * <ol>
 *   <li>{@link RequiredCheck} — NotBlank / NotNull (입력 여부)
 *   <li>{@link FormatCheck} — Size / Pattern / Email / AssertTrue (형식·정책)
 * </ol>
 *
 * Controller 에서 {@code @Validated(SignUpRequest.OrderedChecks.class)} 적용 시 {@link RequiredCheck} 가
 * 실패한 필드는 {@link FormatCheck} 검증을 건너뛴다.
 */
@Getter
@Setter
public class SignUpRequest {

  public interface RequiredCheck {}

  public interface FormatCheck {}

  /** Controller 에서 사용할 그룹 시퀀스. 1단계(RequiredCheck) → 2단계(FormatCheck). */
  @GroupSequence({RequiredCheck.class, FormatCheck.class})
  public interface OrderedChecks {}

  // ── 계정 정보 ───────────────────────────────────────
  @NotBlank(message = "이메일을 입력해주세요.", groups = RequiredCheck.class)
  @Email(message = "올바른 이메일 형식이 아닙니다.", groups = FormatCheck.class)
  private String email;

  @NotBlank(message = "비밀번호를 입력해주세요.", groups = RequiredCheck.class)
  @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.", groups = FormatCheck.class)
  @Pattern(
      regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
      message = "비밀번호는 영문과 숫자를 모두 포함해야 합니다.",
      groups = FormatCheck.class)
  private String password;

  @NotBlank(message = "비밀번호 확인을 입력해주세요.", groups = RequiredCheck.class)
  private String passwordConfirm;

  // ── 개인 정보 ───────────────────────────────────────
  @NotBlank(message = "이름을 입력해주세요.", groups = RequiredCheck.class)
  @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다.", groups = FormatCheck.class)
  private String name;

  @NotBlank(message = "핸드폰 번호를 입력해주세요.", groups = RequiredCheck.class)
  @Pattern(regexp = "^[0-9]{10,11}$", message = "숫자만 입력해주세요. (10~11자리)", groups = FormatCheck.class)
  private String phone;

  @NotNull(message = "성별을 선택해주세요.", groups = RequiredCheck.class)
  private UserGender gender;

  /** prototype.tsx 는 type=text 로 "YYYY / MM / DD" 또는 "YYYY-MM-DD" 입력 받음 */
  @NotBlank(message = "생년월일을 입력해주세요.", groups = RequiredCheck.class)
  @Pattern(
      regexp = "^\\d{4}[-/.\\s]?\\d{2}[-/.\\s]?\\d{2}$",
      message = "YYYY-MM-DD 또는 YYYY/MM/DD 형식으로 입력해주세요.",
      groups = FormatCheck.class)
  private String birthDateText;

  @NotBlank(message = "우편번호를 입력해주세요.", groups = RequiredCheck.class)
  @Pattern(regexp = "^[0-9]{5}$", message = "우편번호 5자리를 입력해주세요.", groups = FormatCheck.class)
  private String zipcode;

  @NotBlank(message = "주소를 입력해주세요.", groups = RequiredCheck.class)
  private String address;

  private String addressDetail;

  // ── 약관 동의 — F-signup-terms-agreement (Q1=a: Map<code, Boolean>) ────────────
  // 폼 바인딩 예: agreements[SERVICE]=true / agreements[PRIVACY]=true. 미체크 시 key 없음.
  // 필수 약관 전건 동의 검증은 활성 약관 목록을 아는 서비스 계층에서 수행 (DTO 로는 표현 불가).
  private Map<String, Boolean> agreements = new HashMap<>();

  // 중복확인 통과 여부 — hidden input 으로 전송, 이메일 변경 시 false 로 reset
  private boolean emailChecked;

  // F-signup-01: 휴대폰 인증 통과 여부 — hidden input 으로 폼 렌더용.
  // ⚠️ 서버는 이 값을 절대 신뢰하지 않는다 (변조 방어). 진리 소스는 세션의 phoneVerifiedAt / phoneVerifiedNumber.
  // 클라이언트 UX 상 hidden field 는 존재해야 폼 재렌더 시 인증 완료 상태를 시각적으로 유지 가능.
  private boolean phoneVerified;

  // ── 객체 레벨 검증 (FormatCheck 그룹 — 개별 필드 입력 통과 후 검증) ──────────

  @AssertTrue(message = "비밀번호와 비밀번호 확인이 일치하지 않습니다.", groups = FormatCheck.class)
  public boolean isPasswordMatched() {
    return password != null && password.equals(passwordConfirm);
  }

  @AssertTrue(message = "아이디 중복확인을 진행해주세요.", groups = FormatCheck.class)
  public boolean isEmailChecked() {
    return emailChecked;
  }

  /** birthDateText 를 LocalDate 로 파싱 (UserService 에서 호출). */
  public LocalDate parseBirthDate() {
    if (birthDateText == null || birthDateText.isBlank()) return null;
    String normalized = birthDateText.replaceAll("[\\s/.]", "-");
    try {
      return LocalDate.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    } catch (DateTimeParseException e) {
      return null;
    }
  }
}
